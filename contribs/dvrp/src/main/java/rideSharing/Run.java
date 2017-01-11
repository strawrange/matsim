package rideSharing;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.VrpDataImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dynagent.run.DynQSimConfigConsistencyChecker;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

public class Run {
	public static final String MODE_DRIVER = "ride_share_driver";
	public static final String MODE_PASSENGER = "ride_share_passenger";
    private static final String RIDE_SHARE_GROUP_NAME = "ride_share";
    private static final String RIDE_SHARE_FILE = "rideShareFile";


    public static void run(boolean otfvis)
    {
        String configFile = "./src/main/resources/ride_share/one_taxi_config.xml";
        run(configFile, otfvis);
    }


    public static void run(String configFile,  boolean otfvis)
    {
        ConfigGroup rideShareCfg = new ConfigGroup(RIDE_SHARE_GROUP_NAME) {};
        OTFVisConfigGroup otfvisConfig = new OTFVisConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile,otfvisConfig,rideShareCfg);
        config.qsim().setMainModes(Arrays.asList(TransportMode.car,MODE_DRIVER));
        config.addConfigConsistencyChecker(new DynQSimConfigConsistencyChecker());
        config.checkConsistency();
        Scenario scenario = ScenarioUtils.loadScenario(config);

       final VrpData vrpData = new VrpDataImpl();
       //new VehicleReader(scenario.getNetwork(),vrpData).createVehicleFromPlan(scenario.getPopulation());

       new VehicleReader(scenario.getNetwork(), vrpData).readFile(rideShareCfg.getValue(RIDE_SHARE_FILE));

        Controler controler = new Controler(scenario);
       controler.addOverridingModule(new AbstractModule() {
            public void install()
            {
                addRoutingModuleBinding(MODE_PASSENGER).toInstance(new DynRoutingModule(MODE_PASSENGER));
                //addRoutingModuleBinding(MODE_DRIVER).toInstance(new DynRoutingModule(MODE_DRIVER));
                bind(VrpData.class).toInstance(vrpData);
            }
        });
        controler.addOverridingModule(new DynQSimModule<>(RideShareQSimProvider.class));
        //controler.addOverridingModule(new DynQSimModule<>(RandomDynQSimProvider.class));
        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule());
        }


        controler.run();
    }


    public static void main(String... args)
    {
        run(true);
    }
}
