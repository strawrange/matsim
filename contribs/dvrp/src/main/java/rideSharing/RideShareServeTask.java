package rideSharing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

public class RideShareServeTask extends StayTaskImpl
{
    private final RideShareRequest request;
    private final boolean isPickup;//pickup or drop off


    public RideShareServeTask(double beginTime, double endTime, Link link, boolean isPickup,
            RideShareRequest request)
    {
        super(beginTime, endTime, link, isPickup ? "pickup" : "dropoff");
        this.request = request;
        this.isPickup = isPickup;
    }


    public RideShareRequest getRequest()
    {
        return request;
    }


    public boolean isPickup()
    {
        return isPickup;
    }
}
