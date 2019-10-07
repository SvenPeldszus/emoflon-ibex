package org.emoflon.ibex.tgg.operational.monitoring;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.tgg.operational.matches.IMatch;

import language.TGGRule;

public interface IVictoryDataProvider {
	
	/**
	 * Returns the TGGRule based on the rule name
	 * @param pRuleName the rulename
	 * @return the TGGRule
	 */
	public TGGRule getRule(String pRuleName);
	
	/**
	 * Returns all the matches
	 * @return all the matches
	 */
	public Set<IMatch> getMatches();
	
	/**
	 * Returns the matches based on the rulename of the match
	 * @param match the match that is processed
	 * @return the other matches based on the rulename of the match
	 */
	public Set<IMatch> getMatches(IMatch match);
	
	/**
	 * Returns the matches related to a rule
	 * @param pRuleName pRuleName the rule
	 * @return the matches related to the rule
	 */
	public Set<IMatch> getMatches(String pRuleName);
	
	/**
	 * Returns the neighboring nodes of some nodes 
	 * @param nodes the nodes we want to return their neighbors
	 * @param k the number of neighbors to return for every node counted by distance
	 * @return the neighboring nodes of each node
	 */
	public Collection<EObject> getMatchNeighbourhoods(Collection<EObject> nodes, int k);
	
	/**
	 * Returns the neighboring nodes of one node
	 * @param node node the node we want to calculate the neighbors
	 * @param k the number of neighbors to return for the node counted by distance
	 * @return the neighbors of that node
	 */
	public Collection<EObject> getMatchNeighbourhood(EObject node, int k);
	
	/**
	 * Process resources and saves them at selected paths
	 * @param pLocations  locations to save all four models
	 * @return newUri
	 * @throws IOException
	 */
	abstract public Set<URI> saveModels(String[] pLocations) throws IOException;
	
	/**
	 *  Returns all the Rules
	 * @return return all rules
	 */ 
	public Collection<TGGRule> getAllRules();

	public String[][] getDefaultSaveData();
}
