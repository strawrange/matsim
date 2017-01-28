package rideSharing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import org.matsim.core.mobsim.framework.MobsimAgent.State;

public class RideShareOptimizer  implements VrpOptimizer{
	private final QSim qsim;

    private final TravelTime travelTime;
    private final LeastCostPathCalculator router;

    public LeastCostPathCalculator getRouter() {
		return router;
	}

	private Map<Id<Vehicle>, Vehicle> vehicles ;//we have only one vehicle
    

	private Map<Id<Vehicle>, Schedule<AbstractTask>>schedule = new LinkedHashMap<>();// the vehicle's schedule

    public static final double PICKUP_DURATION = 120;
    public static final double DROPOFF_DURATION = 60;
    public static final double STAY_DURATION = 0;



    public RideShareOptimizer(Scenario scenario, VrpData vrpData, QSim qsim)
    {
        this.qsim = qsim;

        travelTime = new FreeSpeedTravelTime();
        router = new Dijkstra(scenario.getNetwork(), new TimeAsTravelDisutility(travelTime),
                travelTime);

        vrpData.clearRequestsAndResetSchedules();//necessary if we run more than 1 iteration
             // vehicle = vrpData.getVehicles().values().iterator().next();
   }
    
    @Override
    public void requestSubmitted(Request request){
    	Vehicle vehicle = null;
    	for(Vehicle veh: vehicles.values()){
			if(veh.getT1() == 0){
				continue;
			}
    		if(vehicle == null){
    			vehicle = veh;
    		}else{
    			if(vehicle.getT0() > veh.getT0()){
    				Vehicle v = vehicle;
    				vehicle = veh;
    				veh = v;
    			}
				if(vehicle.getSchedule().getLastTask() == null){
					continue;
				}
				if(veh.getSchedule().getLastTask() == null){
					vehicle = veh;
					continue;
				}
				if(veh.getSchedule().getLastTask() == null
						&& veh.getT0() < vehicle.getSchedule().getLastTask().getEndTime()){
					vehicle = veh;
					continue;
				}
				if(veh.getSchedule().getLastTask().getEndTime() < 
						vehicle.getSchedule().getLastTask().getEndTime()){

					vehicle = veh;
					continue;
				}
    			/*if(veh.getT0() < vehicle.getT0()){
    				if(vehicle.getSchedule().getDropoffTask() == null &&
    						veh.getSchedule().getDropoffTask() != null){
    					vehicle = veh;
    				}else if()
    			}else{
    			if(veh.getSchedule().getDropoffTask() == null){
    				vehicle = veh;
    			}else if(vehicle.getSchedule().getDropoffTask().getEndTime() > veh.getSchedule().getDropoffTask().getEndTime()){
    				vehicle = veh;
    			}*/
    		}
    	}
    	if(vehicle == null){
    		return;
    	}
		requestSubmitted(request,vehicle);
    }



