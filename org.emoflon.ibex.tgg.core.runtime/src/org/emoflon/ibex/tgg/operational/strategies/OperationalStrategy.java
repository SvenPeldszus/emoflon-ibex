package org.emoflon.ibex.tgg.operational.strategies;

import static org.emoflon.ibex.common.collections.CollectionFactory.cfactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.compiler.patterns.TGGPatternUtil;
import org.emoflon.ibex.tgg.operational.IGreenInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.BenchmarkLogger;
import org.emoflon.ibex.tgg.operational.debug.LoggerConfig;
import org.emoflon.ibex.tgg.operational.defaults.IbexGreenInterpreter;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.matches.DefaultMatchContainer;
import org.emoflon.ibex.tgg.operational.matches.IMatchContainer;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.matches.ImmutableMatchContainer;
import org.emoflon.ibex.tgg.operational.monitoring.AbstractIbexObservable;
import org.emoflon.ibex.tgg.operational.patterns.GreenPatternFactory;
import org.emoflon.ibex.tgg.operational.patterns.IGreenPattern;
import org.emoflon.ibex.tgg.operational.patterns.IGreenPatternFactory;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.ibex.tgg.operational.strategies.modules.MatchDistributor;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import org.emoflon.ibex.tgg.operational.updatepolicy.IUpdatePolicy;
import org.emoflon.ibex.tgg.operational.updatepolicy.NextMatchUpdatePolicy;

import language.TGG;
import language.TGGRuleNode;
import runtime.TGGRuleApplication;

public abstract class OperationalStrategy extends AbstractIbexObservable implements IbexExecutable {
	protected final static Logger logger = Logger.getRootLogger();

	// Match and pattern management
	protected IMatchContainer operationalMatchContainer;
	protected Map<TGGRuleApplication, ITGGMatch> consistencyMatches;
	private boolean domainsHaveNoSharedTypes;
	private Map<String, IGreenPatternFactory> factories;
	
	protected Map<ITGGMatch, String> blockedMatches = cfactory.createObjectToObjectHashMap();

	// Configuration
	protected final IbexOptions options;

	// Model manipulation
	protected IGreenInterpreter greenInterpreter;
	
	protected TGGResourceHandler resourceHandler;

	protected MatchDistributor matchDistributor;
	
	protected long chooseMatchTime = 0;
	protected long finishRuleApplicationTime = 0;
	protected long initMatchApplicationTime = 0;


	/***** Constructors *****/

	public OperationalStrategy(IbexOptions options) {
		this(options, new NextMatchUpdatePolicy());
	}

	protected OperationalStrategy(IbexOptions options, IUpdatePolicy policy) {
		this.options = options;
		initialize(options, policy);
	}
	
	private void initialize(IbexOptions options, IUpdatePolicy policy) {
		BenchmarkLogger.startTimer();
		this.notifyStartInit();
		
		options.executable(this);
		
		this.setUpdatePolicy(policy);

		resourceHandler = options.resourceHandler();
		matchDistributor = options.matchDistributor();
		factories = new HashMap<>();

		matchDistributor.register(getPatternRelevantForCompiler(), this::addOperationalRuleMatch, this::removeOperationalRuleMatch);
		
		this.notifyStartLoading();
		resourceHandler.initialize();
		this.notifyLoadingFinished();
		try {
			matchDistributor.initialize();
		} catch (IOException e) {
			e.printStackTrace();
		}

		greenInterpreter = new IbexGreenInterpreter(this);

		consistencyMatches = cfactory.createObjectToObjectHashMap();
		
		options.debug.benchmarkLogger().addToInitTime(BenchmarkLogger.stopTimer());
		
		this.operationalMatchContainer = createMatchContainer();
		domainsHaveNoSharedTypes = getTGG().getSrc().stream().noneMatch(getTGG().getTrg()::contains);
		
		
		this.notifyDoneInit();
		
		options.debug.benchmarkLogger().addToInitTime(BenchmarkLogger.stopTimer());
	}

	/***** Resource management *****/

	public TGG getTGG() {
		return options.tgg.tgg();
	}

