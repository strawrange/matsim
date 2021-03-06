/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.dvrp.passenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.jmx.Agent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.*;

import rideSharing.RideShareRequest;


public class PassengerEngine
    implements MobsimEngine, DepartureHandler
{
    private final String mode;

    protected EventsManager eventsManager;
    private InternalInterface internalInterface;
    private final PassengerRequestCreator requestCreator;
    private final VrpOptimizer optimizer;
    private final VrpData vrpData;
    private final Network network;

    private final AdvanceRequestStorage advanceRequestStorage;
    private final AwaitingPickupStorage awaitingPickupStorage;
    
    private final QSim qsim;
    private List<RideShareRequest> waitingRequest = new ArrayList<>();



	public List<RideShareRequest> getWaitingRequest() {
		return waitingRequest;
	}


	public void setWaitingRequest(RideShareRequest r) {
		this.waitingRequest.add(r);
	}


	public PassengerEngine(String mode, EventsManager eventsManager,
            PassengerRequestCreator requestCreator, VrpOptimizer optimizer, VrpData vrpData,
            Network network, QSim qsim)
    {
        this.mode = mode;
        this.eventsManager = eventsManager;
        this.requestCreator = requestCreator;
        this.optimizer = optimizer;
        this.vrpData = vrpData;
        this.network = network;
        this.qsim = qsim;

        advanceRequestStorage = new AdvanceRequestStorage();
        awaitingPickupStorage = new AwaitingPickupStorage();
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
    }


    public String getMode()
    {
        return mode;
    }


    @Override
    public void onPrepareSim()
    {
    	if (optimizer instanceof VrpOptimizer){
    	optimizer.setVehicleSchedule(vrpData);
    	}
    }


    @Override
    public void doSimStep(double time)
    {
    	if(time >= 30 * 3600 ){
        	for(MobsimAgent agent: qsim.getAgents()){
        		if(agent instanceof MobsimPassengerAgent &&agent.getMode() == mode){
        			if(!agent.getCurrentLinkId().equals(agent.getDestinationLinkId())){
        			eventsManager.processEvent( new PersonStuckEvent(time, agent.getId(), agent.getCurrentLinkId(), agent.getMode()));
        			}
        		}
        	}
    	}
    }


    @Override
    public void afterSim()
    {
    	//for(MobsimAgent agent: qsim.getAgents()){
    		//if(true){
    			//if(!agent.getCurrentLinkId().equals(agent.getDestinationLinkId())){
    			//	eventsManager.processEvent( new PersonStuckEvent(agent.getActivityEndTime(), agent.getId(), agent.getCurrentLinkId(), agent.getMode()));
    			//}
    		//}
    	//}
    }


    /**
     * This is to register an advance booking. The method is called when, in reality, the request is
     * made.
     */
    public boolean prebookTrip(double now, MobsimPassengerAgent passenger, Id<Link> fromLinkId,
            Id<Link> toLinkId, double departureTime)
    {

    	if (departureTime <= now) {
            throw new IllegalStateException("This is not a call ahead");
        }

        PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime,
                now);
        optimizer.requestSubmitted(request);

        if (!request.isRejected()) {
            advanceRequestStorage.storeAdvanceRequest(request);
        }

        return !request.isRejected();
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId)
    {
    	if (!waitingRequest.isEmpty()){
    		for(RideShareRequest r : waitingRequest){
    	        PassengerRequest remainRequest = requestCreator.createRequest(r.getId(),r.getPassenger() , r.getFromLink(), r.getToLink(),
    	                now, now, now);   			
    			optimizer.requestSubmitted(remainRequest);
    		}
    		waitingRequest.clear();
        }
    	if (!agent.getMode().equals(mode)) {
            return false;
        }
        /*boolean isDyn = false;
        for(Vehicle veh: vrpData.getVehicles().values()){
        	if(veh.getT1() > now){
        		isDyn = true;
        	}else{
        		veh.resetSchedule();
        	}
        }
        if(isDyn == false){
        	return false;
        }
        */      

        MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;
        

        Id<Link> toLinkId = passenger.getDestinationLinkId();
        double departureTime = now;

        internalInterface.registerAdditionalAgentOnLink(passenger);

        PassengerRequest request = advanceRequestStorage.retrieveAdvanceRequest(passenger,
                fromLinkId, toLinkId, now);

        if (request == null) {//this is an immediate request
            request = createRequest(passenger, fromLinkId, toLinkId, departureTime, now);
            optimizer.requestSubmitted(request);
        }
        else {
            PassengerPickupActivity awaitingPickup = awaitingPickupStorage
                    .retrieveAwaitingPickup(request);

            if (awaitingPickup != null) {
                awaitingPickup.notifyPassengerIsReadyForDeparture(passenger, now);
            }
        }

        return !request.isRejected();
    }


    //================ REQUESTS CREATION

    private long nextId = 0;


    private PassengerRequest createRequest(MobsimPassengerAgent passenger, Id<Link> fromLinkId,
            Id<Link> toLinkId, double departureTime, double now)
    {
        Map<Id<Link>, ? extends Link> links = network.getLinks();
        Link fromLink = links.get(fromLinkId);
        Link toLink = links.get(toLinkId);
        Id<Request> id = Id.create(mode + "_" + nextId++, Request.class);

        PassengerRequest request = requestCreator.createRequest(id, passenger, fromLink, toLink,
                departureTime, departureTime, now);
        vrpData.addRequest(request);
        return request;
    }
    
    public PassengerRequest createRequest(Id<Link> fromLinkId,
            Id<Link> toLinkId, double departureTime, double now)
    {
        Map<Id<Link>, ? extends Link> links = network.getLinks();
        Link fromLink = links.get(fromLinkId);
        Link toLink = links.get(toLinkId);
        Id<Request> id = Id.create(mode + "_" + nextId++, Request.class);

        PassengerRequest request = requestCreator.createRequest(id,null , fromLink, toLink,
                departureTime, departureTime, now);

        return request;
    }


    //================ PICKUP / DROPOFF

    public boolean pickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
            PassengerRequest request, double now)
    {
        MobsimPassengerAgent passenger = request.getPassenger();
        Id<Link> linkId = driver.getCurrentLinkId();

        if (passenger.getCurrentLinkId() != linkId || passenger.getState() != State.LEG
                || !passenger.getMode().equals(mode)) {
            awaitingPickupStorage.storeAwaitingPickup(request, pickupActivity);
            return false;//wait for the passenger
        }

        if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(),
                driver.getCurrentLinkId()) == null) {
            //the passenger has already been picked up and is on another taxi trip
            //seems there have been at least 2 requests made by this passenger for this location
            awaitingPickupStorage.storeAwaitingPickup(request, pickupActivity);
            return false;//wait for the passenger (optimistically, he/she should appear soon)
        }

        MobsimVehicle mobVehicle = driver.getVehicle();
        mobVehicle.addPassenger(passenger);
        passenger.setVehicle(mobVehicle);

        eventsManager.processEvent(
                new PersonEntersVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

        return true;
    }


    public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now)
    {
        MobsimPassengerAgent passenger = request.getPassenger();

        MobsimVehicle mobVehicle = driver.getVehicle();
        mobVehicle.removePassenger(passenger);
        passenger.setVehicle(null);

        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

        passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
        passenger.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(passenger);
    }
}
