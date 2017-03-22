package rideSharing;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.functions.ActivityTypeOpeningIntervalCalculator;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class RideShareActivityScoring implements ActivityScoring {
	
	private double score;
	CharyparNagelScoringParameters params;

	public RideShareActivityScoring(final CharyparNagelScoringParameters params) {
		this.params = params;
	}
	
	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void handleFirstActivity(Activity act) {
		if(act.getType().equals("RideSharePickup") || act.getType().equals("RideShareDropoff")){
			score =+ ( - params.marginalUtilityOfPerforming_s - params.marginalUtilityOfWaiting_s) * ((act.getEndTime() - act.getStartTime()) / 3600);
			}else if (act.getType().equals("RideShareStay")){
				score =+ (- params.marginalUtilityOfPerforming_s - params.marginalUtilityOfWaiting_s) * ((act.getEndTime() - act.getStartTime()) / 3600);
				}

	}

	@Override
	public void handleActivity(Activity act) {
		if(act.getType().equals("RideSharePickup") || act.getType().equals("RideShareDropoff")){
			score =+ ( - params.marginalUtilityOfPerforming_s - params.marginalUtilityOfWaiting_s) * ((act.getEndTime() - act.getStartTime()) / 3600);
			}else if (act.getType().equals("RideShareStay")){
				score =+ (- params.marginalUtilityOfPerforming_s - params.marginalUtilityOfWaiting_s) * ((act.getEndTime() - act.getStartTime()) / 3600);
				}

	}

	@Override
	public void handleLastActivity(Activity act) {
		if(act.getType().equals("RideSharePickup") || act.getType().equals("RideShareDropoff")){
			score =+ ( - params.marginalUtilityOfPerforming_s - params.marginalUtilityOfWaiting_s) * ((act.getEndTime() - act.getStartTime()) / 3600);
			}else if (act.getType().equals("RideShareStay")){
				score =+ (- params.marginalUtilityOfPerforming_s - params.marginalUtilityOfWaiting_s) * ((act.getEndTime() - act.getStartTime()) / 3600);
				}

	}

}
