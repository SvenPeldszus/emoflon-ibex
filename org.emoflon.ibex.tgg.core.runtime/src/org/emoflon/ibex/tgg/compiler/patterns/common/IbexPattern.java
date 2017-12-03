package org.emoflon.ibex.tgg.compiler.patterns.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.emoflon.ibex.tgg.compiler.patterns.IbexPatternOptimiser;

import language.TGGRule;
import language.TGGRuleCorr;
import language.TGGRuleEdge;
import language.TGGRuleNode;

public abstract class IbexPattern implements IPattern {

	protected TGGRule rule;
	
	protected IbexPatternOptimiser optimiser = new IbexPatternOptimiser();

	private Collection<PatternInvocation> positiveInvocations = new ArrayList<>();
	private Collection<PatternInvocation> negativeInvocations = new ArrayList<>();
	
	private Collection<TGGRuleNode> bodyNodes;
	private Collection<TGGRuleEdge> bodyEdges;

	protected IbexPattern() {
		
	}
	
	public IbexPattern(TGGRule rule) {
		this.rule = rule;
		initialize();
	}

	protected void initialize() {
		bodyNodes = calculateBodyNodes(rule.getNodes());
		bodyEdges = calculateBodyEdges(rule.getEdges());
		
		// optimisation needs to be done after bodyEdges have been set initially, since "retainAsOpposite()" accesses "getBodyEdges()"
		bodyEdges = bodyEdges.stream()
				   			 .filter(e -> optimiser.retainAsOpposite(e, this))
				   			 .collect(Collectors.toSet());
	}
	
	protected abstract boolean isRelevantForBody(TGGRuleEdge e);

	protected abstract boolean isRelevantForBody(TGGRuleNode n);

	public abstract boolean isRelevantForSignature(TGGRuleNode e);

	protected Collection<TGGRuleNode> getSignatureNodes(TGGRule rule) {
		return rule.getNodes().stream().filter(e -> isRelevantForSignature(e)).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Collection<TGGRuleEdge> calculateBodyEdges(Collection<TGGRuleEdge> signatureElements) {
		ArrayList<TGGRuleEdge> result = new ArrayList<>();
		signatureElements.stream().filter(e -> isRelevantForBody(e)).forEach(e -> result.add((TGGRuleEdge) e));
		return result;
	}

	protected Collection<TGGRuleNode> calculateBodyNodes(Collection<TGGRuleNode> signatureElements) {
		ArrayList<TGGRuleNode> result = new ArrayList<>();
		signatureElements.stream().filter(e -> isRelevantForBody(e)).forEach(e -> result.add((TGGRuleNode) e));
		return result;
	}

	public Collection<TGGRuleNode> getLocalNodes() {
		return bodyNodes;
	}

	public Collection<TGGRuleCorr> getLocalCorrNodes() {
		Collection<TGGRuleCorr> corrs = new HashSet<>();
		bodyNodes.stream().filter(n -> n instanceof TGGRuleCorr).forEach(n -> corrs.add((TGGRuleCorr) n));
		return corrs;
	}

	public Collection<TGGRuleNode> getBodySrcTrgNodes() {
		Collection<TGGRuleNode> srcTrgNodes = new HashSet<TGGRuleNode>(bodyNodes);
		srcTrgNodes.removeAll(getLocalCorrNodes());
		return srcTrgNodes;
	}

	public Collection<TGGRuleEdge> getLocalEdges() {
		return bodyEdges;
	}

	public String getName() {
		return rule.getName() + getPatternNameSuffix();
	}

	abstract protected String getPatternNameSuffix();

	public Collection<TGGRuleNode> getSignatureNodes() {
		return getSignatureNodes(getRule());
	}

	public Collection<PatternInvocation> getPositiveInvocations() {
		return positiveInvocations;
	}
	
	public void addTGGPositiveInvocation(IbexPattern pattern) {
		PatternInvocation pi = new PatternInvocation(this, pattern, getTGGVariableMapping(this, pattern));
		positiveInvocations.add(pi);
	}
	
	public void addPositiveInvocation(IbexPattern pattern, Map<TGGRuleNode, TGGRuleNode> mapping) {
		PatternInvocation pi = new PatternInvocation(this, pattern, mapping);
		positiveInvocations.add(pi);
	}

	public Collection<PatternInvocation> getNegativeInvocations() {
		return negativeInvocations;
	}
	
	public void addTGGNegativeInvocation(IPattern pattern) {
		PatternInvocation pi = new PatternInvocation(this, pattern, getTGGVariableMapping(this, pattern));
		negativeInvocations.add(pi);
	}
		
	public void addTGGNegativeInvocations(Collection<IPattern> patterns){
		for (IPattern n : patterns)
			addTGGNegativeInvocation(n);
	}
	
	public void addNegativeInvocation(IbexPattern pattern, Map<TGGRuleNode, TGGRuleNode> mapping) {
		PatternInvocation pi = new PatternInvocation(this, pattern, mapping);
		negativeInvocations.add(pi);
	}
	
	private Map<TGGRuleNode, TGGRuleNode> getTGGVariableMapping(IPattern rootPattern, IPattern invocationpattern) {
		Map<TGGRuleNode, TGGRuleNode> mapping = new HashMap<>();
		Set<TGGRuleNode> rootElements = Stream.concat(rootPattern.getSignatureNodes().stream(), rootPattern.getLocalNodes().stream()).collect(Collectors.toSet());
		Collection<TGGRuleNode> invocationElements = invocationpattern.getSignatureNodes();

		// map invocation elements to root elements based on their name
		invocationElements.stream().forEach(iEl -> mapping.put(rootElements.stream().filter(rEl -> rEl.getName().equals(iEl.getName()))
		                                                                            .findFirst()
		                                                                            .orElseThrow(() -> new IllegalStateException("The node " + iEl.getName() + " is missing in the Pattern [" + rootElements + "]")), 
		                                                       iEl));
		return mapping;
	}
	
	public boolean ignored() {
		return false;
	}

	public TGGRule getRule() {
		return rule;
	}

}
