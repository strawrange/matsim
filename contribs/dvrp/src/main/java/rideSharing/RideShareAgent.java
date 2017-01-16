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
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynLeg;
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
	
	PersonDriverAgentImpl pAgent;
	DynAgent dAgent;
	State status;
	Boolean isDyn = false;
	VrpData vrpData;
	PassengerEngine passengerEngine;
	
	public Boolean getIsDyn() {
		return isDyn;
	}

	public void setIsDyn(Boolean isDyn) {
		this.isDyn = isDyn;
	}

	public RideShareAgent(PersonDriverAgentImpl pAgent, DynAgent dAgent, VrpData vrpData, PassengerEngine passengerEngine){
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
		return isDyn?dAgent.getMode():TransportMode.car;
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
		double DynEndTime = logic.getVehicle().getT1();
		double DynStartTime = logic.getVehicle().getT0();
		if(now >= DynStartTime && now < DynEndTime && !isDyn){
	    	pAgent.endActivityAndComputeNextState(now);
	    	setIsDyn(true);
		}else{
		Schedule<? extends Task> schedule = logic.getVehicle().getSchedule();
			if(isDyn && !schedule.getCurrentTask().equals(null) ){
				if(schedule.getNextTask().equals(null)||schedule.getDropoffTask().equals(null)||schedule.getDropoffTask().getEndTime() >= DynEndTime){
					schedule.clearTasks();
					Request request = passengerEngine.createRequest(dAgent.getVehicle().getCurrentLink().getId(), pAgent.getDestinationLinkId(), now, now);
					logic.driveRequestSubmitted(request, now);
					dAgent.endActivityAndComputeNextState(now);
			//endLegAndComputeNextState(now);
					setIsDyn(false);
					return;
				}
			}
		}


		if(isDyn){
			dAgent.endActivityAndComputeNextState(now);
		}else{
			pAgent.endActivityAndComputeNextState(now);
		}
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		PlanElement plan = getCurrentPlanElement();
		if(plan instanceof Leg && ((Leg)plan).getMode().equals(Run.MODE_DRIVER)){
			pAgent.setCurrentLinkId(dAgent.getCurrentLinkId());
		}
			//dAgent.endLegAndComputeNextState(now);
		//}else{
		//pAgent.endLegAndComputeNextState(now);
		//}
		//VrpAgentLogic logic = (VrpAgentLogic)(dAgent.getAgentLogic());
		//double DynEndTime = logic.getVehicle().getT1();
		//if(now >= DynEndTime && isDyn){
		//	setIsDyn(false);
		//	pAgent.setCurrentLinkId(dAgent.getCurrentLinkId());
		//}

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

	public PersonDriverAgentImpl getpAgent() {
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
		return dAgent.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle);
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		// TODO Auto-generated method stub
		return dAgent.getExitAtStop(stop);
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		// TODO Auto-generated method stub
		return dAgent.getDesiredAccessStopId();
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		// TODO Auto-generated method stub
		return dAgent.getDesiredDestinationStopId();
	}

	@Override
	public double getWeight() {
		// TODO Auto-generated method stub
		return dAgent.getWeight();
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		// TODO Auto-generated method stub
		//pAgent.setVehicle(veh);
		dAgent.setVehicle(veh);
	}

	@Override
	public MobsimVehicle getVehicle() {
		// TODO Auto-generated method stub
		return dAgent.getVehicle();
	}

	@Override
	public Id<org.matsim.vehicles.Vehicle> getPlannedVehicleId() {
		// TODO Auto-generated method stub
		return isDyn ? dAgent.getPlannedVehicleId():pAgent.getPlannedVehicleId();
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		// TODO Auto-generated method stub
		return dAgent.chooseNextLinkId();
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		// TODO Auto-generated method stub
		dAgent.notifyMoveOverNode(newLinkId);
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// TODO Auto-generated method stub
		return dAgent.isWantingToArriveOnCurrentLink();
	}

	public void doSimStep(double time) {
		// TODO Auto-generated method stub
		
	}

}
