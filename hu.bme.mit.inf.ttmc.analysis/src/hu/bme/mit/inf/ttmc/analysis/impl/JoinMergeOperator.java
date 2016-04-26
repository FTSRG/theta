package hu.bme.mit.inf.ttmc.analysis.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.inf.ttmc.analysis.Domain;
import hu.bme.mit.inf.ttmc.analysis.MergeOperator;
import hu.bme.mit.inf.ttmc.analysis.State;

public class JoinMergeOperator<S extends State> implements MergeOperator<S> {

	private final Domain<S> domain;

	public JoinMergeOperator(final Domain<S> domain) {
		this.domain = domain;
	}

	@Override
	public S merge(final S state1, final S state2) {
		checkNotNull(state1);
		checkNotNull(state2);
		return domain.join(state1, state2);
	}

}
