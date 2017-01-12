package rideSharing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

public class DynAgentSource implements AgentSource{
    private final DynActionCreator nextActionCreator;
    private final VrpOptimizer optimizer;
    private final QSim qSim;
    private final VehicleType vehicleType;
    private VrpData vrpData;
    

    public DynAgentSource(DynActionCreator nextActionCreator,
            VrpOptimizer optimizer, QSim qSim, VehicleType vehicleType)
    {
        this.nextActionCreator = nextActionCreator;
        this.optimizer = optimizer;
        this.qSim = qSim;
        this.vehicleType = vehicleType;
    }

	@Override
	public void insertAgentsIntoMobsim() {
        VehiclesFactory vehicleFactory = VehicleUtils.getFactory();
        for (Vehicle vrpVeh : vrpData.getVehicles().values()) {
            Id<Vehicle> id = vrpVeh.getId();
            Id<Link> startLinkId = vrpVeh.getStartLink().getId();

            VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator, vrpVeh);
            DynAgent vrpAgent = new DynAgent(Id.createPersonId(id), startLinkId,
                    qSim.getEventsManager(), vrpAgentLogic);
            QVehicle mobsimVehicle = new QVehicle(vehicleFactory
                    .createVehicle(Id.create(id, org.matsim.vehicles.Vehicle.class), vehicleType));
            vrpAgent.setVehicle(mobsimVehicle);
            mobsimVehicle.setDriver(vrpAgent);

            qSim.addParkedVehicle(mobsimVehicle, startLinkId);
            qSim.insertAgentIntoMobsim(vrpAgent);
        }
	}


}
