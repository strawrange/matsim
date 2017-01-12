package rideSharing;

import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class RideShareActionCreator implements VrpAgentLogic.DynActionCreator
{
    private final PassengerEngine passengerEngine;
    private final MobsimTimer timer;


    public RideShareActionCreator(PassengerEngine passengerEngine, MobsimTimer timer)
    {
        this.passengerEngine = passengerEngine;
        this.timer = timer;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        switch (task.getType()) {
            case DRIVE:
            	return VrpLegs.createLegWithOfflineTracker((DriveTask)task, timer);
            case STAY:
                if (task instanceof RideShareServeTask) { //PICKUP or DROPOFF
                    final RideShareServeTask serveTask = (RideShareServeTask)task;
                    final RideShareRequest request = serveTask.getRequest();

                    if (serveTask.isPickup()) {
                        return new SinglePassengerPickupActivity(passengerEngine, serveTask,
                                request, RideShareOptimizer.PICKUP_DURATION, "RideSharePickup");
                    }
                    else {
                        return new SinglePassengerDropoffActivity(passengerEngine, serveTask,
                                request, "RideShareDropoff");
                    }
                }
                else { //WAIT
                    return new VrpActivity("RideShareStay", (StayTask)task);
                }
        }

        throw new RuntimeException();
    }
}