	protected IMatchContainer createMatchContainer() {
		return new DefaultMatchContainer(options.tgg.flattenedTGG());
	}

	@Override
	public void saveModels() throws IOException {
		resourceHandler.saveRelevantModels();
	}

	/***** Match and pattern management *****/
	protected void addOperationalRuleMatch(ITGGMatch match) {
		if (match.getType() == PatternType.CONSISTENCY) {
			addConsistencyMatch(match);
			return;
		}

		if (isPatternRelevantForInterpreter(match.getType()) && matchIsDomainConform(match)) {
			operationalMatchContainer.addMatch(match);
			LoggerConfig.log(LoggerConfig.log_incomingMatches(), () -> "Received and added " + match.getPatternName());
		} else
			LoggerConfig.log(LoggerConfig.log_incomingMatches(), () -> "Received but rejected " + match.getPatternName());
	}

	protected void addConsistencyMatch(ITGGMatch match) {
		TGGRuleApplication ruleAppNode = getRuleApplicationNode(match);
		consistencyMatches.put(ruleAppNode, match);
		LoggerConfig.log(LoggerConfig.log_incomingMatches(), () -> "Received and added consistency match: " + match.getPatternName() + "(" + match.hashCode() + ")");
	}

	protected boolean removeOperationalRuleMatch(ITGGMatch match) {
		return operationalMatchContainer.removeMatch(match);
	}

	
	public boolean isPatternRelevantForInterpreter(PatternType patternType) {
		return getPatternRelevantForCompiler().contains(patternType);
	}
	
	public abstract Collection<PatternType> getPatternRelevantForCompiler();
	
	private boolean matchIsDomainConform(ITGGMatch match) {
		if (domainsHaveNoSharedTypes || options.patterns.ignoreDomainConformity())
			return true;

		return matchedNodesAreInCorrectResource(resourceHandler.getSourceResource(), //
				getGreenFactory(match.getRuleName()).getBlackSrcNodesInRule(), match)
				&& matchedNodesAreInCorrectResource(resourceHandler.getSourceResource(), //
						getGreenFactory(match.getRuleName()).getGreenSrcNodesInRule(), match)
				&& matchedNodesAreInCorrectResource(resourceHandler.getTargetResource(), //
						getGreenFactory(match.getRuleName()).getBlackTrgNodesInRule(), match)
				&& matchedNodesAreInCorrectResource(resourceHandler.getTargetResource(), //
						getGreenFactory(match.getRuleName()).getGreenTrgNodesInRule(), match);
	}

	private boolean matchedNodesAreInCorrectResource(Resource r, Collection<TGGRuleNode> nodes, ITGGMatch match) {
		return nodes.stream().noneMatch(n -> match.isInMatch(n.getName()) && !nodeIsInResource(match, n.getName(), r));
	}

	private Map<EObject, Resource> cacheObjectToResource = cfactory.createObjectToObjectHashMap();

	private boolean nodeIsInResource(ITGGMatch match, String name, Resource r) {
		EObject root = (EObject) match.get(name);
		if (!cacheObjectToResource.containsKey(root))
			cacheObjectToResource.put(root, root.eResource());

		return cacheObjectToResource.get(root).equals(r);
	}

	public IGreenPattern revokes(ITGGMatch match) {
		throw new IllegalStateException("Not clear how to revoke a match of " + match.getPatternName());
	}

	public abstract void run() throws IOException;

