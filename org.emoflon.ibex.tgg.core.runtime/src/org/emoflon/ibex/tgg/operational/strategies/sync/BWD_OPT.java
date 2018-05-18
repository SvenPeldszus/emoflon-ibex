package org.emoflon.ibex.tgg.operational.strategies.sync;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.edge.RuntimeEdge;
import org.emoflon.ibex.tgg.operational.edge.RuntimeEdgeHashingStrategy;
import org.emoflon.ibex.tgg.operational.matches.IMatch;
import org.emoflon.ibex.tgg.operational.patterns.IGreenPattern;
import org.emoflon.ibex.tgg.operational.strategies.OPT;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import language.TGGRuleCorr;
import language.TGGRuleNode;

public abstract class BWD_OPT extends OPT {

	public BWD_OPT(IbexOptions options) throws IOException {
		super(options);
	}

	@Override
	public void loadModels() throws IOException {
		s = createResource(projectPath + "/instances/src.xmi");
		t = loadResource(projectPath + "/instances/trg.xmi");
		c = createResource(projectPath + "/instances/corr.xmi");
		p = createResource(projectPath + "/instances/protocol.xmi");

		EcoreUtil.resolveAll(rs);
	}

	@Override
	protected void wrapUp() {
		ArrayList<EObject> objectsToDelete = new ArrayList<EObject>();
		for (int v : chooseTGGRuleApplications()) {
			int id = v < 0 ? -v : v;
			IMatch comatch = idToMatch.get(id);
			if (v < 0) {
				for (TGGRuleCorr createdCorr : getGreenFactory(matchIdToRuleName.get(id)).getGreenCorrNodesInRule())
					objectsToDelete.add((EObject) comatch.get(createdCorr.getName()));

				for (TGGRuleNode createdSrcNode : getGreenFactory(matchIdToRuleName.get(id)).getGreenSrcNodesInRule())
					objectsToDelete.add((EObject) comatch.get(createdSrcNode.getName()));

				objectsToDelete.addAll(getRuleApplicationNodes(comatch));
			}
		}

		EcoreUtil.deleteAll(objectsToDelete, true);
		consistencyReporter.initTrg(this);
	}

	@Override
	public boolean isPatternRelevantForCompiler(String patternName) {
		return patternName.endsWith(PatternSuffixes.BWD_OPT);
	}

	@Override
	public boolean isPatternRelevantForInterpreter(String patternName) {
		return patternName.endsWith(PatternSuffixes.BWD_OPT);
	}

	@Override
	protected void prepareMarkerCreation(IGreenPattern greenPattern, IMatch comatch, String ruleName) {
		idToMatch.put(idCounter, comatch);
		matchIdToRuleName.put(idCounter, ruleName);
		matchToWeight.put(idCounter, this.getWeightForMatch(comatch, ruleName));

		getGreenNodes(comatch, ruleName).forEach(e -> {
			if (!nodeToMarkingMatches.containsKey(e))
				nodeToMarkingMatches.put(e, new IntOpenHashSet());
			nodeToMarkingMatches.get(e).add(idCounter);
		});

		getGreenEdges(comatch, ruleName).forEach(e -> {
			if (!edgeToMarkingMatches.containsKey(e)) {
				edgeToMarkingMatches.put(e, new IntOpenHashSet());
			}
			edgeToMarkingMatches.get(e).add(idCounter);
		});
		
		getBlackNodes(comatch, ruleName).forEach(e -> {
			if (!contextNodeToNeedingMatches.containsKey(e))
				contextNodeToNeedingMatches.put(e, new IntOpenHashSet());
			contextNodeToNeedingMatches.get(e).add(idCounter);
		});
		
		getBlackEdges(comatch, ruleName).forEach(e -> {
			if (!contextEdgeToNeedingMatches.containsKey(e)) {
				contextEdgeToNeedingMatches.put(e, new IntOpenHashSet());
			}
			contextEdgeToNeedingMatches.get(e).add(idCounter);
		});

		matchToContextNodes.put(idCounter, new ObjectOpenHashSet<>());
		matchToContextNodes.get(idCounter).addAll(getBlackNodes(comatch, ruleName));

		matchToContextEdges.put(idCounter, new ObjectOpenCustomHashSet<RuntimeEdge>(new RuntimeEdgeHashingStrategy()));
		matchToContextEdges.get(idCounter).addAll(getBlackEdges(comatch, ruleName));

		handleBundles(comatch, ruleName);

		idCounter++;
	}

	@Override
	public void saveModels() throws IOException {
		p.save(null);

		// Unrelax the metamodel
		unrelaxReferences(options.tgg().getSrc());

		// Remove adapters to avoid problems with notifications
		s.eAdapters().clear();
		s.getAllContents().forEachRemaining(o -> o.eAdapters().clear());

		// Copy and fix the model in the process
		FixingCopier.fixAll(s, c, "source");

		// Now save fixed models
		s.save(null);
		c.save(null);
	}

	/* (non-Javadoc)
	 * @see org.emoflon.ibex.tgg.operational.strategies.OPT#getWeightForMatch(org.emoflon.ibex.tgg.operational.matches.IMatch, java.lang.String)
	 */
	@Override
	protected int getWeightForMatch(IMatch comatch, String ruleName) {
		return
				getGreenFactory(ruleName).getGreenTrgEdgesInRule().size() + 
				getGreenFactory(ruleName).getGreenTrgNodesInRule().size();
	}

	public void backward() throws IOException {
		run();
	}

	@Override
	public void run() throws IOException {
		do {
			blackInterpreter.updateMatches();
		} while (processOneOperationalRuleMatch());

		wrapUp();
	}

	@Override
	public void loadTGG() throws IOException {
		super.loadTGG();
		relaxReferences(options.tgg().getSrc());
	}
}
