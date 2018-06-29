package org.emoflon.ibex.tgg.operational.repair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.sync.ConsistencyPattern;
import org.emoflon.ibex.tgg.operational.matches.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import runtime.TGGRuleApplication;

public class RepairStrategyController {
	
	private OperationalStrategy oStrategy;

	// a collection of matches which have been broken
	private Map<TGGRuleApplication, IMatch> pendingRepairs;
	
	// a collection of matches that may have been repaired
	private Map<TGGRuleApplication, IMatch> brokenRuleApplications;
	private Map<TGGRuleApplication, AbstractRepairStrategy> repairCandidateToStrategy;
	
	// a collection of repair strategies to repair broken matches into valid ones
	private Collection<AbstractRepairStrategy> strategies;
	
	public RepairStrategyController(OperationalStrategy operationalStrategy) {
		oStrategy = operationalStrategy;
		pendingRepairs = new Object2ObjectOpenHashMap<TGGRuleApplication, IMatch>();
		brokenRuleApplications = new Object2ObjectOpenHashMap<TGGRuleApplication, IMatch>();
		repairCandidateToStrategy = new Object2ObjectOpenHashMap<TGGRuleApplication, AbstractRepairStrategy>();
		strategies = new ArrayList<>();
	}

	public void registerStrategy(AbstractRepairStrategy strategy) {
		strategies.add(strategy);
	}
	
	public void clearStrategies() {
		strategies.clear();
	}
	
	public Map<TGGRuleApplication, IMatch> getBrokenRuleApplications() {
		return brokenRuleApplications;
	}
	
	/**
	 * This methods registers brokenRuleApplications which are processed by the registered repair strategies.
	 * After calling this method, the repairsSuccessfull method has to be called with newly registered matches
	 * to check whether the repairings were successful or not
	 * @param brokenRuleApplications
	 */
	public void repairMatches(Map<TGGRuleApplication, IMatch> brokenRuleApplications) {
		this.brokenRuleApplications = brokenRuleApplications;
		
		if(isSyncRunning())
			invokeStrategies();
	}
	
	/**
	 *  check if the repair was successful by searching for a new match for a TGGRuleApplication which was repaired by a strategy.
	 *  if no match can be found for a pending TGGRuleApplication, the previous repair step is assumed invalid and thus has to be revoked.
	 *  These elements can be accessed via getBrokenRuleApplications()
	 * @param operationalMatches
	 */
	public void repairsSuccessful(Map<TGGRuleApplication, IMatch> operationalMatches) {
		Iterator<TGGRuleApplication> it = pendingRepairs.keySet().iterator();
		while(it.hasNext()) {
			TGGRuleApplication ra = it.next();
			IMatch match = pendingRepairs.get(ra);
			// check if a new match can be found and if it is considered a valid one. if so revoke the repair and add it back to broken matches
			if(!operationalMatches.containsKey(ra) ||
					!repairCandidateToStrategy.get(ra).checkIfRepairWasSucessful(ra, match, operationalMatches.get(ra))) {
				repairCandidateToStrategy.get(ra).revokeRepair(ra);
				brokenRuleApplications.put(ra, match);
			}
			else {
				oStrategy.addOperationalRuleMatch(PatternSuffixes.removeSuffix(match.getPatternName()), match);
			}
			
			// remove the candidate from both repair maps. Whether the repair was successful or not, no further repairs will take place here.
//			repairCandidates.remove(ra);
//			repairCandidateToStrategy.remove(ra);
		}
		repairCandidateToStrategy.clear();
		pendingRepairs.clear();
	}
	
	public boolean repairCandidatesPending() {
		return !strategies.isEmpty() && !pendingRepairs.isEmpty();
	}
	
	private void invokeStrategies() {
		for(TGGRuleApplication ra : brokenRuleApplications.keySet()) {
			Iterator<AbstractRepairStrategy> it = strategies.iterator();
			while(it.hasNext()) {
				IMatch repairCandidate = brokenRuleApplications.get(ra);
				AbstractRepairStrategy strategy = it.next();
				if(strategy.isCandidate(ra, repairCandidate) && strategy.repair(ra, repairCandidate)) {
					pendingRepairs.put(ra, repairCandidate);
					repairCandidateToStrategy.put(ra, strategy);
					brokenRuleApplications.remove(ra);
					break;
				}
			}
		}
	}
	
	// TODO lfritsche: duplicate from OperationalStrategy
	public TGGRuleApplication getRuleApplicationNode(IMatch match) {
		return (TGGRuleApplication) match
				.get(ConsistencyPattern.getProtocolNodeName(PatternSuffixes.removeSuffix(match.getPatternName())));
	}
	
	private boolean isSyncRunning() {
		return oStrategy instanceof SYNC;
	}
}