package rideSharing;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleImpl;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynLeg;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public final class RideShareAgent implements MobsimDriverPassengerAgent{
	
	MobsimDriverPassengerAgent pAgent;
	DynAgent dAgent;
	State status;
	Boolean isDyn = false;
	Boolean legDyn = false;
	VrpData vrpData;
	PassengerEngine passengerEngine;
	
	public Boolean getIsDyn() {
		return isDyn;
	}

	public void setIsDyn(Boolean isDyn) {
		this.isDyn = isDyn;
	}

	public RideShareAgent(MobsimDriverPassengerAgent pAgent, DynAgent dAgent, VrpData vrpData, PassengerEngine passengerEngine){
		this.pAgent = pAgent;
		this.dAgent = dAgent;
		this.vrpData = vrpData;
		this.passengerEngine = passengerEngine;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		// TODO Auto-generated method stub
		
		return 	isDyn?dAgent.getCurrentLinkId():pAgent.getCurrentLinkId();
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		// TODO Auto-generated method stub
		return isDyn?dAgent.getDestinationLinkId():pAgent.getDestinationLinkId();
	}

	@Override
	public String getMode() {
		// TODO Auto-generated method stub
		if(pAgent.getCurrentPlanElement() == null){
			return Run.MODE_DRIVER;
		}
		if(pAgent.getCurrentPlanElement() instanceof Leg){
			return ((Leg)pAgent.getCurrentPlanElement()).getMode();
		}
		return Run.MODE_DRIVER;
				//isDyn?dAgent.getMode():TransportMode.car;
	}

	@Override
	public Id<Person> getId() {
		// TODO Auto-generated method stub
		return isDyn?dAgent.getId():pAgent.getId();
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return isDyn ? dAgent.getState():pAgent.getState();
	}

	@Override
	public double getActivityEndTime() {
		// TODO Auto-generated method stub
		return isDyn ? dAgent.getActivityEndTime():pAgent.getActivityEndTime();
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		VrpAgentLogic logic = (VrpAgentLogic)(dAgent.getAgentLogic());
		if(pAgent.getNextPlanElement() instanceof Leg){
			if(!((Leg)pAgent.getNextPlanElement()).getMode().equals(Run.MODE_DRIVER)){
				pAgent.endActivityAndComputeNextState(now);
				return;
			}
		}
		double DynEndTime = logic.getVehicle().getT1();
		double DynStartTime = logic.getVehicle().getT0();
		if(pAgent.getActivityEndTime() != Double.NEGATIVE_INFINITY && pAgent.getActivityEndTime() < DynStartTime ){
			DynStartTime = pAgent.getActivityEndTime();
		}
		Schedule<? extends Task> schedule = logic.getVehicle().getSchedule();
		if(!isDyn && now > DynEndTime && DynEndTime > 0){
	    	pAgent.endActivityAndComputeNextState(now);
	    	dAgent.setCurrentLinkId(getCurrentLinkId());
			setIsDyn(true);
			schedule.clearTasks();
			Request request = passengerEngine.createRequest(dAgent.getCurrentLinkId(), pAgent.getDestinationLinkId(), now, now);
			logic.driveRequestSubmitted(request, now);
			dAgent.endActivityAndComputeNextState(now);
			schedule.getVehicle().removeT();
			legDyn = true;
			return;
		}
		if(now >= DynStartTime && now < DynEndTime && !isDyn){
			//dAgent.initialActivity();
	    	pAgent.endActivityAndComputeNextState(now);
	    	dAgent.setCurrentLinkId(getCurrentLinkId());
	    	setIsDyn(true);
	    	dAgent.setCurrentLinkId(getCurrentLinkId());
	    	dAgent.endActivityAndComputeNextStateWithoutStayEvent(now);
	    	return;
			//if(dAgent.getCurrentAction() instanceof DynLeg){
			//	dAgent.endLegAndComputeNextState(now);
			//	return;
			//}

	    	//logic.getVehicle().getSchedule().resetSchedule();
	    	//logic.getVehicle().getSchedule().nextTask();

		}else{
		
			if(isDyn && !(schedule.getCurrentTask() == null)){
				Task task = schedule.getCurrentTask();
				if(!(task instanceof RideShareServeTask) || !((RideShareServeTask)task).isPickup()){
					if(schedule.getNextTask() == null||schedule.getDropoffTask() == null||schedule.getDropoffTask().getEndTime() >= DynEndTime || now >= DynEndTime){
						schedule.clearTasks();
						Request request = passengerEngine.createRequest(dAgent.getCurrentLinkId(), pAgent.getDestinationLinkId(), now, now);
						logic.driveRequestSubmitted(request, now);
						dAgent.endActivityAndComputeNextState(now);
						schedule.getVehicle().removeT();
						legDyn = true;
			//endLegAndComputeNextState(now);
						return;
					}
				}
			}
		}


		if(isDyn){
			dAgent.endActivityAndComputeNextState(now);
		}else{
			pAgent.endActivityAndComputeNextState(now);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void endLegAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		//PlanElement plan = getCurrentPlanElement();
		//double DynStartTime = logic.getVehicle().getT0();
		//if(plan instanceof Leg && ((Leg)plan).getMode().equals(Run.MODE_DRIVER) && isDyn){
		if(legDyn){
			VrpAgentLogic logic = (VrpAgentLogic)(dAgent.getAgentLogic());
			pAgent.setCurrentLinkId(dAgent.getCurrentLinkId());			
			dAgent.endLegAndComputeNextState(now);
			dAgent.endActivityAndComputeNextState(now);
			setIsDyn(false);
			legDyn = false;
			@SuppressWarnings("unchecked")
			Schedule<AbstractTask> s = (Schedule<AbstractTask>) logic.getVehicle().getSchedule();
			Vehicle veh = logic.getVehicle();
			/*if(veh.getT0() == 0){
				return;
			}
			if(s.getTasks().size() > 1 && s.getTasks().get(s.getTasks().size() - 2).getOnWayToActivity() && 
	        		(!s.getTasks().get(s.getTasks().size() - 2).getStatus().equals(Task.TaskStatus.PERFORMED))){
	    		return;
	    	}else if(lastTask.getStatus().equals(Task.TaskStatus.PERFORMED)){*/
	    		veh.resetSchedule();
	    		veh.getAgentLogic().computeInitialActivity(veh.getAgentLogic().getDynAgent());
	            
	    		s = (Schedule<AbstractTask>) veh.getSchedule();
	    		veh.setStartLink(dAgent.getVehicle().getCurrentLink());
				s.addTask(new StayTaskImpl(veh.getT0(), veh.getT1(), veh.getStartLink(), "wait"));
				s.addStayTaskNumber();
				veh.setSchedule(s);
				logic.getOptimizer().updateSchedule(veh,s);        
			//dAgent.endLegAndComputeNextState(now);
		//}else{
		pAgent.endLegAndComputeNextStateWithoutEvent(now);
		return;
		//}
		//VrpAgentLogic logic = (VrpAgentLogic)(dAgent.getAgentLogic());
		//double DynEndTime = logic.getVehicle().getT1();
		//if(now >= DynEndTime && isDyn){
		//	setIsDyn(false);
		//	pAgent.setCurrentLinkId(dAgent.getCurrentLinkId());
		}

		if(isDyn){
			dAgent.endLegAndComputeNextState(now);
		}else{
			pAgent.endLegAndComputeNextState(now);
		}
	}

	@Override
	public void setStateToAbort(double now) {
		// TODO Auto-generated method stub
		if(isDyn){
			dAgent.setStateToAbort(now);
		}else{
			pAgent.setStateToAbort(now);
		}
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		return isDyn ? dAgent.getExpectedTravelTime():pAgent.getExpectedTravelTime();
	}

	@Override
	public Double getExpectedTravelDistance() {
		// TODO Auto-generated method stub
		return isDyn ? dAgent.getExpectedTravelDistance():pAgent.getExpectedTravelDistance();
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		// TODO Auto-generated method stub
		if(isDyn){
			dAgent.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}else{
			pAgent.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		return pAgent.getCurrentFacility();
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		return pAgent.getDestinationFacility();
	}

	public MobsimDriverPassengerAgent getpAgent() {
		return pAgent;
	}


	public DynAgent getdAgent() {
		return dAgent;
	}

	public PlanElement getNextPlanElement() {
		// TODO Auto-generated method stub
		return pAgent.getNextPlanElement();
	}

	public PlanElement getCurrentPlanElement() {
		// TODO Auto-generated method stub
		return pAgent.getCurrentPlanElement();
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
			TransitVehicle transitVehicle) {
		// TODO Auto-generated method stub
		return pAgent.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle);
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		// TODO Auto-generated method stub
		return pAgent.getExitAtStop(stop);
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		// TODO Auto-generated method stub
		return pAgent.getDesiredAccessStopId();
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		// TODO Auto-generated method stub
		return pAgent.getDesiredDestinationStopId();
	}

	@Override
	public double getWeight() {
		// TODO Auto-generated method stub
		return pAgent.getWeight();
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		// TODO Auto-generated method stub
		pAgent.setVehicle(veh);
		dAgent.setVehicle(veh);
	}

	@Override
	public MobsimVehicle getVehicle() {
		// TODO Auto-generated method stub
		return isDyn ?dAgent.getVehicle():pAgent.getVehicle();
	}

	@Override
	public Id<org.matsim.vehicles.Vehicle> getPlannedVehicleId() {
		// TODO Auto-generated method stub
		return isDyn ? dAgent.getPlannedVehicleId():pAgent.getPlannedVehicleId();
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		// TODO Auto-generated method stub
		return isDyn ?dAgent.chooseNextLinkId():pAgent.chooseNextLinkId();
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		// TODO Auto-generated method stub
		if(isDyn){
		dAgent.notifyMoveOverNode(newLinkId);
		}else{
			pAgent.notifyMoveOverNode(newLinkId);
		}
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// TODO Auto-generated method stub
		return isDyn ?dAgent.isWantingToArriveOnCurrentLink():pAgent.isWantingToArriveOnCurrentLink();
	}

	public void doSimStep(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCurrentLinkId(Id<Link> currentLinkId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endLegAndComputeNextStateWithoutEvent(double now) {
		// TODO Auto-generated method stub
		
	}

}
