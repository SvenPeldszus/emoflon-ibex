package org.emoflon.ibex.tgg.compiler.patterns.filter_app_conds;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.patterns.PatternFactory;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.common.IbexPattern;
import org.emoflon.ibex.tgg.compiler.patterns.common.RulePartPattern;

import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;

public class FilterACPattern extends RulePartPattern {

	private TGGRuleNode entryPoint;
	private EReference edgeType;
	private EdgeDirection eDirection;
	private IbexPattern premise;

	public FilterACPattern(TGGRuleNode entryPoint, EReference edgeType, EdgeDirection eDirection, PatternFactory factory) {
		super(factory.getFlattenedVersionOfRule());
		this.entryPoint = entryPoint;
		this.edgeType = edgeType;
		this.eDirection = eDirection;

		premise = factory.createSearchEdgePattern(entryPoint, edgeType, eDirection);
		addDECAsBodyNode();
		
		// Create pattern network
		addTGGPositiveInvocation(premise);
	}
	
	private void addDECAsBodyNode() {
		getBodyNodes().add(EcoreUtil.copy(FilterACHelper.getDECNode(premise.getRule())));
	}

	@Override
	protected boolean injectivityIsAlreadyChecked(TGGRuleNode node1, TGGRuleNode node2) {
		return true;
	}

	@Override
	protected boolean isRelevantForBody(TGGRuleEdge e) {
		return false;
	}

	@Override
	protected boolean isRelevantForBody(TGGRuleNode n) {
		return FilterACHelper.isDECNode(n);
	}
	
	@Override
	public boolean isRelevantForSignature(TGGRuleElement e) {
		return premise.getSignatureElements().stream().filter(element -> element.getName().equals(e.getName())).count() != 0;
	}

	@Override
	protected String getPatternNameSuffix() {
		return getPatternNameSuffix(entryPoint, edgeType, eDirection);
	}
	
	public static String getPatternNameSuffix(TGGRuleNode entryPoint, EReference edgeType, EdgeDirection eDirection){
		return PatternSuffixes.SEP + entryPoint.getName() + "_" + edgeType.getName() + "_" + eDirection.toString().toLowerCase() + "_DEC_" + entryPoint.getDomainType().getName();
	}
}