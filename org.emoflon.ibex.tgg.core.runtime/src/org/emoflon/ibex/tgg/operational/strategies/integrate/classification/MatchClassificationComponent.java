package org.emoflon.ibex.tgg.operational.strategies.integrate.classification;

import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.emf.EMFEdge;
import org.emoflon.ibex.tgg.operational.strategies.integrate.INTEGRATE;
import org.emoflon.ibex.tgg.operational.strategies.integrate.Mismatch;
import org.emoflon.ibex.tgg.operational.strategies.integrate.util.MatchAnalysis;

import language.DomainType;
import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;

public abstract class MatchClassificationComponent {

	abstract public Mismatch classify(INTEGRATE integrate, MatchAnalysis analysis);

	abstract public boolean isApplicable(MatchAnalysis analysis);

	protected DomainType oppositeOf(DomainType type) {
		switch (type) {
		case SRC:
			return DomainType.TRG;
		case TRG:
			return DomainType.SRC;
		case CORR:
			return DomainType.CORR;
		default:
			return null;
		}
	}

	protected void classifyElts(INTEGRATE integrate, Mismatch mismatch, Set<TGGRuleElement> elements,
			EltClassifier classifier) {
		elements.forEach(elt -> {
			if (elt instanceof TGGRuleNode) {
				EObject node = (EObject) mismatch.getMatch().get(elt.getName());
				mismatch.addClassification(node, classifier);
			} else if (elt instanceof TGGRuleEdge) {
				EMFEdge edge = integrate.getRuntimeEdge(mismatch.getMatch(), (TGGRuleEdge) elt);
				mismatch.addClassification(edge, classifier);
			}
		});
	}

}
