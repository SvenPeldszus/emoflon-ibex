package org.emoflon.ibex.tgg.operational.strategies.gen;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.TGGPatternUtil;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.matches.IMatch;
import org.emoflon.ibex.tgg.operational.matches.SimpleMatch;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;

import language.BindingType;
import language.TGGComplementRule;
import language.TGGRule;
import language.TGGRuleEdge;
import runtime.TGGRuleApplication;

/**
 * Different than other OperationalStrategy subtypes, MODELGEN (i) additionally
 * has a stop criterion (see MODELGENStopCriterion) (ii) does not remove
 * processed matches from its MatchContainer in processOperationalRuleMatches()
 * (iii) gets matches randomly from MatchContainer (iv) does not create a
 * protocol for scalability
 * 
 * @author leblebici
 *
 */
public abstract class MODELGEN extends OperationalStrategy {

	protected MODELGENStopCriterion stopCriterion;

	public MODELGEN(IbexOptions options) throws IOException {
		super(options);
	}

	public void setStopCriterion(MODELGENStopCriterion stop) {
		this.stopCriterion = stop;
	}

	@Override
	public void saveModels() throws IOException {
		s.save(null);
		t.save(null);
		c.save(null);
		p.save(null);
	}

	@Override
	public void loadModels() throws IOException {
		s = createResource(options.projectPath() + "/instances/src.xmi");
		t = createResource(options.projectPath() + "/instances/trg.xmi");
		c = createResource(options.projectPath() + "/instances/corr.xmi");
		p = createResource(options.projectPath() + "/instances/protocol.xmi");

		EcoreUtil.resolveAll(rs);
	}

	/**
	 * If we get notified about a new match that is the NAC of an axiom (i.e. a
	 * match for an GENAxiomNacPattern) we need to remove the always available empty
	 * axiom match. Otherwise we can add the match as usual.
	 */
	@Override
	public void addMatch(org.emoflon.ibex.common.operational.IMatch match) {
		if (TGGPatternUtil.isGENAxiomNacPattern(match.getPatternName())) {
			this.deleteAxiomMatchesForFoundNACs(match);
		} else {
			super.addMatch(match);
		}
	}

	protected abstract void registerUserMetamodels() throws IOException;

	@Override
	public boolean isPatternRelevantForCompiler(String patternName) {
		return patternName.endsWith(PatternSuffixes.GEN) || patternName.endsWith(PatternSuffixes.GEN_AXIOM_NAC);
	}

	/**
	 * differently from the super class implementation, MODELGEN (i) does not remove
	 * successful matches (but uses them repeatedly) (ii) updates the state of its
	 * stop criterion
	 */
	@Override
	protected boolean processOneOperationalRuleMatch() {
		if (stopCriterion.dont() || operationalMatchContainer.isEmpty())
			return false;

		IMatch match = chooseOneMatch();
		String ruleName = operationalMatchContainer.getRuleName(match);

		if (stopCriterion.dont(ruleName))
			removeOperationalRuleMatch(match);
		else {
			Optional<IMatch> comatch = processOperationalRuleMatch(ruleName, match);
			comatch.ifPresent(cm -> {
				updateStopCriterion(ruleName);
				if (isKernelMatch(ruleName))
					processComplementRuleMatches(cm);
			});

			if (!comatch.isPresent()) {
				// FIXME[Anjorin]: Dangerous to just discard in case context edges are pending
				removeOperationalRuleMatch(match);
				logger.debug("Unable to apply: " + match);
			}
		}

		return true;
	}

	/**
	 * We have found a match for a NAC of an axiom. This means this axiom is no
	 * longer applicable and thus needs to be removed from the set of matches
	 * 
	 * @param match the match of a NAC for an Axiom
	 */
	private void deleteAxiomMatchesForFoundNACs(org.emoflon.ibex.common.operational.IMatch match) {
		Set<IMatch> matchesToRemove = new HashSet<>();
		String axiomName = TGGPatternUtil.getGENBlackPatternName(TGGPatternUtil.extractGENAxiomNacName(match.getPatternName()));
		operationalMatchContainer.getMatches().stream().filter(m -> m.getPatternName().equals(axiomName)).forEach(m -> {
			matchesToRemove.add(m);
		});
		matchesToRemove.stream().forEach(m -> removeMatch(m));
	}

	private void processComplementRuleMatches(IMatch comatch) {
		blackInterpreter.updateMatches();
		Set<IMatch> complementRuleMatches = findAllComplementRuleMatches();

		if (!complementRuleMatches.isEmpty()) {
			HashMap<String, Integer> complementRulesBounds = callUpdatePolicy(complementRuleMatches);

			while (!complementRuleMatches.isEmpty()) {
				IMatch match = complementRuleMatches.iterator().next();
				processComplementRuleMatch(match, complementRulesBounds);
				complementRuleMatches.remove(match);
				removeOperationalRuleMatch(match);
			}
		}

		// Close the kernel, so other complement rules cannot find this match anymore
		TGGRuleApplication application = (TGGRuleApplication) comatch
				.get(TGGPatternUtil.getProtocolNodeName(PatternSuffixes.removeSuffix(comatch.getPatternName())));
		application.setAmalgamated(true);
	}

