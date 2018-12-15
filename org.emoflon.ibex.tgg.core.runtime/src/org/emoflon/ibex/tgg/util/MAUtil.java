package org.emoflon.ibex.tgg.util;

import java.util.Collection;
import java.util.stream.Collectors;

import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;

import language.BindingType;
import language.DomainType;
import language.LanguageFactory;
import language.TGGAttributeConstraint;
import language.TGGAttributeExpression;
import language.TGGAttributeVariable;
import language.TGGComplementRule;
import language.TGGEnumExpression;
import language.TGGLiteralExpression;
import language.TGGParamValue;
import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;

public class MAUtil {
	public static final String FUSED = "_FUSED_";
	
	public static TGGRuleNode createProxyNode(TGGRuleNode node) {
		TGGRuleNode copiedNode = LanguageFactory.eINSTANCE.createTGGRuleNode();
		copiedNode.setName(node.getName());
		copiedNode.setType(node.getType());
		copiedNode.setBindingType(node.getBindingType());
		copiedNode.setDomainType(node.getDomainType());
		return copiedNode;
	}

	public static void addKernelGivenDomainAndContextNodes(TGGComplementRule rule,
			Collection<TGGRuleNode> signatureNodes, DomainType domain) {
		Collection<TGGRuleNode> kernelNodes = rule.getKernel().getNodes();
		for (TGGRuleNode n : kernelNodes) {
			if (n.getDomainType() == domain || n.getBindingType() == BindingType.CONTEXT)
				signatureNodes.add(createProxyNode(n));
		}
	}

	public static Collection<TGGRuleEdge> getComplementGivenDomainAndContextEdgesNotInKernel(TGGComplementRule rule,
			Collection<TGGRuleNode> signatureNodes, DomainType domain) {
		return rule.getEdges().stream().filter(e -> e.getDomainType() == domain)
				.filter(e -> e.getBindingType() == BindingType.CONTEXT)
				.filter(e -> !rule.getKernel().getEdges().contains(e)).map(e -> createProxyEdge(signatureNodes, e))
				.collect(Collectors.toList());
	}

	private static TGGRuleEdge createProxyEdge(Collection<TGGRuleNode> signatureNodes, TGGRuleEdge e) {
		TGGRuleEdge edge = LanguageFactory.eINSTANCE.createTGGRuleEdge();
		edge.setName(e.getName());
		edge.setType(e.getType());
		edge.setBindingType(e.getBindingType());
		edge.setDomainType(e.getDomainType());
		edge.setSrcNode(signatureNodes.stream().filter(n -> n.getName().equals(e.getSrcNode().getName())).findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Unable to find " + e.getSrcNode().getName() + " in signature nodes!")));
		edge.setTrgNode(signatureNodes.stream().filter(n -> n.getName().equals(e.getTrgNode().getName())).findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Unable to find " + e.getTrgNode().getName() + " in signature nodes!")));
		return edge;
	}

	public static void addComplementGivenDomainAndContextNodes(TGGComplementRule rule,
			Collection<TGGRuleNode> signatureNodes, DomainType domain) {
		for (TGGRuleNode n : rule.getNodes()) {
			if (nodeIsNotInKernel(rule, n)
					&& (n.getDomainType() == domain || n.getBindingType() == BindingType.CONTEXT))
				signatureNodes.add(createProxyNode(n));
		}
	}

	public static boolean nodeIsNotInKernel(TGGComplementRule rule, TGGRuleElement node) {
		return rule.getKernel().getNodes().stream().noneMatch(re -> re.getName().equals(node.getName()));
	}

	public static String setFusedName(String complementName, String kernelName) {
		return complementName + FUSED + kernelName;
	}

	public static String getKernelName(String fusedName) {
		String name = PatternSuffixes.removeSuffix(fusedName);
		return name.split(FUSED)[1];
	}

	public static String getComplementName(String fusedName) {
		String name = PatternSuffixes.removeSuffix(fusedName);
		return name.split(FUSED)[0];
	}

	public static boolean isFusedPatternMatch(String patternName) {
		return patternName.contains(FUSED);
	}

	public static void replaceWith(TGGAttributeConstraint constraint, TGGParamValue toBeReplaced,
			TGGParamValue useToReplace) {
		assert (constraint.getParameters().contains(toBeReplaced));
		constraint.getParameters().replaceAll(p -> p.equals(toBeReplaced) ? useToReplace : p);
	}

	public static boolean equal(TGGParamValue p1, TGGParamValue p2) {
		if (!p1.eClass().getName().equals(p2.eClass().getName()))
			return false;

		// Enums
		if (p1 instanceof TGGEnumExpression) {
			return ((TGGEnumExpression) p1).getEenum().equals(((TGGEnumExpression) p2).getEenum())
					&& ((TGGEnumExpression) p1).getLiteral().equals(((TGGEnumExpression) p2).getLiteral());
		}

		// Literals
		if (p1 instanceof TGGLiteralExpression) {
			return ((TGGLiteralExpression) p1).getValue().equals(((TGGLiteralExpression) p2).getValue());
		}

		// Locals
		if (p1 instanceof TGGAttributeVariable) {
			return ((TGGAttributeVariable) p1).getName().equals(((TGGAttributeVariable) p2).getName());
		}

		// Attribute expressions
		if (p1 instanceof TGGAttributeExpression) {
			return attributeAccess(p1).equals(attributeAccess(p2));
		}

		return false;
	}

	public static String attributeAccess(TGGParamValue p) {
		assert (p instanceof TGGAttributeExpression);
		TGGAttributeExpression exp = (TGGAttributeExpression) p;
		return exp.getObjectVar().getName() + "." + exp.getAttribute().getName();
	}
}
