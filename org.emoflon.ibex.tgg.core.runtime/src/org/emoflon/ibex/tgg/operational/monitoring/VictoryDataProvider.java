package org.emoflon.ibex.tgg.operational.monitoring;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.operational.matches.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;

import language.TGGRule;

/**
 * Gathering and processing data from IBEX
 */
public class VictoryDataProvider implements IVictoryDataProvider {

	private final static Logger logger = Logger.getLogger(VictoryDataProvider.class);

	String[][] defaultSaveData = new String[4][1];

	OperationalStrategy op;
	String[] savedPLocations;

	public VictoryDataProvider(OperationalStrategy pOperationalStrategy) {
		this.op = pOperationalStrategy;
		this.getDefaultSaveLocation();
	}

	@Override
	public TGGRule getRule(String pRuleName) {
		try {
			return op.getOptions().flattenedTGG().getRules().stream().filter(r -> r.getName().equals(pRuleName))
					.findFirst().get();
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public Collection<EObject> getMatchNeighbourhoods(Collection<EObject> nodes, int k) {
		try {
			Collection<EObject> neighbors = new HashSet<EObject>();
			for (EObject node : nodes) {
				neighbors.addAll(getMatchNeighbourhood(node, k));
			}
			return neighbors;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public Collection<EObject> getMatchNeighbourhood(EObject node, int k) {
		try {
			Set<EObject> neighbors = new HashSet<EObject>();
			if (k >= 0) {
				neighbors.add(node);

				if (k > 0) {
					calculateNeighbourhood(neighbors, node, 0, k);
				}
			}

			return neighbors;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}
	
	/**
	 * Calculation of neighboring nodes in recursive manner
	 * @param neighbors the neighbors of distance 1
	 * @param node the node we want to calculate its neighbors
	 * @param i counter
	 * @param k the number of neighbors to return for the node counted by distance
	 */
	private void calculateNeighbourhood(Collection<EObject> neighbors, EObject node, int i, int k) {

		i++;
		Set<EObject> currentNeighbors = new HashSet<EObject>();
		currentNeighbors = node.eContents().stream().collect(Collectors.toSet());
		neighbors.addAll(currentNeighbors);
		Iterator<EObject> cn = currentNeighbors.iterator();
		if (i < k) {
			while (cn.hasNext()) {
				calculateNeighbourhood(neighbors, cn.next(), i, k);
			}
		}

	}

	@Override
	public Set<IMatch> getMatches() {
		try {
			return op.getMatchContainer().getMatches();
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public Set<IMatch> getMatches(String pRuleName) {
		try {
			return this.getMatches().stream().filter(r -> r.getRuleName().equals(pRuleName))
					.collect(Collectors.toSet());
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public Set<IMatch> getMatches(IMatch match) {
		try {
			return this.getMatches(match.getRuleName());
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public Set<URI> saveModels(String[] pLocations) throws IOException {
		this.savedPLocations = pLocations;

		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
		Date date = new Date(System.currentTimeMillis());
		String time = dateFormat.format(date).toString();
		LinkedHashMap<String, Resource> resources = new LinkedHashMap<String, Resource>();
		LinkedHashMap<String, URI> oldUri = new LinkedHashMap<String, URI>();
		Set<URI> newUri = new HashSet<URI>();

		// storing resources that needs to be saved
		resources.put("s", op.getSourceResource());
		resources.put("t", op.getTargetResource());
		resources.put("c", op.getCorrResource());
		resources.put("p", op.getProtocolResource());

		System.out.println(Arrays.toString(pLocations));

		// save models
		int count = 0;
		for (Entry<String, Resource> e : resources.entrySet()) {
			oldUri.put(e.getKey(), e.getValue().getURI());

			if (pLocations.length > 0) {
				URI uri = saveModel(e.getValue(), time, pLocations[count]);
				newUri.add(uri);
			} else {
				saveModel(e.getValue(), time, null);
			}
			count++;
		}

		// revert the URIs to before saving models
		for (Entry<String, URI> e : oldUri.entrySet()) {
			resources.get(e.getKey()).setURI(e.getValue());
		}
		return newUri;
	}
	
	/**
	 * Saves each model at passed location
	 * @param r Resource to be saved
	 * @param time current time for unique model name
	 * @param newLocation path to save model at
	 * @return newUri
	 * @throws IOException
	 */
	private URI saveModel(Resource r, String time, String newLocation) throws IOException {
		String newPath;
		URI newUri;
		File file = new File(newLocation);

		if (file.isAbsolute()) {
			newPath = "file:/" + FilenameUtils.getFullPath(newLocation);
		} else {
			newPath = FilenameUtils.getFullPath("platform:" + newLocation);
			System.out.println(newPath);
		}

		newPath += FilenameUtils.getBaseName(newLocation) + "-";
		newPath += time + "." + FilenameUtils.getExtension(newLocation);
		newUri = URI.createURI(newPath);

		r.setURI(newUri);
		r.save(null);

		return newUri;
	}
	
	/**
	 * @return saved locations
	 */
	public String[] getPLocations() {
		return this.savedPLocations;
	}
	
	/**
	 * Creating a map for of default locations for each resource to save models
	 */
	public void getDefaultSaveLocation() {
		int count = 0;
		LinkedHashMap<String, Resource> resources = new LinkedHashMap<String, Resource>();

		resources.put("s", op.getSourceResource());
		resources.put("t", op.getTargetResource());
		resources.put("c", op.getCorrResource());
		resources.put("p", op.getProtocolResource());

		for (Entry<String, Resource> e : resources.entrySet()) {
			Resource r = e.getValue();
			String uri = r.getURI().toString();

			String path = FilenameUtils.getFullPath(r.getURI().path());
			String fileName = FilenameUtils.getBaseName(uri);
			String extension = FilenameUtils.getExtension(uri);

			defaultSaveData[count] = new String[] { path, fileName, extension };
			count++;
		}

	}
	
	/**
	 * Returns default locations map for each resource
	 */
	public String[][] getDefaultSaveData() {
		return defaultSaveData;
	}

	@Override
	public Collection<TGGRule> getAllRules() {
		return op.getOptions().flattenedTGG().getRules();
	}
}
