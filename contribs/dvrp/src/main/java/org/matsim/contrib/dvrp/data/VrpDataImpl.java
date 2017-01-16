/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;


/**
 * @author michalm
 */
public class VrpDataImpl
    implements VrpData
{
    private final Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<>();
    private final Map<Id<Request>, Request> requests = new LinkedHashMap<>();

    private final Map<Id<Vehicle>, Vehicle> unmodifiableVehicles = Collections
            .unmodifiableMap(vehicles);
    private final Map<Id<Request>, Request> unmodifiableRequests = Collections
            .unmodifiableMap(requests);


    @Override
    public Map<Id<Vehicle>, Vehicle> getVehicles()
    {
        return unmodifiableVehicles;
    }


    @Override
    public Map<Id<Request>, Request> getRequests()
    {
        return unmodifiableRequests;
    }


    @Override
    public void addVehicle(Vehicle vehicle)
    {
        vehicles.put(vehicle.getId(), vehicle);
    }


    @Override
    public void addRequest(Request request)
    {
        requests.put(request.getId(), request);
    }


    @Override
    public void clearRequestsAndResetSchedules()
    {
        for (Vehicle v : vehicles.values()) {
            v.resetSchedule();
        }

        requests.clear();
    }


	@Override
	public Vehicle changeNormalVehicle(org.matsim.vehicles.Vehicle vehicle, Leg leg, QSim qsim) {
		// TODO Auto-generated method stub
			Vehicle v = new VehicleReader(qsim.getScenario().getNetwork(),this).createVehicle(Id.create(vehicle.getId(),Vehicle.class), leg.getRoute().getStartLinkId(),leg.getDepartureTime(),leg.getDepartureTime() + leg.getTravelTime());
			return v;

	}
}
