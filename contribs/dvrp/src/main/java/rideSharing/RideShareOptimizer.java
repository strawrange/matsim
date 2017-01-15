package rideSharing;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiRequest;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiServeTask;
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

public class RideShareOptimizer  implements VrpOptimizer{
	private final QSim qsim;

    private final TravelTime travelTime;
    private final LeastCostPathCalculator router;

    private Vehicle vehicle;//we have only one vehicle
    

	private Schedule<AbstractTask> schedule;// the vehicle's schedule

    public static final double PICKUP_DURATION = 120;
    public static final double DROPOFF_DURATION = 60;



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
    public void requestSubmitted(Request request)
    {
        StayTask lastTask = (StayTask)Schedules.getLastTask(schedule);// only WaitTask possible here
        double currentTime = qsim.getSimTimer().getTimeOfDay();

        switch (lastTask.getStatus()) {
            case PLANNED:
                schedule.removeLastTask();// remove waiting
                break;

            case STARTED:
                lastTask.setEndTime(currentTime);// shorten waiting
                break;

            default:
                throw new IllegalStateException();
        }

        RideShareRequest req = (RideShareRequest)request;
        Link fromLink = req.getFromLink();
        Link toLink = req.getToLink();

        double t0 = schedule.getStatus() == ScheduleStatus.UNPLANNED ? //
                Math.max(vehicle.getT0(), currentTime) : //
                Schedules.getLastTask(schedule).getEndTime();

        VrpPathWithTravelData p1 = VrpPaths.calcAndCreatePath(lastTask.getLink(), fromLink, t0,
                router, travelTime);
        schedule.addTask(new DriveTaskImpl(p1));

        double t1 = p1.getArrivalTime();
        double t2 = t1 + PICKUP_DURATION;// 2 minutes for picking up the passenger
        schedule.addTask(new RideShareServeTask(t1, t2, fromLink, true, req));

        VrpPathWithTravelData p2 = VrpPaths.calcAndCreatePath(fromLink, toLink, t2, router,
                travelTime);
        schedule.addTask(new DriveTaskImpl(p2));

        double t3 = p2.getArrivalTime();
        double t4 = t3 + DROPOFF_DURATION;// 1 minute for dropping off the passenger
        schedule.addTask(new RideShareServeTask(t3, t4, toLink, false, req));

        //just wait (and be ready) till the end of the vehicle's time window (T1)
        double tEnd = Math.max(t4, vehicle.getT1());
        schedule.addTask(new StayTaskImpl(t4, tEnd, toLink, "wait"));
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        shiftTimings();
        schedule.nextTask();
    }


    /**
     * Simplified version. For something more advanced, see
     * {@link org.matsim.contrib.taxi.scheduler.TaxiScheduler#updateBeforeNextTask(Schedule)} in the
     * taxi contrib
     */
    private void shiftTimings()
    {
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double now = qsim.getSimTimer().getTimeOfDay();
        Task currentTask = schedule.getCurrentTask();
        double diff = now - currentTask.getEndTime();

        if (diff == 0) {
            return;
        }

        currentTask.setEndTime(now);

        List<AbstractTask> tasks = schedule.getTasks();
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

            double tEnd = Math.max(waitTask.getBeginTime(), vehicle.getT1());
            waitTask.setEndTime(tEnd);
        }
    }
    
	@SuppressWarnings("unchecked")
	public void setVehicleSchedule(VrpData vrpData) {
		this.vehicle = vrpData.getVehicles().values().iterator().next();
        schedule = (Schedule<AbstractTask>)vehicle.getSchedule();
        schedule.addTask(
                new StayTaskImpl(vehicle.getT0(), vehicle.getT1(), vehicle.getStartLink(), "wait"));
	}


}