    public void requestSubmitted(Request request, Vehicle veh)
    {
    	Schedule<AbstractTask> s = schedule.get(veh.getId());

        StayTask lastTask = (StayTask)Schedules.getLastTask(s);// only WaitTask possible here
        double currentTime = qsim.getSimTimer().getTimeOfDay();
        

        
        if(s.getTasks().size() > 1 && s.getTasks().get(s.getTasks().size() - 2).getOnWayToActivity() && 
        		(!s.getTasks().get(s.getTasks().size() - 2).getStatus().equals(Task.TaskStatus.PERFORMED))){
    		return;
    	}else if(lastTask.getStatus().equals(Task.TaskStatus.PERFORMED)){

    		veh.resetSchedule();
    		veh.getAgentLogic().computeInitialActivity(veh.getAgentLogic().getDynAgent());
            
    		s = (Schedule<AbstractTask>) veh.getSchedule();
			s.addTask(new StayTaskImpl(veh.getT0(), veh.getT1(), veh.getStartLink(), "wait"));
			s.addStayTaskNumber();
			veh.setSchedule(s);
			this.schedule.put(veh.getId(),s);
            
      
            
    	}
        
        lastTask = (StayTask)Schedules.getLastTask(s);
        
        switch (lastTask.getStatus()) {
            case PLANNED:
                s.removeLastTask();// remove waiting
                s.reduceStayTaskNumber();
                break;

            case STARTED:
                lastTask.setEndTime(currentTime);// shorten waiting
                break;

            default:
                throw new IllegalStateException();
        }
        
        // if driver on his way to next activity (e.g.home), he will not receive any request

        RideShareRequest req = (RideShareRequest)request;
        Link fromLink = req.getFromLink();
        Link toLink = req.getToLink();
        List <AbstractTask> prepareTasks = new ArrayList<>();

        double t0 = s.getStatus() == ScheduleStatus.UNPLANNED ? //
                Math.max(veh.getT0(), currentTime) : //
                Schedules.getLastTask(s).getEndTime();

        VrpPathWithTravelData p1 = VrpPaths.calcAndCreatePath(lastTask.getLink(), fromLink, t0,
                router, travelTime);
        AbstractTask tempTask = new DriveTaskImpl(p1, lastTask.getLink(), fromLink);
        distanceCalculator(tempTask, request, veh);
        prepareTasks.add(tempTask);

        double t1 = p1.getArrivalTime();
        double t2 = t1 + PICKUP_DURATION;// 2 minutes for picking up the passenger
        tempTask = new RideShareServeTask(t1, t2, fromLink, true, req);
        distanceCalculator(tempTask, request, veh);
        prepareTasks.add(tempTask);


        VrpPathWithTravelData p2 = VrpPaths.calcAndCreatePath(fromLink, toLink, t2, router,
                travelTime);
        tempTask = new DriveTaskImpl(p2, fromLink, toLink);
        distanceCalculator(tempTask, request, veh);
        prepareTasks.add(tempTask);


        double t3 = p2.getArrivalTime();
        double t4 = t3 + DROPOFF_DURATION;// 1 minute for dropping off the passenger
        tempTask = new RideShareServeTask(t3, t4, toLink, false, req);
        distanceCalculator(tempTask, request, veh);
        prepareTasks.add(tempTask);


        //just wait (and be ready) till the end of the vehicle's time window (T1)
        //double tEnd;
        //if(veh.getT1() != 0){
        //tEnd = Math.max(t4, veh.getT1());
        //}else{
        //	tEnd = t4;
        //}
        //s.addTask(new StayTaskImpl(t4, tEnd, toLink, "wait"));
        s.addTaskInOrder(prepareTasks);
    }