	private HashMap<String, Integer> callUpdatePolicy(Set<IMatch> complementRuleMatches) {
		Set<String> uniqueRulesNames = complementRuleMatches.stream()
				.map(m -> PatternSuffixes.removeSuffix(m.getPatternName())).distinct().collect(Collectors.toSet());
		HashMap<String, Integer> complementRulesBounds = updatePolicy.getNumberOfApplications(uniqueRulesNames);

		checkComplianceWithSchema(complementRulesBounds);

		return complementRulesBounds;
	}

	private void processComplementRuleMatch(IMatch match, HashMap<String, Integer> complementRulesBounds) {
		String ruleName = operationalMatchContainer.getRuleName(match);
		TGGComplementRule rule = (TGGComplementRule) getRule(ruleName).get();

		if (rule.isBounded()) {
			processOperationalRuleMatch(ruleName, match);
		} else {
			IntStream.range(0, complementRulesBounds.get(ruleName))
					.forEach(i -> processOperationalRuleMatch(ruleName, match));
		}
	}

	private Set<IMatch> findAllComplementRuleMatches() {
		Set<IMatch> allComplementRuleMatches = operationalMatchContainer.getMatches().stream()
				.filter(m -> getComplementRulesNames().contains(PatternSuffixes.removeSuffix(m.getPatternName())))
				.collect(Collectors.toSet());

		return allComplementRuleMatches;
	}

	private void updateStopCriterion(String ruleName) {
		stopCriterion.update(ruleName,
				getGreenFactory(ruleName).getGreenSrcNodesInRule().size()
						+ getGreenFactory(ruleName).getGreenSrcEdgesInRule().size(),
				getGreenFactory(ruleName).getGreenTrgNodesInRule().size()
						+ getGreenFactory(ruleName).getGreenTrgEdgesInRule().size());
	}

	@Override
	public void run() throws IOException {
		collectMatchesForAxioms();
		do
			blackInterpreter.updateMatches();
		while (processOneOperationalRuleMatch());
	}

	/**
	 * Axioms are always applicable but the PatternMatcher does not return empty
	 * matches enabling the application of the axioms. Therefore we add the empty
	 * matches ourself. The match of an axiom will be removed once a match of a NAC
	 * of the axiom is found.
	 */
	private void collectMatchesForAxioms() {
		options.getFlattenedConcreteTGGRules().stream().filter(r -> getGreenFactory(r.getName()).isAxiom())
				.forEach(r -> {
					addOperationalRuleMatch(new SimpleMatch(TGGPatternUtil.getGENBlackPatternName(r.getName())));
				});
	}

	// FIXME[Anjorin]: After latest updates to the green interpreter, I believe
	// these checks can be removed here (they are performed by the interpreter
	// anyway).
	private void checkComplianceWithSchema(HashMap<String, Integer> complementRulesBounds) {
		HashMap<EReference, Integer> edgesToBeCreated = new HashMap<EReference, Integer>();

		complementRulesBounds.keySet().stream().forEach(name -> {
			if ((getComplementRule(name).get()).isBounded()) {
				processBoundedRuleLimits(name, edgesToBeCreated);
			} else {
				getRelevantEdges(getComplementRule(name).get()).stream().forEach(e -> {
					if (!edgesToBeCreated.containsKey(e.getType())) {
						edgesToBeCreated.put(e.getType(), complementRulesBounds.get(name));
					} else {
						edgesToBeCreated.put(e.getType(),
								edgesToBeCreated.get(e.getType()) + complementRulesBounds.get(name));
					}
				});
			}
		});
		edgesToBeCreated.keySet().stream()
				.filter(e -> e.getUpperBound() != -1 && edgesToBeCreated.get(e) > e.getUpperBound()).findAny()
				.ifPresent(e -> {
					throw new IllegalArgumentException("Cardinalities for " + e.getName() + " are violated");
				});
	}

	private void processBoundedRuleLimits(String name, HashMap<EReference, Integer> edgesToBeCreated) {
		// find all matches for bounded rule collected by operational match container
		int number = (int) findAllComplementRuleMatches().stream().filter(m -> m.getPatternName().contains(name))
				.count();
		getRelevantEdges(getComplementRule(name).get()).stream().forEach(e -> {
			edgesToBeCreated.put(e.getType(), number);
		});
	}

	private Set<TGGRuleEdge> getRelevantEdges(TGGRule rule) {
		Set<TGGRuleEdge> relevantEdges = rule.getEdges().stream().filter(
				e -> e.getBindingType() == BindingType.CREATE && e.getSrcNode().getBindingType() == BindingType.CONTEXT)
				.collect(Collectors.toSet());
		return relevantEdges;
	}

}
