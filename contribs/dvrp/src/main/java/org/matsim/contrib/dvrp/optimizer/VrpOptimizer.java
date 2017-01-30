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

package org.matsim.contrib.dvrp.optimizer;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.LeastCostPathCalculator;


public interface VrpOptimizer
{
    /**
     * This function can be generalized (in the future) to encompass request modification,
     * cancellation etc.
     */
	public LeastCostPathCalculator getRouter() ;
	
    public void setVehicleSchedule(VrpData vrpData);
    
    void requestSubmitted(Request request);
    
    public void driveRequestSubmitted(Request request, double now, Id<org.matsim.contrib.dvrp.data.Vehicle> id);

    void nextTask(Schedule<? extends Task> schedule);
    
    void distanceCalculator(AbstractTask task, Request request, Vehicle vehicle);
    
    public void updateSchedule(Vehicle veh, Schedule<AbstractTask> s);

	double getQsimEndTime();
}
