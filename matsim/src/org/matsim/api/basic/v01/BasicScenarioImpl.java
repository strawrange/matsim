/* *********************************************************************** *
 * project: org.matsim.*
 * BasicScenarioImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.api.basic.v01;

import org.matsim.api.basic.v01.facilities.BasicFacilities;
import org.matsim.api.basic.v01.network.BasicNetwork;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.basic.v01.BasicPopulationImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.households.BasicHousehold;
import org.matsim.core.basic.v01.households.BasicHouseholds;
import org.matsim.core.basic.v01.households.BasicHouseholdsImpl;
import org.matsim.core.basic.v01.vehicles.BasicVehicles;
import org.matsim.core.basic.v01.vehicles.BasicVehiclesImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

public class BasicScenarioImpl implements BasicScenario {

	private final Config config;
	private final BasicNetwork<?, ?> network;
	private final BasicFacilities facilities;
	private final BasicPopulation<?> population;
	private BasicHouseholds<BasicHousehold> households;
	private BasicVehiclesImpl vehicles;
	
	public BasicScenarioImpl() {
		this.config = new Config();
		this.config.addCoreModules();
		this.network = new NetworkLayer();  // TODO shoul be changed to a basic implementation
		this.facilities = new FacilitiesImpl(); // TODO shoul be changed to a basic implementation
		this.population = new BasicPopulationImpl();
		this.households = new BasicHouseholdsImpl();
		this.vehicles = new BasicVehiclesImpl();
	}
	
	public BasicNetwork<?, ?> getNetwork() {
		return this.network;
	}

	public BasicFacilities getFacilities() {
		return this.facilities;
	}

	public BasicPopulation<?> getPopulation() {
		return this.population;
	}

	public Config getConfig() {
		return this.config;
	}

	public Id createId(String string) {
		return new IdImpl(string);
	}

	public Coord createCoord(double x, double y) {
		return new CoordImpl(x, y);
	}

	public BasicHouseholds<BasicHousehold> getHouseholds() {
		return this.households;
	}

	public BasicVehicles getVehicles(){
		return this.vehicles;
	}
	
}
