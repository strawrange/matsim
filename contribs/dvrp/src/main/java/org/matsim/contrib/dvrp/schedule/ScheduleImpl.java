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

package org.matsim.contrib.dvrp.schedule;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import rideSharing.RideShareServeTask;


public class ScheduleImpl<T extends AbstractTask>
    implements Schedule<T>
{
    private final Vehicle vehicle;

    private final List<T> tasks = new ArrayList<>();
    private final List<T> unmodifiableTasks = Collections.unmodifiableList(tasks);

    private ScheduleStatus status = ScheduleStatus.UNPLANNED;
    private T currentTask = null;
    
    private int endRideShareNumber = 0;
    private int stayTaskNumber = 0;
    
    public void resetSchedule(){
    	this.status = ScheduleStatus.PLANNED;
    }


    public ScheduleImpl(Vehicle vehicle)
    {
        this.vehicle = vehicle;
    }
    
    public void clearTasks(){
    	int size = tasks.size();
    	if(currentTask == null){
    		tasks.removeAll(tasks.subList(0, tasks.size()));
    		this.reduceStayTaskNumber();
    		return;
    	}
    	else {
    		tasks.removeAll(tasks.subList(getCurrentTask().taskIdx +1, tasks.size()));
    		if(currentTask instanceof StayTask ){
        		StayTaskImpl currentStayTask = (StayTaskImpl)currentTask;
        		if(!currentStayTask.getName().equals("wait")){
        			this.reduceStayTaskNumber();
        		}else if(getCurrentTask().taskIdx != size - 1){
        			this.reduceStayTaskNumber();
        		}
        	}else
        		throw new IllegalStateException(); //current task need to be drop off
    	}
    	
    	
    	
 //   	status = ScheduleStatus.UNPLANNED;
    }
    


    @Override
    public Vehicle getVehicle()
    {
        return vehicle;
    }


    @Override
    public List<T> getTasks()
    {
        return unmodifiableTasks;
    }


    @Override
    public int getTaskCount()
    {
        return tasks.size();
    }

    @Override
    public int getEndRideShareNumber(){
    	return this.endRideShareNumber;
    };
    
    @Override
    public void addEndRideShareNumber(){
    	this.endRideShareNumber ++;
    };
    
    @Override
    public int getStayTaskNumber(){
    	return this.stayTaskNumber;
    };
    
    @Override
    public void addStayTaskNumber(){
    	this.stayTaskNumber ++;
    };

    @Override
    public void reduceStayTaskNumber(){
    	this.stayTaskNumber --;
    }
    
    public void addTask(T task)
    {
        addTask(tasks.size(), task);
    }


    public void addTask(int taskIdx, T task)
    {
        validateArgsBeforeAddingTask(taskIdx, task);

        if (status == ScheduleStatus.UNPLANNED) {
            status = ScheduleStatus.PLANNED;
        }

        tasks.add(taskIdx, task);
        task.schedule = this;
        task.taskIdx = taskIdx;
        task.status = TaskStatus.PLANNED;

        // update idx of the existing tasks
        for (int i = taskIdx + 1; i < tasks.size(); i++) {
            tasks.get(i).taskIdx = i;
        }
    }
    
    public void addTaskWithoutCheck(int taskIdx, T task)
    {

        if (status == ScheduleStatus.UNPLANNED) {
            status = ScheduleStatus.PLANNED;
        }

        tasks.add(taskIdx, task);
        task.schedule = this;
        task.taskIdx = taskIdx;
        task.status = TaskStatus.PLANNED;

        // update idx of the existing tasks
        for (int i = taskIdx + 1; i < tasks.size(); i++) {
            tasks.get(i).taskIdx = i;
        }
    }


    private void validateArgsBeforeAddingTask(int taskIdx, Task task)
    {
        failIfCompleted();
        if (status == ScheduleStatus.STARTED && taskIdx < currentTask.getTaskIdx()) {
            throw new IllegalStateException();
        }

        double beginTime = task.getBeginTime();
        double endTime = task.getEndTime();
        Link beginLink = Tasks.getBeginLink(task);
        Link endLink = Tasks.getEndLink(task);
        int taskCount = tasks.size();

        if (taskIdx < 0 || taskIdx > taskCount) {
            throw new IllegalArgumentException();
        }

        if (beginTime > endTime) {
            throw new IllegalArgumentException();
        }

        if (taskIdx > 0) {
            Task previousTask = tasks.get(taskIdx - 1);

            if (Math.round(previousTask.getEndTime() * 100.0) / 100.0 != Math.round(beginTime * 100.0) / 100.0)  {
                throw new IllegalArgumentException();
            }

            if (Tasks.getEndLink(previousTask) != beginLink) {
            	Logger.getLogger(getClass()).error("Last task End link: "+Tasks.getEndLink(previousTask).getId()+ " ; next Task start link: "+ beginLink.getId());
                throw new IllegalArgumentException();
            }
        }
        else  { //taskIdx == 0
            if (vehicle.getStartLink() != beginLink) {
                throw new IllegalArgumentException();
            }
        }

        if (taskIdx < taskCount) {
            Task nextTask = tasks.get(taskIdx);// currently at taskIdx, but soon at taskIdx+1

            if (Math.round(nextTask.getBeginTime() * 100.0) / 100.0 != Math.round(endTime * 100.0) / 100.0) {
                throw new IllegalArgumentException();
            }

            if (Tasks.getBeginLink(nextTask) != endLink) {
                throw new IllegalArgumentException();
            }
        }
    }

    private void validateArgsAfterReroute(List<T> tasks, int rerouteIdx)
    {
    	for(int taskIdx = rerouteIdx; taskIdx < tasks.size(); taskIdx++){
    		T task = tasks.get(taskIdx);
    		failIfCompleted();
    		//if (status == ScheduleStatus.STARTED) {
    			//throw new IllegalStateException();
    		//}

    		double beginTime = task.getBeginTime();
    		double endTime = task.getEndTime();
    		Link beginLink = Tasks.getBeginLink(task);
    		Link endLink = Tasks.getEndLink(task);
    		int taskCount = tasks.size();


    		if (beginTime > endTime) {
    			throw new IllegalArgumentException();
    		}

    		if (taskIdx > 0) {
    			Task previousTask = tasks.get(taskIdx - 1);

    			if (Math.round(previousTask.getEndTime() * 100.0) / 100.0 != Math.round(beginTime * 100.0) / 100.0)  {
    				throw new IllegalArgumentException();
    			}

    			if (Tasks.getEndLink(previousTask) != beginLink) {
    				Logger.getLogger(getClass()).error("Last task End link: "+Tasks.getEndLink(previousTask).getId()+ " ; next Task start link: "+ beginLink.getId());
    				throw new IllegalArgumentException();
    			}
    		}
    		else  { //taskIdx == 0
    			if (vehicle.getStartLink() != beginLink) {
    				throw new IllegalArgumentException();
    			}
    		}

    		if (taskIdx < taskCount - 1) {
    			Task nextTask = tasks.get(taskIdx + 1);

    			if (Math.round(nextTask.getBeginTime() * 100.0) / 100.0 != Math.round(endTime * 100.0) / 100.0) {
    				throw new IllegalArgumentException();
    			}

    			if (Tasks.getBeginLink(nextTask) != endLink) {
    				throw new IllegalArgumentException();
    			}
    		}
    	}
    }


    @Override
    public void removeLastTask()
    {
        removeTaskImpl(tasks.size() - 1);
    }


    @Override
    public void removeTask(T task)
    {
        removeTaskImpl(task.getTaskIdx());
    }


    private void removeTaskImpl(int taskIdx)
    {
        failIfUnplanned();
        failIfCompleted();

        AbstractTask task = tasks.get(taskIdx);

        if (task.getStatus() != TaskStatus.PLANNED) {
            throw new IllegalStateException();
        }

        tasks.remove(taskIdx);

        for (int i = taskIdx; i < tasks.size(); i++) {
            tasks.get(i).taskIdx = i;
        }

        if (tasks.size() == 0) {
            status = ScheduleStatus.UNPLANNED;
        }
    }


    @Override
    public T getCurrentTask()
    {
        failIfNotStarted();//status != ScheduleStatus.STARTED
        return currentTask;
    }
    
    @Override
    public T getNextTask()
    {
        failIfNotStarted();//status != ScheduleStatus.STARTED
        if (tasks.size() == currentTask.taskIdx + 1){
        	return null;
        }
        return tasks.get(currentTask.taskIdx + 1);
    }


    @Override
    public T nextTask()
    {
        failIfUnplanned();
        failIfCompleted();

        nextTaskImpl();

        return currentTask;
    }


    private void nextTaskImpl()
    {
        int nextIdx;

        if (status == ScheduleStatus.PLANNED) {
            status = ScheduleStatus.STARTED;
            nextIdx = 0;
        }
        else { // STARTED
            currentTask.status = TaskStatus.PERFORMED;
            // TODO ??            currentTask.setTaskTracker(null);
            nextIdx = currentTask.taskIdx + 1;
        }

        if (nextIdx == tasks.size()) {
            currentTask = null;
            status = ScheduleStatus.COMPLETED;
        }
        else {
            currentTask = tasks.get(nextIdx);
            currentTask.status = TaskStatus.STARTED;
        }
    }


    @Override
    public void addTaskInOrder(List<T> prepareTasks){
    	if (tasks.size() == 0 || tasks.size() == this.getStayTaskNumber()){
    		for(int i = 0; i < 4; i++){
    			addTask(prepareTasks.get(i));
    		}
    		prepareTasks.clear();
    	}
    	
    	else {
    		int currentTaskIdx = 0;
    		int cycleIdx = 0;
    		
    		AbstractTask test= this.currentTask;
    		
    		if(test != null){
    			currentTaskIdx = this.getCurrentTask().getTaskIdx();
    			cycleIdx = (currentTaskIdx - this.getStayTaskNumber() - this.getEndRideShareNumber()) / 4 + 1;
    		}
    		
    		if ((cycleIdx * 4 + this.getStayTaskNumber() + this.getEndRideShareNumber()) == tasks.size()){
    			for(int j = 0; j < 4; j++){
    				addTask(prepareTasks.get(j));
    			}
    			prepareTasks.clear();
    		}
    		else for (int i = cycleIdx * 4 + this.getStayTaskNumber() + this.getEndRideShareNumber(); i < tasks.size(); i++){
    	
    			if(prepareTasks.get(0).getDistanceDifference() < tasks.get(i).getDistanceDifference()){
    				for(int j = 0; j < 4; j++){
    					addTaskWithoutCheck(i+j, prepareTasks.get(j));
    				}
    				prepareTasks.clear();
    				this.reroute(i); //reroute from the fist inserted task
    				break;
    			}
    		}
    		if(prepareTasks.size()!=0){
    			for(int j = 0; j < 4; j++){
    				addTask(prepareTasks.get(j));
    			}  
    			prepareTasks.clear();
    		}
    	}
    	
 	
	//just wait (and be ready) till the end of the vehicle's time window (T1)
    	int size = tasks.size();
    	RideShareServeTask lastTask = (RideShareServeTask)tasks.get(size - 1);
    	double t = lastTask.getEndTime();
    	Link lastLink = lastTask.getLink();
        double tEnd = Math.max(t, vehicle.getT1());
        addTask((T) new StayTaskImpl(t, tEnd, lastLink, "wait"));
        this.addStayTaskNumber();
    }



    
    @Override
    public ScheduleStatus getStatus()
    {
        return status;
    }


    @Override
    public double getBeginTime()
    {
        failIfUnplanned();
        return tasks.get(0).getBeginTime();
    }


    @Override
    public double getEndTime()
    {
        failIfUnplanned();
        return tasks.get(tasks.size() - 1).getEndTime();
    }


    @Override
    public String toString()
    {
        return "Schedule_" + vehicle.getId();
    }


    private void failIfUnplanned()
    {
        if (status == ScheduleStatus.UNPLANNED) {
            throw new IllegalStateException();
        }
    }


    private void failIfCompleted()
    {
        if (status == ScheduleStatus.COMPLETED) {
            throw new IllegalStateException();
        }
    }


    private void failIfNotStarted()
    {
        if (status != ScheduleStatus.STARTED) {
            throw new IllegalStateException();
        }
    }

	@SuppressWarnings("unchecked")
	@Override
	public T getDropoffTask() {
		// TODO Auto-generated method stub
		if(currentTask == null ){
			return null;
		}
		for(int i = currentTask.taskIdx + 1; i < tasks.size(); i++){
			if(tasks.get(i).getType().equals(Task.TaskType.STAY) ){
				if(tasks.get(i) instanceof RideShareServeTask){
					final RideShareServeTask serveTask = (RideShareServeTask)tasks.get(i);
					if(!serveTask.isPickup()){
						return (T) serveTask;
					}
				}
			}
		}
		return null;
	}
	@Override
	public T getLastTask(){
		if(getTaskCount() <= 1){
			return null;
		}else{
			return tasks.get(getTaskCount() - 2);
		}
	}


	@Override
	public void reroute(int rerouteIdx) {
		
		final LeastCostPathCalculator router = this.vehicle.getAgentLogic().getOptimizer().getRouter();
		final TravelTime travelTime = new FreeSpeedTravelTime();;
		
    	for(int i = rerouteIdx; i < tasks.size(); i++){
    		if(tasks.get(i) instanceof DriveTask){		    

    			DriveTaskImpl tempTask = (DriveTaskImpl)tasks.get(i);
    			Link fromLink = tempTask.getFromLink();
    			double startTime = tempTask.getBeginTime();
    		    
    			if (rerouteIdx == 0 && i == 0){
    				DriveTask lastDriveTask = (DriveTask)tasks.get(4);
        			fromLink = lastDriveTask.getPath().getFromLink();
        			VrpPathWithTravelData path = (VrpPathWithTravelData) lastDriveTask.getPath();
        			startTime = path.getDepartureTime();
    		    }
    			
    			else {
    				if(tasks.get(i - 1) instanceof StayTask){
    			
    				StayTask lastStayTask = (StayTask)tasks.get(i - 1);
    				fromLink = lastStayTask.getLink();
    				startTime = lastStayTask.getEndTime();
    			}
    			
    			if(tasks.get(i - 1) instanceof DriveTask){
        			DriveTask lastDriveTask = (DriveTask)tasks.get(i - 1);
        			fromLink = lastDriveTask.getPath().getToLink();
        			VrpPathWithTravelData path = (VrpPathWithTravelData) lastDriveTask.getPath();
        			startTime = path.getArrivalTime();
        		}
    			
    			}
    			
    			Link toLink = tempTask.getPath().getToLink();
    			
    			
    			VrpPathWithTravelData newPath = VrpPaths.calcAndCreatePath(fromLink, toLink, startTime, router,
    	                travelTime);
    			
    			double dis = tempTask.getDistanceDifference();
    			tempTask = new DriveTaskImpl(newPath, fromLink, toLink);
    			tempTask.setDistanceDifference(dis);
    			
    			tasks.remove(i);
    			addTaskWithoutCheck(i, (T) tempTask);
    		}
    		
    		if(tasks.get(i) instanceof RideShareServeTask){
    			
    			RideShareServeTask tempTask = (RideShareServeTask)tasks.get(i);
    			double t1 = tempTask.getBeginTime();
    			double t2;
    			
    			if(tasks.get(i - 1) instanceof RideShareServeTask){
    				RideShareServeTask lastStayTask = (RideShareServeTask)tasks.get(i - 1);
    				t1 = lastStayTask.getEndTime();
    			}
    			
    			if(tasks.get(i - 1) instanceof DriveTask){
        			DriveTask lastDriveTask = (DriveTask)tasks.get(i - 1);
        			VrpPathWithTravelData path = (VrpPathWithTravelData) lastDriveTask.getPath();
        			t1 = path.getArrivalTime();
        		}
    			
    			
    			if(tempTask.isPickup())
    				t2 = t1 + 120; //PICKUP_DURATION
    			else
    				t2 = t1 + 60;//DROPOFF_DURATION
    			
    			tempTask.setBeginTime(t1);
    			tempTask.setEndTime(t2);
    			
    			tasks.remove(i);
    			addTaskWithoutCheck(i, (T) tempTask); 
    		}    		
    	}
    	validateArgsAfterReroute(tasks, rerouteIdx);
	}
}
