package rideSharing;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.Facility;

public class RideShareRoutingModule implements RoutingModule{
	
    private final String mode;
    private final double MaxRideSharingTravelTime = Double.POSITIVE_INFINITY;

    
    public RideShareRoutingModule(String mode)
    {
        this.mode = mode;
        
    }
    

	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		// TODO Auto-generated method stub
		/*Activity last;
		Activity next;
		for(int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++){
			if(person.getSelectedPlan().getPlanElements().get(i) instanceof Leg ){
				last = (Activity) person.getSelectedPlan().getPlanElements().get(i - 1);
				next = (Activity) person.getSelectedPlan().getPlanElements().get(i + 1); 
				Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(i);
				if(leg.getDepartureTime() == departureTime){
					leg.setDepartureTime(Math.min(departureTime,last.getEndTime()));
					if(next.getStartTime() != Double.NEGATIVE_INFINITY){
						leg.setTravelTime(Math.max(next.getStartTime() - departureTime,1));
					}else{
						leg.setTravelTime(MaxRideSharingTravelTime);
					}
					leg.setRoute(new LinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId()));
					return Collections.singletonList(leg);
				}
			}
		}
		*/
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(MaxRideSharingTravelTime);
		leg.setRoute(new LinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId()));
		return Collections.singletonList(leg);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO Auto-generated method stub
		return EmptyStageActivityTypes.INSTANCE;
	}

}
