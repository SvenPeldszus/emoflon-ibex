package org.emoflon.ibex.tgg.operational.strategies.opt;

import static org.emoflon.ibex.common.collections.CollectionFactory.cfactory;

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
import org.emoflon.ibex.tgg.operational.patterns.IGreenPattern;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;

import language.TGGRuleCorr;
import language.TGGRuleNode;

public final class BWD_OPT extends IbexExecutable {
	
	public BWD_OPT(IbexOptions options) throws IOException {
		strategy = new BWD_OPT_Op(this, options);
	}
	
	class BWD_OPT_Op extends OPT {

		public BWD_OPT_Op(BWD_OPT bwd_Opt, IbexOptions options) throws IOException {
			super(bwd_Opt, options);
			relaxReferences(options.tgg().getSrc());
		}

		@Override
		protected void wrapUp() {
			ArrayList<EObject> objectsToDelete = new ArrayList<EObject>();
			for (int v : chooseTGGRuleApplications()) {
				int id = v < 0 ? -v : v;
				ITGGMatch comatch = idToMatch.get(id);
				if (v < 0) {
					for (TGGRuleCorr createdCorr : getGreenFactory(matchIdToRuleName.get(id)).getGreenCorrNodesInRule())
						objectsToDelete.add((EObject) comatch.get(createdCorr.getName()));

					for (TGGRuleNode createdSrcNode : getGreenFactory(matchIdToRuleName.get(id))
							.getGreenSrcNodesInRule())
						objectsToDelete.add((EObject) comatch.get(createdSrcNode.getName()));

					objectsToDelete.addAll(getRuleApplicationNodes(comatch));
				}
			}

			EcoreUtil.deleteAll(objectsToDelete, true);
			consistencyReporter.initTrg(this);
		}

		@Override
		public Collection<PatternType> getPatternRelevantForCompiler() {
			Collection<PatternType> types = new LinkedList<>();
			types.add(PatternType.BWD_OPT);
			return types;
		}

		@Override
		public boolean isPatternRelevantForInterpreter(PatternType type) {
			return type == PatternType.BWD_OPT;
		}

		@Override
		protected void prepareMarkerCreation(IGreenPattern greenPattern, ITGGMatch comatch, String ruleName) {
			idToMatch.put(idCounter, comatch);
			matchIdToRuleName.put(idCounter, ruleName);
			matchToWeight.put(idCounter, this.getWeightForMatch(comatch, ruleName));

			getGreenNodes(comatch, ruleName).forEach(e -> {
				if (!nodeToMarkingMatches.containsKey(e))
					nodeToMarkingMatches.put(e, cfactory.createIntSet());
				nodeToMarkingMatches.get(e).add(idCounter);
			});

			getGreenEdges(comatch, ruleName).forEach(e -> {
				if (!edgeToMarkingMatches.containsKey(e)) {
					edgeToMarkingMatches.put(e, cfactory.createIntSet());
				}
				edgeToMarkingMatches.get(e).add(idCounter);
			});

			getBlackNodes(comatch, ruleName).forEach(e -> {
				if (!contextNodeToNeedingMatches.containsKey(e))
					contextNodeToNeedingMatches.put(e, cfactory.createIntSet());
				contextNodeToNeedingMatches.get(e).add(idCounter);
			});

			getBlackEdges(comatch, ruleName).forEach(e -> {
				if (!contextEdgeToNeedingMatches.containsKey(e)) {
					contextEdgeToNeedingMatches.put(e, cfactory.createIntSet());
				}
				contextEdgeToNeedingMatches.get(e).add(idCounter);
			});

			matchToContextNodes.put(idCounter, cfactory.createObjectSet());
			matchToContextNodes.get(idCounter).addAll(getBlackNodes(comatch, ruleName));

			matchToContextEdges.put(idCounter, cfactory.createEMFEdgeHashSet());
			matchToContextEdges.get(idCounter).addAll(getBlackEdges(comatch, ruleName));

			idCounter++;
		}

		@Override
		public void terminate() {
			// Unrelax the metamodel
			unrelaxReferences(options.tgg().getSrc());

			// Remove adapters to avoid problems with notifications
			resourceHandler.getSourceResource().eAdapters().clear();
			resourceHandler.getSourceResource().getAllContents().forEachRemaining(o -> o.eAdapters().clear());
			resourceHandler.getCorrResource().eAdapters().clear();
			resourceHandler.getCorrResource().getAllContents().forEachRemaining(o -> o.eAdapters().clear());

			// Copy and fix the model in the process
			FixingCopier.fixAll(resourceHandler.getSourceResource(), resourceHandler.getCorrResource(), "source");

			// Now save fixed models
			try {
				resourceHandler.saveModels();
			} catch (IOException e) {
				e.printStackTrace();
			}

			super.terminate();
		}

		@Override
		public double getDefaultWeightForMatch(IMatch comatch, String ruleName) {
			return getGreenFactory(ruleName).getGreenTrgEdgesInRule().size()
					+ getGreenFactory(ruleName).getGreenTrgNodesInRule().size();
		}

		public void backward() throws IOException {
			run();
		}
	}
}
