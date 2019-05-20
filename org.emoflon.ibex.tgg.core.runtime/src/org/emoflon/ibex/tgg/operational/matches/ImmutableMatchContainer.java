package org.emoflon.ibex.tgg.operational.matches;

import java.util.Collections;
import java.util.Set;

public class ImmutableMatchContainer {
	
	private IMatchContainer matchContainer;
	
	public ImmutableMatchContainer(IMatchContainer matchContainer) {
		this.matchContainer = matchContainer;
	}
	
	public IMatch getNext() {
		return matchContainer.getNext();
	}
	
	public Set<IMatch> getMatches() {
		return Collections.unmodifiableSet(matchContainer.getMatches());
	}
	
	public boolean isEmpty() {
		return matchContainer.isEmpty();
	}

	public String getRuleName(IMatch match) {
		return matchContainer.getRuleName(match);
	}

}
