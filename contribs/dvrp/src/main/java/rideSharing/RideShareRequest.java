package rideSharing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class RideShareRequest extends RequestImpl
implements PassengerRequest
{
private final MobsimPassengerAgent passenger;
private final Link fromLink;
private final Link toLink;


public RideShareRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink,
        Link toLink, double time)
{
    //I want a taxi now, i.e. t0 == t1 == submissionTime
    super(id, 1, time, time, time);
    this.passenger = passenger;
    this.fromLink = fromLink;
    this.toLink = toLink;
}


@Override
public Link getFromLink()
{
    return fromLink;
}


@Override
public Link getToLink()
{
    return toLink;
}


@Override
public MobsimPassengerAgent getPassenger()
{
    return passenger;
}
}
