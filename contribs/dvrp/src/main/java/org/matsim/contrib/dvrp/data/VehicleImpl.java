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

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;


public class VehicleImpl
    implements Vehicle
{
    private final Id<Vehicle> id;
    private Link startLink;
    private final double capacity;
    private final int index = 0;

    // TW for vehicle
    private ArrayList<Double> t0 = new ArrayList<Double>();
    private ArrayList<Double> t1 = new ArrayList<Double>();
    
    //initialized

    private Schedule<? extends AbstractTask> schedule= new ScheduleImpl<AbstractTask>(this);

    private VrpAgentLogic agentLogic;


    public VehicleImpl(Id<Vehicle> id, Link startLink, double capacity, double t0, double t1)
    {
        this.id = id;
        this.startLink = startLink;
        this.capacity = capacity;
        this.t0.add((Double)t0);
        this.t1.add((Double)t1);

        //schedule = new ScheduleImpl<AbstractTask>(this);
    }
    
    public void addT(Double t0, Double t1){
    	this.t0.add(t0);
    	this.t1.add(t1);
    }
    
    public void removeT(){
    	if(this.t0.size() == 0){
    		return;
    	}
    	this.t0.remove(index);
    	this.t1.remove(index);
    }



	@Override
    public Id<Vehicle> getId()
    {
        return id;
    }


    @Override
    public Link getStartLink()
    {
        return startLink;
    }
    
    @Override
    public void setStartLink(Link link)
    {
        this.startLink = link;
    }

    @Override
    public double getCapacity()
    {
        return capacity;
    }


    @Override
    public double getT0()
    {
        return t0.size()==0?0:t0.get(index);
    }


    @Override
    public double getT1()
    {
        return t1.size()==0?0:t1.get(index);
    }


    @Override
    public Schedule<? extends AbstractTask> getSchedule()
    {
        return schedule;
    }

    @SuppressWarnings("unchecked")
	@Override
    public void setSchedule(Schedule<? extends Task> schedule) {
		this.schedule = (Schedule<? extends AbstractTask>) schedule;
	}



	@Override
    public VrpAgentLogic getAgentLogic()
    {
        return agentLogic;
    }


    @Override
    public void setAgentLogic(VrpAgentLogic agentLogic)
    {
        this.agentLogic = agentLogic;
    }


    @Override
    public String toString()
    {
        return "Vehicle_" + id;
    }


    @Override
    public void setT1(double t1)
    {
        this.t1.add(t1);
    }


    @Override
    public void resetSchedule()
    {
        schedule = new ScheduleImpl<AbstractTask>(this);
    }
}