	protected boolean processOneOperationalRuleMatch() {
		this.updateBlockedMatches();
		if (operationalMatchContainer.isEmpty())
			return false;

		long tic = System.nanoTime();
		ITGGMatch match = chooseOneMatch();
		String ruleName = match.getRuleName();
		chooseMatchTime += System.nanoTime() - tic;

		Optional<ITGGMatch> result = processOperationalRuleMatch(ruleName, match);
		removeOperationalRuleMatch(match);

		LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "Processing match: " + match);
		if (result.isPresent()) {
			options.debug.benchmarkLogger().addToNumOfMatchesApplied(1);
			LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "Removed as it has just been applied: ");
		} else
			LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "Removed as application failed: ");
		LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "" + match);
		return true;
	}

	protected ITGGMatch chooseOneMatch() {
		ITGGMatch match = this.notifyChooseMatch(new ImmutableMatchContainer(operationalMatchContainer));
		
		if (match == null)
			throw new IllegalStateException("Update policies should never return null!");

		return match;
	}

	protected Optional<ITGGMatch> processOperationalRuleMatch(String ruleName, ITGGMatch match) {
		//generatedPatternsSizeObserver.setNodes(match);
		long tic = System.nanoTime();

		if (getBlockedMatches().containsKey(match)) { 
			LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "Application blocked by update policy.");
			return Optional.empty();
		}

		IGreenPatternFactory factory = getGreenFactory(ruleName);
		IGreenPattern greenPattern = factory.create(match.getType());

		LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "Attempting to apply: " + match.getPatternName() + "(" + match.hashCode() + ") with " + greenPattern);
		initMatchApplicationTime += System.nanoTime() - tic;

		Optional<ITGGMatch> comatch = greenInterpreter.apply(greenPattern, ruleName, match);
		
		comatch.ifPresent(cm -> {
			LoggerConfig.log(LoggerConfig.log_matchApplication(), () -> "Successfully applied: " + match.getPatternName());
			this.notifyMatchApplied(match, ruleName);
			operationalMatchContainer.matchApplied(match);
			
			long l_tic = System.nanoTime();
			handleSuccessfulRuleApplication(cm, ruleName, greenPattern);
			finishRuleApplicationTime += System.nanoTime() - l_tic;
		});

		return comatch;
	}

	protected void handleSuccessfulRuleApplication(ITGGMatch cm, String ruleName, IGreenPattern greenPattern) {
		createMarkers(greenPattern, cm, ruleName);
	}

	public TGGRuleApplication getRuleApplicationNode(ITGGMatch match) {
		return (TGGRuleApplication) match.get(TGGPatternUtil.getProtocolNodeName(//
				PatternSuffixes.removeSuffix(match.getPatternName())));
	}

	protected void updateBlockedMatches() {
		if(this.getUpdatePolicy() instanceof NextMatchUpdatePolicy)
			return;
		
		for(ITGGMatch match : operationalMatchContainer.getMatches()) {
			if(!this.getUpdatePolicy().matchShouldBeApplied(match, match.getRuleName())) {
				if(!blockedMatches.containsKey(match))
					blockedMatches.put(match, "Match is blocked by the update policy");
				this.operationalMatchContainer.removeMatch(match);
			}
		}
	}

	public Map<ITGGMatch, String> getBlockedMatches() {
	    return blockedMatches;
	}
	
	public IGreenInterpreter getGreenInterpreter() {
		return greenInterpreter;
	}

	public void terminate() throws IOException {
		matchDistributor.removeBlackInterpreter();
		operationalMatchContainer.removeAllMatches();
		cacheObjectToResource.clear();
	}

	public synchronized IGreenPatternFactory getGreenFactory(String ruleName) {
		assert (ruleName != null);
		if (!factories.containsKey(ruleName)) {
			factories.put(ruleName, new GreenPatternFactory(options, ruleName));
		}

		return factories.get(ruleName);
	}
	
	protected void prepareMarkerCreation(IGreenPattern greenPattern, ITGGMatch comatch, String ruleName) {

	}

	protected void createMarkers(IGreenPattern greenPattern, ITGGMatch comatch, String ruleName) {
		prepareMarkerCreation(greenPattern, comatch, ruleName);
		greenPattern.createMarkers(ruleName, comatch);
	}

	/***** Configuration *****/

	public Map<String, IGreenPatternFactory> getFactories() {
		return factories;
	}

	public IbexOptions getOptions() {
		return options;
	}
	
	public IMatchContainer getMatchContainer() {
		return operationalMatchContainer;
	}
	
	protected void collectDataToBeLogged() {
		matchDistributor.collectDataToBeLogged();
	}
	
	public TGGResourceHandler getResourceHandler() {
		return resourceHandler;
	}
	
	public void setResourceHandler(TGGResourceHandler resourceHandler) {
		this.resourceHandler = resourceHandler;
	}
}