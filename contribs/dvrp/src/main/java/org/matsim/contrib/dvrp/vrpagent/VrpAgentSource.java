/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.*;

import rideSharing.RideShareAgent;
import rideSharing.Run;


public class VrpAgentSource
    implements AgentSource
{
    private final DynActionCreator nextActionCreator;
    private final VrpData vrpData;
    private final VrpOptimizer optimizer;
    private final QSim qSim;
    private final VehicleType vehicleType;
    private final PassengerEngine passengerEngine;




    public VrpAgentSource(DynActionCreator nextActionCreator, VrpData vrpData,
            VrpOptimizer optimizer, QSim qSim, PassengerEngine passengerEngine)
    {
        this(nextActionCreator, vrpData, optimizer, qSim, VehicleUtils.getDefaultVehicleType(), passengerEngine);
    }


    public VrpAgentSource(DynActionCreator nextActionCreator, VrpData vrpData,
            VrpOptimizer optimizer, QSim qSim, VehicleType vehicleType,  PassengerEngine passengerEngine)
    {
        this.nextActionCreator = nextActionCreator;
        this.vrpData = vrpData;
        this.optimizer = optimizer;
        this.qSim = qSim;
        this.vehicleType = vehicleType;
        this.passengerEngine = passengerEngine;
    }
    

    @Override
    public void insertAgentsIntoMobsim()
    {
    	vrpData.clear();
    	
    	VehiclesFactory vehicleFactory = VehicleUtils.getFactory();
    	for(Person p: qSim.getScenario().getPopulation().getPersons().values()){
    		//need to be changed
    		Leg leg = null;
    		Plan plan = p.getSelectedPlan();
    		PlanElement planElement;
    		for(int i = 0; i < plan.getPlanElements().size(); i++){
    			planElement = plan.getPlanElements().get(i);
    			if (planElement instanceof Leg) {
    				leg = ((Leg) planElement);
    	    		if(!leg.equals(null) && leg.getMode().equals(Run.MODE_DRIVER)){
    	                org.matsim.vehicles.Vehicle vehicle = vehicleFactory.createVehicle(Id.createVehicleId(p.getId()), vehicleType);
    	                Activity next = (Activity) plan.getPlanElements().get(i + 1);
    	                if(next.getStartTime() != Double.NEGATIVE_INFINITY){
    	                	leg.setTravelTime(Math.max((next.getStartTime() - leg.getDepartureTime()),1));
    	                }else{
    	                	leg.setTravelTime(3600);
    	                }
    	                if (vrpData.getVehicles().containsKey(vehicle.getId())){
    	                		vrpData.getVehicles().get(vehicle.getId()).addT(leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime());
    	                }else{
    	                Vehicle v = this.vrpData.changeNormalVehicle(vehicle,leg,qSim);
    	                vrpData.addVehicle(v);
    	                }
    	                //leg.setMode(TransportMode.car);
    	    		}
    			}
    		}

    	}
        for(Vehicle vrpVeh: vrpData.getVehicles().values()){
            Id<Vehicle> id = vrpVeh.getId();
            Id<Link> startLinkId = vrpVeh.getStartLink().getId();

            VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator, vrpVeh);
            DynAgent vrpAgent = new DynAgent(Id.createPersonId(id), startLinkId,qSim.getEventsManager(), vrpAgentLogic);
            
            RideShareAgent rideShareAgent;
            QVehicle mobsimVehicle = new QVehicle(vehicleFactory.createVehicle(Id.create(id, org.matsim.vehicles.Vehicle.class), vehicleType));
            if(qSim.getAgentMap().get(Id.createPersonId(id)) instanceof PersonDriverAgentImpl){
            	PersonDriverAgentImpl agent = (PersonDriverAgentImpl) qSim.getAgentMap().get(Id.createPersonId(id));
                rideShareAgent = new RideShareAgent(agent,vrpAgent,vrpData,passengerEngine);
            }else{
            	TransitAgent agent = (TransitAgent) qSim.getAgentMap().get(Id.createPersonId(id));
                rideShareAgent = new RideShareAgent(agent,vrpAgent,vrpData,passengerEngine);
            }

            rideShareAgent.setVehicle(mobsimVehicle);
            mobsimVehicle.setDriver(rideShareAgent);

            //qSim.addParkedVehicle(mobsimVehicle, startLinkId);
            //if(qSim.getAgentMap().containsKey(vrpAgent.getId())){
            //	qSim.getAgentMap().remove(vrpAgent.getId());
            //}
            qSim.insertAgentIntoMobsim(rideShareAgent);
        }
    }
}