    public void driveRequestSubmitted(Request request, double now, Id<Vehicle> vehId)
    {
    	Schedule<AbstractTask> s = schedule.get(vehId);
    	
        StayTask lastTask = (StayTask)Schedules.getLastTask(s);// last is not stay task
        double currentTime = qsim.getSimTimer().getTimeOfDay();

        switch (lastTask.getStatus()) {
            case PLANNED:
            	s.removeLastTask();// remove waiting
                s.reduceStayTaskNumber();

            case STARTED:
                lastTask.setEndTime(currentTime);// shorten waiting
                break;

            default:
                throw new IllegalStateException();
        }

        RideShareRequest req = (RideShareRequest)request;
        Link fromLink = req.getFromLink();
        Link toLink = req.getToLink();
        Vehicle vehicle = vehicles.get(vehId);

        VrpPathWithTravelData p1 = VrpPaths.calcAndCreatePath(fromLink, toLink, request.getT0(),
                router, travelTime);
        DriveTaskImpl tempTask = new DriveTaskImpl(p1);
        tempTask.onWayToActivity();
        schedule.get(vehId).addTask(tempTask);
        
        //just wait (and be ready) till the end of the vehicle's time window (T1)
        double t1 = p1.getArrivalTime() + STAY_DURATION;
        double tEnd = Math.max(t1, vehicle.getT1());
        schedule.get(vehId).addTask(new StayTaskImpl(t1, tEnd, toLink, "wait"));
        schedule.get(vehId).addStayTaskNumber();
        
        schedule.get(vehId).addEndRideShareNumber();
        
    }

    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        shiftTimings(schedule.getVehicle().getId());
        schedule.nextTask();
    }


    /**
     * Simplified version. For something more advanced, see
     * {@link org.matsim.contrib.taxi.scheduler.TaxiScheduler#updateBeforeNextTask(Schedule)} in the
     * taxi contrib
     * @param id 
     */
    private void shiftTimings(Id<Vehicle> id)
    {
        if (schedule.get(id).getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double now = qsim.getSimTimer().getTimeOfDay();
        Task currentTask = schedule.get(id).getCurrentTask();
        double diff = now - currentTask.getEndTime();

        if (diff == 0) {
            return;
        }

        currentTask.setEndTime(now);

        List<AbstractTask> tasks = schedule.get(id).getTasks();
        int nextTaskIdx = currentTask.getTaskIdx() + 1;

        //all except the last task (waiting)
        for (int i = nextTaskIdx; i < tasks.size() - 1; i++) {
            Task task = tasks.get(i);
            task.setBeginTime(task.getBeginTime() + diff);
            task.setEndTime(task.getEndTime() + diff);
        }

        //wait task
        if (nextTaskIdx != tasks.size()) {
            Task waitTask = tasks.get(tasks.size() - 1);
            waitTask.setBeginTime(waitTask.getBeginTime() + diff);

            double tEnd = Math.max(waitTask.getBeginTime(), vehicles.get(id).getT1());
            waitTask.setEndTime(tEnd);
        }
    }
    
	@SuppressWarnings("unchecked")
	public void setVehicleSchedule(VrpData vrpData) {
		this.vehicles = vrpData.getVehicles();
		for(Vehicle vehicle : vehicles.values()){
			Schedule<AbstractTask> s = (Schedule<AbstractTask>) vehicle.getSchedule();
			s.addTask(new StayTaskImpl(vehicle.getT0(), vehicle.getT1(), vehicle.getStartLink(), "wait"));
			s.addStayTaskNumber();
			vehicle.setSchedule(s);
			this.schedule.put(vehicle.getId(),s);
		}
	}

	@Override
	public void distanceCalculator(AbstractTask task, Request request, Vehicle vehicle){
		RideShareRequest req = (RideShareRequest)request;
		
		RideShareAgent agent = (RideShareAgent) qsim.getAgentMap().get(vehicle.getId());
		
		Id<Link> destinationId = null;
		
		PlanElement currentPlan = agent.getCurrentPlanElement();
		//State currentState  = agent.getState();
		
		if(currentPlan instanceof Activity){
			Leg nextLeg = (Leg) agent.getNextPlanElement();
			destinationId = nextLeg.getRoute().getEndLinkId();
		}else if (currentPlan instanceof Leg){
			destinationId = agent.getpAgent().getDestinationLinkId();   		
		}
		Link destination = qsim.getScenario().getNetwork().getLinks().get(destinationId);
		
		Link departure = vehicle.getStartLink();
	
		
		double departureDis = Math.pow((req.getFromLink().getCoord().getX() - departure.getCoord().getX()), 2) + 
				Math.pow((req.getFromLink().getCoord().getY() - departure.getCoord().getY()), 2);
		
		double destinationDis = Math.pow((req.getToLink().getCoord().getX() - destination.getCoord().getX()), 2) + 
				Math.pow((req.getToLink().getCoord().getY() - destination.getCoord().getY()), 2);
		
		double distanceDif = Math.pow(departureDis, 0.5) + Math.pow(destinationDis, 0.5);
		
		task.setDistanceDifference(distanceDif);
	}



}
