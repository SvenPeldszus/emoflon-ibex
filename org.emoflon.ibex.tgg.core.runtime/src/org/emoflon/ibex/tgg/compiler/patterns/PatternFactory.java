package org.emoflon.ibex.tgg.compiler.patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.TGGCompiler;
import org.emoflon.ibex.tgg.compiler.patterns.common.MarkedPattern;
import org.emoflon.ibex.tgg.compiler.patterns.protocol.ConsistencyPattern;
import org.emoflon.ibex.tgg.compiler.patterns.protocol.nacs.SrcProtocolNACsPattern;
import org.emoflon.ibex.tgg.compiler.patterns.protocol.nacs.TrgProtocolNACsPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.BWDPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.CCPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.FWDPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.MODELGENPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.RulePartPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.WholeRulePattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.refinement.CCNoNACsPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.refinement.CCPatternWithRefinements;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.refinement.MODELGENNoNACsPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.refinement.MODELGENPatternWithRefinements;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.ConstraintPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.CorrContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.SrcContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.SrcPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.SrcProtocolAndDECPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.TrgContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.TrgPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.TrgProtocolAndDECPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.DEC.DECPattern;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.DEC.DECStrategy;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.DEC.EdgeDirection;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.DEC.NoDECsPatterns;
import org.emoflon.ibex.tgg.compiler.patterns.rulepart.support.DEC.SearchEdgePattern;

import language.BindingType;
import language.DomainType;
import language.TGGRule;
import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;

public class PatternFactory {
	private TGGRule rule;
	private Map<Object, IbexPattern> patterns;
	private TGGCompiler compiler;
	private static final List<MarkedPattern> markedPatterns = createMarkedPatterns();
	
	public static final DECStrategy strategy = DECStrategy.NONE;

	public PatternFactory(TGGRule rule, TGGCompiler compiler) {
		this.rule = rule;
		this.compiler = compiler;
		patterns = new LinkedHashMap<>();
	}

	public Collection<IbexPattern> getPatterns() {
		return Collections.unmodifiableCollection(patterns.values());
	}
	
	private static List<MarkedPattern> createMarkedPatterns() {
		List<MarkedPattern> markedPatterns = new ArrayList<>();
		
		MarkedPattern signProtocolSrcMarkedPattern = new MarkedPattern(DomainType.SRC, false);
		MarkedPattern signProtocolTrgMarkedPattern = new MarkedPattern(DomainType.TRG, false);
		MarkedPattern localProtocolSrcMarkedPattern = new MarkedPattern(signProtocolSrcMarkedPattern, DomainType.SRC, true);
		MarkedPattern localProtocolTrgMarkedPattern = new MarkedPattern(signProtocolTrgMarkedPattern, DomainType.TRG, true);
		
		localProtocolSrcMarkedPattern.addTGGPositiveInvocation(signProtocolSrcMarkedPattern);
		localProtocolTrgMarkedPattern.addTGGPositiveInvocation(signProtocolTrgMarkedPattern);
		
		markedPatterns.add(localProtocolSrcMarkedPattern);
		markedPatterns.add(localProtocolTrgMarkedPattern);
		markedPatterns.add(signProtocolSrcMarkedPattern);
		markedPatterns.add(signProtocolTrgMarkedPattern);
		
		return markedPatterns;
	}
	
