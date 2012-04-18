/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalSimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTime;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;
import org.matsim.core.utils.misc.Time;

public class MultiModalSimEngine implements MobsimEngine, NetworkElementActivator {

	private static Logger log = Logger.getLogger(MultiModalSimEngine.class);

	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;
	
	/*package*/ Netsim qSim;
	/*package*/ MultiModalTravelTime multiModalTravelTime;
	/*package*/ List<MultiModalQLinkExtension> allLinks = null;
	/*package*/ List<MultiModalQLinkExtension> activeLinks;
	/*package*/ List<MultiModalQNodeExtension> activeNodes;
	/*package*/ Queue<MultiModalQLinkExtension> linksToActivate;
	/*package*/ Queue<MultiModalQNodeExtension> nodesToActivate;

	/*package*/ InternalInterface internalInterface = null;

	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	/*package*/ MultiModalSimEngine(Netsim qSim, MultiModalTravelTime multiModalTravelTime) {
		this.qSim = qSim;

		activeLinks = new ArrayList<MultiModalQLinkExtension>();
		activeNodes = new ArrayList<MultiModalQNodeExtension>();
		linksToActivate = new ConcurrentLinkedQueue<MultiModalQLinkExtension>();	// thread-safe Queue!
		nodesToActivate = new ConcurrentLinkedQueue<MultiModalQNodeExtension>();	// thread-safe Queue!

		this.multiModalTravelTime = multiModalTravelTime; 
	}

	@Override
	public Netsim getMobsim() {
		return qSim;
	}

	@Override
	public void onPrepareSim() {
		allLinks = new ArrayList<MultiModalQLinkExtension>();
		for (NetsimLink qLink : this.qSim.getNetsimNetwork().getNetsimLinks().values()) {
			allLinks.add(this.getMultiModalQLinkExtension(qLink));
		}
		this.infoTime = Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime()
				/ INFO_PERIOD)
				* INFO_PERIOD; // infoTime may be < simStartTime, this ensures
		// to print out the info at the very first
		// timestep already
	}

	@Override
	public void doSimStep(double time) {
		moveNodes(time);
		moveLinks(time);
		printSimLog(time);
	}

	/*package*/ void moveNodes(final double time) {
		reactivateNodes();

		ListIterator<MultiModalQNodeExtension> simNodes = this.activeNodes.listIterator();
		MultiModalQNodeExtension node;
		boolean isActive;

		while (simNodes.hasNext()) {
			node = simNodes.next();
			isActive = node.moveNode(time);
			if (!isActive) {
				simNodes.remove();
			}
		}
	}

	/*package*/ void moveLinks(final double time) {
		reactivateLinks();

		ListIterator<MultiModalQLinkExtension> simLinks = this.activeLinks.listIterator();
		MultiModalQLinkExtension link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
			if (!isActive) {
				simLinks.remove();
			}
		}
	}

	/*package*/ void printSimLog(double time) {
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			int nofActiveLinks = this.getNumberOfSimulatedLinks();
			int nofActiveNodes = this.getNumberOfSimulatedNodes();
			log.info("SIMULATION (MultiModalSimEngine) AT " + Time.writeTime(time) 
					+ " #links=" + nofActiveLinks + " #nodes=" + nofActiveNodes);
		}
	}

	@Override
	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (MultiModalQLinkExtension link : this.allLinks) {
			link.clearVehicles();
		}
	}

	@Override
	public void activateLink(MultiModalQLinkExtension link) {
		linksToActivate.add(link);
	}

	@Override
	public void activateNode(MultiModalQNodeExtension node) {
		nodesToActivate.add(node);
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return activeLinks.size();
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		return activeNodes.size();
	}

	/*package*/ void reactivateLinks() {
		if (!linksToActivate.isEmpty()) {
			activeLinks.addAll(linksToActivate);
			linksToActivate.clear();
		}
	}

	/*package*/ void reactivateNodes() {
		if (!nodesToActivate.isEmpty()) {
			activeNodes.addAll(nodesToActivate);
			nodesToActivate.clear();
		}
	}

	/*package*/ MultiModalTravelTime getMultiModalTravelTime() {
		return this.multiModalTravelTime;
	}

	/*package*/ MultiModalQNodeExtension getMultiModalQNodeExtension(NetsimNode qNode) {
		return (MultiModalQNodeExtension) qNode.getCustomAttributes().get(MultiModalQNodeExtension.class.getName());
	}

	/*package*/ MultiModalQLinkExtension getMultiModalQLinkExtension(NetsimLink qLink) {
		return (MultiModalQLinkExtension) qLink.getCustomAttributes().get(MultiModalQLinkExtension.class.getName());
	}
}
