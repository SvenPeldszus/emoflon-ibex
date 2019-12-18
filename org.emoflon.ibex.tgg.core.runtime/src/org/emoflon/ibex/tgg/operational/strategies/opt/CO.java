package org.emoflon.ibex.tgg.operational.strategies.opt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.ibex.tgg.operational.updatepolicy.NextMatchUpdatePolicy;

public final class CO extends IbexExecutable {

	public CO(IbexOptions options) throws IOException {
		strategy = new CO_Op(this, options);
	}
}

class CO_Op extends CC_Op {
	
	protected CO_Op(CO co, IbexOptions options) throws IOException {
		super(co, options, new NextMatchUpdatePolicy());
	}

	@Override
	public Collection<PatternType> getPatternRelevantForCompiler() {
		Collection<PatternType> types = new LinkedList<>();
		types.add(PatternType.CO);
		types.add(PatternType.GENForCO);
		return types;
	}

	@Override
	protected void wrapUp() {
		ArrayList<EObject> objectsToDelete = new ArrayList<EObject>();

		for (int v : chooseTGGRuleApplications()) {
			int id = v < 0 ? -v : v;
			ITGGMatch comatch = idToMatch.get(id);
			if (v < 0)
				objectsToDelete.add(getRuleApplicationNode(comatch));
		}

		EcoreUtil.deleteAll(objectsToDelete, true);
		consistencyReporter.initWithCorr(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.emoflon.ibex.tgg.operational.strategies.OPT#getWeightForMatch(org.emoflon
	 * .ibex.tgg.operational.matches.IMatch, java.lang.String)
	 */
	@Override
	public double getDefaultWeightForMatch(IMatch comatch, String ruleName) {
		return super.getDefaultWeightForMatch(comatch, ruleName)
				+ getGreenFactory(ruleName).getGreenCorrNodesInRule().size();
	}

	@Override
	public boolean modelsAreConsistent() {
		return getInconsistentSrcNodes().size() + getInconsistentTrgNodes().size()
				+ getInconsistentSrcEdges().size() + getInconsistentTrgEdges().size()
				+ getInconsistentCorrNodes().size() == 0;
	}

	public Collection<EObject> getInconsistentCorrNodes() {
		return consistencyReporter.getInconsistentCorrNodes();
	}
}