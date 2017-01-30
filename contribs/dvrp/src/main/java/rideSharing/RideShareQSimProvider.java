package rideSharing;


import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.TripPrebookingManager;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.DefaultRoutingModules;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RideShareQSimProvider implements Provider<Mobsim>{

	 private final Scenario scenario;
	    private final EventsManager events;
	    private final Collection<AbstractQSimPlugin> plugins;
	    private final VrpData vrpData;


	    @Inject
	    public RideShareQSimProvider(Scenario scenario, EventsManager events,
	            Collection<AbstractQSimPlugin> plugins, VrpData vrpData)
	    {
	        this.scenario = scenario;
	        this.events = events;
	        this.plugins = plugins;
	        this.vrpData = vrpData;
	    }


	    @Override
	    public Mobsim get()
	    {
	        QSim qSim = QSimUtils.createQSim(scenario,events, plugins);
	        
	        RideShareOptimizer optimizer = new RideShareOptimizer(scenario,vrpData, qSim);
	        //qSim.addQueueSimulationListeners(optimizer);
	        
	        PassengerEngine passengerEngine = new PassengerEngine(Run.MODE_PASSENGER, events,
	                new RideShareRequestCreator(), optimizer,  vrpData,scenario.getNetwork(), qSim);
	        qSim.addMobsimEngine(passengerEngine);
	        qSim.addDepartureHandler(passengerEngine);
	        
	        //TripPrebookingManager tripPrebookManager = new TripPrebookingManager(passengerEngine);
	        //qSim.addQueueSimulationListeners(tripPrebookManager);
	        
	        RideShareActionCreator actionCreator = new RideShareActionCreator(passengerEngine,
	                qSim.getSimTimer());
	        qSim.addAgentSource(new VrpAgentSource(actionCreator, vrpData, optimizer, qSim, passengerEngine));
	        
	        return qSim;
	    }
}

