package rideSharing;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.functions.ActivityTypeOpeningIntervalCalculator;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
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
			double performance_score = calcRideShareScore(act.getStartTime(), act.getEndTime(), act);
			score = score + params.marginalUtilityOfWaiting_s * ((act.getEndTime() - act.getStartTime()) / 3600) - performance_score;
			}else if (act.getType().equals("RideShareStay")){
				double performance_score = calcRideShareScore(act.getStartTime(), act.getEndTime(), act);
				score = score + params.marginalUtilityOfWaiting_s * ((act.getEndTime() - act.getStartTime()) / 3600) - performance_score;
				}

	}
	private double calcRideShareScore(double arrivalTime, double departureTime, Activity act) {
		// TODO Auto-generated method stub
		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double tmpScore = 0.0;

		if (actParams.isScoreAtAll()) {
			/* Calculate the times the agent actually performs the
			 * activity.  The facility must be open for the agent to
			 * perform the activity.  If it's closed, but the agent is
			 * there, the agent must wait instead of performing the
			 * activity (until it opens).
			 *
			 *                                             Interval during which
			 * Relationship between times:                 activity is performed:
			 *
			 *      O________C A~~D  ( 0 <= C <= A <= D )   D...D (not performed)
			 * A~~D O________C       ( A <= D <= O <= C )   D...D (not performed)
			 *      O__A+++++C~~D    ( O <= A <= C <= D )   A...C
			 *      O__A++D__C       ( O <= A <= D <= C )   A...D
			 *   A~~O++++++++C~~D    ( A <= O <= C <= D )   O...C
			 *   A~~O+++++D__C       ( A <= O <= D <= C )   O...D
			 *
			 * Legend:
			 *  A = arrivalTime    (when agent gets to the facility)
			 *  D = departureTime  (when agent leaves the facility)
			 *  O = openingTime    (when facility opens)
			 *  C = closingTime    (when facility closes)
			 *  + = agent performs activity
			 *  ~ = agent waits (agent at facility, but not performing activity)
			 *  _ = facility open, but agent not there
			 *
			 * assume O <= C
			 * assume A <= D
			 */

			double activityStart = arrivalTime;
			double activityEnd = departureTime;

			double duration = activityEnd - activityStart;

			// disutility if too late

			double latestStartTime = actParams.getLatestStartTime();
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
			double typicalDuration = actParams.getTypicalDuration();

			if ( this.params.usingOldScoringBelowZeroUtilityDuration ) {
				if (duration > 0) {
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
					double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
					tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
				} else {
					tmpScore += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
				}
			} else {
				if ( duration >= 3600.*actParams.getZeroUtilityDuration_h() ) {
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
					// also removing the "wait" alternative scoring.
					tmpScore += utilPerf ;
				} else {
//					if ( wrnCnt < 1 ) {
//						wrnCnt++ ;
//						log.warn("encountering duration < zeroUtilityDuration; the logic for this was changed around mid-nov 2013.") ;
//						log.warn( "your final score thus will be different from earlier runs; set usingOldScoringBelowZeroUtilityDuration to true if you "
//								+ "absolutely need the old version.  See https://matsim.atlassian.net/browse/MATSIM-191." );
//						log.warn( Gbl.ONLYONCE ) ;
//					}
					
					// below zeroUtilityDuration, we linearly extend the slope ...:
					double slopeAtZeroUtility = this.params.marginalUtilityOfPerforming_s * typicalDuration / ( 3600.*actParams.getZeroUtilityDuration_h() ) ;
					if ( slopeAtZeroUtility < 0. ) {
						// (beta_perf might be = 0)
						System.err.println("beta_perf: " + this.params.marginalUtilityOfPerforming_s);
						System.err.println("typicalDuration: " + typicalDuration );
						System.err.println( "zero utl duration: " + actParams.getZeroUtilityDuration_h() );
						throw new RuntimeException( "slope at zero utility < 0.; this should not happen ...");
					}
					double durationUnderrun = actParams.getZeroUtilityDuration_h()*3600. - duration ;
					if ( durationUnderrun < 0. ) {
						throw new RuntimeException( "durationUnderrun < 0; this should not happen ...") ;
					}
					tmpScore -= slopeAtZeroUtility * durationUnderrun ;
				}
				
			}

			// disutility if stopping too early
			double earliestEndTime = actParams.getEarliestEndTime();
			if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
			}

			// disutility if duration was too short
			double minimalDuration = actParams.getMinimalDuration();
			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
			}
		}
		if(Double.isInfinite(tmpScore)){
			System.out.println("xxx");
		}
		return tmpScore;
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
