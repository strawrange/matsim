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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.Vehicle;


public interface VrpData
{
    Map<Id<org.matsim.contrib.dvrp.data.Vehicle>, org.matsim.contrib.dvrp.data.Vehicle> getVehicles();


    Map<Id<Request>, Request> getRequests();


    void addRequest(Request request);


    void clearRequestsAndResetSchedules();
    

	void addVehicle(org.matsim.contrib.dvrp.data.Vehicle vehicle);


	org.matsim.contrib.dvrp.data.Vehicle changeNormalVehicle(Vehicle vehicle, Leg leg, QSim qsim);

}