	/**
	 * This method computes constraint patterns for a given pattern to deal with all 0..n multiplicities.
	 * For every created edge in the pattern that has a 0..n multiplicity, a pattern is created which ensures that the multiplicity 
	 * is not violated by applying the rule.  These patterns are meant to be negatively invoked.
	 * 
	 * @param pattern The pattern used for the analysis.
	 * @return All patterns that should be negatively invoked to prevent violations of all 0..n multiplicities.
	 */    
	public Collection<IbexPattern> createPatternsForMultiplicityConstraints(RulePartPattern pattern) {
		TGGRule flattenedRule = compiler.getFlattenedVersionOfRule(rule);
		
        // collect edges that need a multiplicity NAC
        Collection<TGGRuleEdge> relevantEdges = flattenedRule.getEdges().stream()
        													   .filter(e -> e.getType().getUpperBound() > 0
        															   && e.getBindingType() == BindingType.CREATE
        															   && e.getSrcNode().getBindingType() == BindingType.CONTEXT)
        													   .collect(Collectors.toList());

        HashMap<TGGRuleNode, HashSet<EReference>> sourceToProcessedEdgeTypes = new HashMap<TGGRuleNode, HashSet<EReference>>();
        Collection<IbexPattern> negativePatterns = new ArrayList<>();
        for (TGGRuleEdge e : relevantEdges) {
			TGGRuleNode src = e.getSrcNode();
			
			// skip this edge if another edge of same type and with same source has already been processed
			Collection<EReference> processedEdgeTypes = sourceToProcessedEdgeTypes.get(src);
			if (processedEdgeTypes != null && processedEdgeTypes.contains(e.getType())) {
				continue;
			}
			
			// add edge to processed edges for its type and source node
			if (sourceToProcessedEdgeTypes.get(src) == null) {
				sourceToProcessedEdgeTypes.put(src, new HashSet<EReference>());
			}
			sourceToProcessedEdgeTypes.get(src).add(e.getType());
			
			// calculate number of create-edges with the same type coming from this source node
			long similarEdgesCount = flattenedRule.getEdges().stream()
															 .filter(edge -> edge.getType() == e.getType()
															 				&& edge.getSrcNode() == src
															 				&& edge.getBindingType() == BindingType.CREATE)
															 .count();

			Collection<TGGRuleElement> signatureElements = new ArrayList<TGGRuleElement>();
			Collection<TGGRuleElement> bodyElements = new ArrayList<TGGRuleElement>();

			// create/add elements to the pattern
			signatureElements.add(src);

			for (int i = 1; i <= e.getType().getUpperBound()+1-similarEdgesCount; i++) {
				TGGRuleNode trg = EcoreUtil.copy(e.getTrgNode());
				TGGRuleEdge edge = EcoreUtil.copy(e);
				
				trg.setName(trg.getName()+i);
				edge.setSrcNode(src);
				edge.setTrgNode(trg);
				
				bodyElements.add(trg);
				bodyElements.add(edge);
			}

			// create pattern and invocation
			String patternName = e.getSrcNode().getName()
					+ "_"
					+ e.getType().getName()
					+ "Edge"
					+ "_Multiplicity";
			
			negativePatterns.add(createPattern(patternName, () -> new ConstraintPattern(rule, signatureElements, bodyElements, patternName)));
        }
        
        return negativePatterns;
	}

