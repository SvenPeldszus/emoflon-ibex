package org.emoflon.ibex.tgg.compiler.transformations.patterns.protocol;

import static org.emoflon.ibex.tgg.compiler.patterns.TGGPatternUtil.getProtocolPatternName;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.ContextPatternTransformation;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.common.OperationalPatternTransformation;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;

import IBeXLanguage.IBeXContextPattern;
import IBeXLanguage.IBeXEdge;
import language.TGGRule;

public class ProtocolPatternTransformation extends OperationalPatternTransformation {

	public ProtocolPatternTransformation(ContextPatternTransformation parent, IbexOptions options, TGGRule rule) {
		super(parent, options, rule);
	}

	@Override
	protected String getPatternName() {
		return getProtocolPatternName(rule.getName());
	}

	@Override
	protected void transformNodes(IBeXContextPattern ibexPattern) {
		parent.createAndConnectProtocolNode(rule, ibexPattern);
	}

	@Override
	protected void transformEdges(IBeXContextPattern ibexPattern) {
		List<IBeXEdge> corrLinks = ibexPattern.getLocalEdges()
			.stream()
			.filter(e -> e.getType().getName().equals("source") || e.getType().getName().equals("target"))
			.collect(Collectors.toList());
		corrLinks.forEach(EcoreUtil::delete);
	}

	@Override
	protected void transformNACs(IBeXContextPattern ibexPattern) {
		// Do nothing
	}

	@Override
	protected boolean patternIsEmpty() {
		// Always has at least one element
		return false;
	}

}
