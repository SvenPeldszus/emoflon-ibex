package org.emoflon.ibex.tgg.operational.patterns;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.patterns.TGGPatternUtil;
import org.emoflon.ibex.tgg.core.util.TGGModelUtils;
import org.emoflon.ibex.tgg.operational.csp.IRuntimeTGGAttrConstrContainer;
import org.emoflon.ibex.tgg.operational.csp.RuntimeTGGAttributeConstraintContainer;
import org.emoflon.ibex.tgg.operational.csp.sorting.SearchPlanAction;
import org.emoflon.ibex.tgg.operational.matches.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;

import language.BindingType;
import language.DomainType;
import language.TGGAttributeConstraint;
import language.TGGParamValue;
import language.TGGRuleEdge;
import language.TGGRuleNode;

public abstract class IbexGreenPattern implements IGreenPattern {
	protected IGreenPatternFactory factory;
	protected OperationalStrategy strategy;
	
	public IbexGreenPattern(IGreenPatternFactory factory) {
		this.factory = factory;
		this.strategy = factory.getStrategy();
	}
	
	@Override
	public IRuntimeTGGAttrConstrContainer getAttributeConstraintContainer(IMatch match) {
		try {			
			return new RuntimeTGGAttributeConstraintContainer(
					factory.getAttributeCSPVariables(), 
					 sortConstraints(factory.getAttributeCSPVariables(), factory.getAttributeConstraints()),
					match,
					factory.getOptions().constraintProvider());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to sort attribute constraints for " + match.getPatternName() + ", " + e.getMessage(), e);
		}
	}
	
	protected List<TGGAttributeConstraint> sortConstraints(List<TGGParamValue> variables, List<TGGAttributeConstraint> constraints) {
		SearchPlanAction spa = new SearchPlanAction(variables, constraints, false, getSrcTrgNodesCreatedByPattern());
		return spa.sortConstraints();
	}

	@Override
	public Collection<TGGRuleEdge> getEdgesMarkedByPattern() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TGGRuleEdge> getMarkedContextEdges() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TGGRuleNode> getNodesMarkedByPattern() {
		return Collections.emptyList();
	}
	
	@Override
	public boolean isToBeIgnored(IMatch match) {
		return false;
	}
	
	@Override
	public void createMarkers(String ruleName, IMatch match) {
		EPackage corrPackage = strategy.getOptions().getCorrMetamodel();
		EClass type = (EClass) corrPackage.getEClassifier(TGGModelUtils.getMarkerTypeName(ruleName));
		
		EObject ra = EcoreUtil.create(type);
		strategy.getProtocolResource().getContents().add(ra);
		
	
		for (TGGRuleNode n : factory.getGreenSrcNodesInRule()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.CREATE, DomainType.SRC, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		for (TGGRuleNode n : factory.getBlackSrcNodesInRule()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.CONTEXT, DomainType.SRC, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}

		for (TGGRuleNode n : factory.getGreenTrgNodesInRule()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.CREATE, DomainType.TRG, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		for (TGGRuleNode n : factory.getBlackTrgNodesInRule()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.CONTEXT, DomainType.TRG, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}

		for (TGGRuleNode n : factory.getGreenCorrNodesInRule()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.CREATE, DomainType.CORR, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		for (TGGRuleNode n : factory.getBlackCorrNodesInRule()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.CONTEXT, DomainType.CORR, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		//usability-team
		//get
		
		for (TGGRuleNode n : factory.getNegativeSrcNodesInNac()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.NEGATIVE, DomainType.SRC, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		for (TGGRuleNode n : factory.getNegativeTrgNodesInNac()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.NEGATIVE, DomainType.TRG, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		for (TGGRuleNode n : factory.getNegativeCorrNodesInNac()) {
			String refName = TGGModelUtils.getMarkerRefName(BindingType.NEGATIVE, DomainType.CORR, n.getName());
			EReference ref = (EReference) type.getEStructuralFeature(refName);			
			ra.eSet(ref, (EObject) match.get(n.getName()));			
		}
		
		match.put(TGGPatternUtil.getProtocolNodeName(ruleName), ra);
	}
}
