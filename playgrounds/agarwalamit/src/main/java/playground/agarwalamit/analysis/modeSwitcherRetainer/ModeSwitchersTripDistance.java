/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.modeSwitcherRetainer;

import java.io.BufferedWriter;
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionHandler;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.PersonFilter;


/**
 * This will first find mode switchers and then returns trip distances in groups. 
 *<p>
 * @author amit
 */

public class ModeSwitchersTripDistance {

	private static final Logger LOG = Logger.getLogger(ModeSwitchersTripDistance.class);

	public ModeSwitchersTripDistance(){
		this(null, null);
	}

	private final String userGroup;
	private final PersonFilter pf;

	public ModeSwitchersTripDistance (final String userGroup, final PersonFilter personFilter) {
		this.pf = personFilter;
		this.userGroup = userGroup;

		if( (userGroup==null && personFilter!=null) || (userGroup!=null && personFilter==null) ) {
			throw new RuntimeException("Either of user group or person filter is null.");
		} else if(userGroup!=null && personFilter!=null) {
			LOG.info("Usergroup filtering is used, result will include persons from given user group only.");
		}
	}

	private final Comparator<Tuple<String, String>> comparator = new Comparator<Tuple<String, String>>() {
		@Override
		public int compare(Tuple<String, String> o1, Tuple<String, String> o2) {
			return o1.toString().compareTo(o2.toString());
		}
	};

	private final SortedMap<Tuple<String, String>, ModeSwitcherInfoCollector> modeSwitchType2InfoCollector = new TreeMap<>(comparator);

	public static void main(String[] args) {
		String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runCase : runCases){
			ModeSwitchersTripDistance mstd = new ModeSwitchersTripDistance();
			mstd.processEventsFiles(dir+runCase, 1000, 1500);
			mstd.writeResults(dir+runCase+"/analysis/");
		}
	}

	public void processEventsFiles (final String eventsDir, final int firstIteration, final int lastIteration){
		// data from event files
		String eventsFileFirstIt = eventsDir+"/ITERS/it."+firstIteration+"/"+firstIteration+".events.xml.gz";
		String eventsFileLastIt = eventsDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";

		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(eventsDir+"/output_network.xml.gz", eventsDir+"/output_config.xml");

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDistsItFirst = getPerson2mode2TripDistances(eventsFileFirstIt,sc);
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDistsTtLast = getPerson2mode2TripDistances(eventsFileLastIt,sc);

		for(Id<Person> pId : person2ModeTravelDistsItFirst.keySet()){

			if(this.userGroup !=null  && ! this.pf.getUserGroupAsStringFromPersonId(pId).equals(this.userGroup)) {
				continue; // if using person filtering and person does not belong to desired user group, dont include it in the analysis
			}

			if(person2ModeTravelDistsTtLast.containsKey(pId) ) {

				int numberOfLegs = 0;
				if(person2ModeTravelDistsTtLast.get(pId).size() != person2ModeTravelDistsItFirst.get(pId).size()){
					//	if person does not have same number of trips as in first iteration
					LOG.warn("Person "+pId+" do not have the same number of trip legs in the two maps. This could be due to stuck and abort event. "
							+ "\n Thus including only minimum number of legs (using the common trips) for that person.");
					numberOfLegs = Math.min(person2ModeTravelDistsTtLast.get(pId).size(),person2ModeTravelDistsItFirst.get(pId).size());

				} else numberOfLegs = person2ModeTravelDistsItFirst.get(pId).size();

				for(int ii=0; ii<numberOfLegs;ii++){

					Tuple<String, Double> firstItMode = person2ModeTravelDistsItFirst.get(pId).get(ii);
					Tuple<String, Double> lastItMode = person2ModeTravelDistsTtLast.get(pId).get(ii);

					Tuple<String, String> modeSwitchType = new Tuple<>(firstItMode.getFirst(), lastItMode.getFirst());
					storeTripDistanceInfo(pId, modeSwitchType, new Tuple<>(firstItMode.getSecond(), lastItMode.getSecond()));
				} 

			} else if(!person2ModeTravelDistsTtLast.containsKey(pId)) {
				LOG.warn("Person "+pId+ "is not present in the last iteration map. This person is thus not included in the results. Probably due to stuck and abort event.");
			}
		}
	}

	private void storeTripDistanceInfo(final Id<Person> personId, final Tuple<String, String> modeSwitchTyp, final Tuple<Double, Double> travelDistances){

		ModeSwitcherInfoCollector infoCollector = this.modeSwitchType2InfoCollector.get(modeSwitchTyp);
		if (infoCollector == null ) {
			infoCollector = new ModeSwitcherInfoCollector();
		}

		infoCollector.addPersonToList(personId);
		infoCollector.addToFirstIterationStats(travelDistances.getFirst());
		infoCollector.addToLastIterationStats(travelDistances.getSecond());

		this.modeSwitchType2InfoCollector.put(modeSwitchTyp, infoCollector);
	}

	private Map<Id<Person>, List<Tuple<String, Double>>> getPerson2mode2TripDistances(final String eventsFile, final Scenario sc){

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		LegModeRouteDistanceDistributionHandler distHandler = new LegModeRouteDistanceDistributionHandler(sc);
		events.addHandler(distHandler);
		reader.readFile(eventsFile);

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripDists = distHandler.getMode2PersonId2TravelDistances();
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDists = new HashMap<>();

		for(String mode : mode2Person2TripDists.keySet()){
			for (Id<Person> p : mode2Person2TripDists.get(mode).keySet()){
				for(Double d :mode2Person2TripDists.get(mode).get(p)){
					Tuple<String, Double> mode2TripDist = new Tuple<>(mode, d);

					if (person2ModeTravelDists.containsKey(p)){
						List<Tuple<String, Double>> mode2TripDistList  = person2ModeTravelDists.get(p);
						mode2TripDistList.add(mode2TripDist);
					} else {
						List<Tuple<String, Double>> mode2TripDistList = new ArrayList<>();
						mode2TripDistList.add(mode2TripDist);
						person2ModeTravelDists.put(p, mode2TripDistList);
					}
				}
			}
		}
		return person2ModeTravelDists;
	}

	public void writeResults(final String outputFolder){
		String outFile = outputFolder+"/modeSwitchersTripDistances.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("firstMode \t lastMode \t numberOfLegs \t totalTripDistancesForFirstIterationInKm \t totalTripDistancesForLastIterationInKm \n");

			for(Tuple<String, String> str: this.modeSwitchType2InfoCollector.keySet()){
				ModeSwitcherInfoCollector infoCollector = this.modeSwitchType2InfoCollector.get(str);
				writer.write(str.getFirst()+"\t"+
						str.getSecond()+"\t"+
						infoCollector.getNumberOfLegs()+"\t" +
						infoCollector.getFirstIterationStats() / 1000.0 + "\t"+
						infoCollector.getLastIterationStats() / 1000.0 +
						"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
		LOG.info("Data is written to "+outFile);
	}
}