	/**
	 * This method computes constraint patterns to deal with containment references.
	 * For every created containment edge in the given pattern with a context node as target, a pattern
	 * is computed which ensures that the target node is not already contained in another reference.
	 * These patterns are meant to be negatively invoked.
	 * 
	 * @param pattern The pattern for which the negative patterns are computed.
	 * @return All patterns that should be negatively invoked to prevent violations of containment.
	 */
	public Collection<IbexPattern> createPatternsForContainmentReferenceConstraints(RulePartPattern pattern) {
		TGGRule flattenedRule = compiler.getFlattenedVersionOfRule(rule);

        // collect edges that need a multiplicity NAC
        Collection<TGGRuleEdge> relevantEdges = flattenedRule.getEdges().stream()
        													   .filter(e -> e.getType().isContainment()
        															   && e.getBindingType() == BindingType.CREATE
        															   && e.getTrgNode().getBindingType() == BindingType.CONTEXT)
        													   .collect(Collectors.toList());

        Collection<IbexPattern> negativePatterns = new ArrayList<>();
        for (TGGRuleEdge e : relevantEdges) {
			TGGRuleNode trg = e.getTrgNode();
			
			Collection<TGGRuleElement> signatureElements = new ArrayList<TGGRuleElement>();
			Collection<TGGRuleElement> bodyElements = new ArrayList<TGGRuleElement>();

			// create/add elements to the pattern
			TGGRuleNode src = EcoreUtil.copy(e.getSrcNode());
			TGGRuleEdge edge = EcoreUtil.copy(e);
			
			edge.setSrcNode(src);
			edge.setTrgNode(trg);
			
			bodyElements.add(src);
			bodyElements.add(edge);
			signatureElements.add(trg);

			// create pattern and invocation
			String patternName = e.getType().getName()
					+ "Edge_"
					+ e.getTrgNode().getName()
					+ "_Containment";
			
			negativePatterns.add(createPattern(patternName, () -> new ConstraintPattern(rule, signatureElements, bodyElements, patternName)));
        }
        
        return negativePatterns;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends IbexPattern> T createPattern(Object key, Supplier<T> creator){
		T p = (T) patterns.computeIfAbsent(key, (k) -> creator.get());
		
		if(p == null)
			throw new IllegalStateException("Created pattern is null!");
		
		return p;
	}

	public IbexPattern createMODELGENPattern() {
		return createPattern(MODELGENPattern.class, () -> new MODELGENPattern(rule, this));
	}
	
	public IbexPattern createSrcContextPattern() {
		return createPattern(SrcContextPattern.class, () -> new SrcContextPattern(rule));
	}

	public IbexPattern createCorrContextPattern() {
		return createPattern(CorrContextPattern.class, () -> new CorrContextPattern(rule));
	}

	public IbexPattern createTrgContextPattern() {
		return createPattern(TrgContextPattern.class, () -> new TrgContextPattern(rule));
	}

	public IbexPattern createCCPattern() {
		return createPattern(CCPattern.class, () -> new CCPattern(rule, this));
	}

	public IbexPattern createSrcPattern() {
		return createPattern(SrcPattern.class, () -> new SrcPattern(rule, this));
	}

	public IbexPattern createTrgPattern() {
		return createPattern(TrgPattern.class, () -> new TrgPattern(rule, this));
	}

	public IbexPattern createFWDPattern() {
		return createPattern(FWDPattern.class, () -> new FWDPattern(rule, this));
	}

	public IbexPattern createBWDPattern() {
		return createPattern(BWDPattern.class, () -> new BWDPattern(rule, this));
	}

	public IbexPattern createConsistencyPattern() {
		return createPattern(ConsistencyPattern.class, () -> new ConsistencyPattern(rule, this));
	}

	public IbexPattern createSrcProtocolAndDECPattern() {
		return createPattern(SrcProtocolAndDECPattern.class, () -> new SrcProtocolAndDECPattern(rule, this));
	}

	public IbexPattern createTrgProtocolAndDECPattern() {
		return createPattern(TrgProtocolAndDECPattern.class, () -> new TrgProtocolAndDECPattern(rule, this, markedPatterns));
	}

	public IbexPattern createWholeRulePattern() {
		return createPattern(WholeRulePattern.class, () -> new WholeRulePattern(rule, this));
	}

	public IbexPattern createSrcProtocolNACsPattern() {
		return createPattern(SrcProtocolNACsPattern.class, () -> new SrcProtocolNACsPattern(rule, markedPatterns, this));
	}

	public IbexPattern createTrgProtocolNACsPattern() {
		return createPattern(TrgProtocolNACsPattern.class, () -> new TrgProtocolNACsPattern(rule, markedPatterns, this));
	}

	public IbexPattern createMODELGENPatternWithRefinement() {
		return createPattern(MODELGENPatternWithRefinements.class, () -> new MODELGENPatternWithRefinements(rule, compiler.getFlattenedVersionOfRule(rule), this));
	}

	public IbexPattern createMODELGENNoNACsPattern() {
		return createPattern(MODELGENNoNACsPattern.class, () -> new MODELGENNoNACsPattern(rule, compiler.getFlattenedVersionOfRule(rule), this));
	}

	public IbexPattern createCCPatternWithRefinement() {
		return createPattern(CCPatternWithRefinements.class, () -> new CCPatternWithRefinements(rule, compiler.getFlattenedVersionOfRule(rule), this));
	}
	
	public IbexPattern createCCNoNACsPattern() {
		return createPattern(CCNoNACsPattern.class, () -> new CCNoNACsPattern(rule, compiler.getFlattenedVersionOfRule(rule), this));
	}

	public PatternFactory getFactory(TGGRule superRule) {
		return compiler.getFactory(superRule);
	}

	public IbexPattern createNoDECsPatterns(DomainType domain) {
		return createPattern(rule.getName() + NoDECsPatterns.getPatternNameSuffix(domain), () -> new NoDECsPatterns(rule, domain, this));
	}

	public IbexPattern createDECPattern(TGGRuleNode entryPoint, EReference edgeType, EdgeDirection eDirection, Collection<TGGRule> savingRules) {
		return createPattern(rule.getName() + DECPattern.getPatternNameSuffix(entryPoint, edgeType, eDirection), () -> new DECPattern(rule, entryPoint, edgeType, eDirection, savingRules, this));
	}

	public IbexPattern createSearchEdgePattern(TGGRuleNode entryPoint, EReference edgeType, EdgeDirection eDirection) {
		return createPattern(rule.getName() + SearchEdgePattern.getPatternNameSuffix(entryPoint, edgeType, eDirection), () -> new SearchEdgePattern(rule, entryPoint, edgeType, eDirection));
	}
	
	public static List<MarkedPattern> getMarkedPatterns() {
		return markedPatterns;
	}
	
	public static IbexPattern getMarkedPattern(DomainType domain, boolean local) {
		return markedPatterns.stream()
							 .filter(p -> p.getDomain().equals(domain) && (p.isLocal() == local))
							 .findFirst()
							 .get();
	}
}