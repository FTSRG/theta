package hu.bme.mit.theta.analysis.composite;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.Domain;
import hu.bme.mit.theta.analysis.InitFunction;
import hu.bme.mit.theta.analysis.Precision;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransferFunction;

public final class CompositeAnalysis<S1 extends State, S2 extends State, A extends Action, P1 extends Precision, P2 extends Precision>
		implements Analysis<CompositeState<S1, S2>, A, CompositePrecision<P1, P2>> {

	private final Domain<CompositeState<S1, S2>> domain;
	private final InitFunction<CompositeState<S1, S2>, CompositePrecision<P1, P2>> initFunction;
	private final TransferFunction<CompositeState<S1, S2>, A, CompositePrecision<P1, P2>> transferFunction;
	private final LTS<? super CompositeState<S1, S2>, ? extends A> actionFunction;

	private CompositeAnalysis(final Analysis<S1, A, P1> analysis1, final Analysis<S2, A, P2> analysis2) {
		checkNotNull(analysis1);
		checkNotNull(analysis2);
		domain = CompositeDomain.create(analysis1.getDomain(), analysis2.getDomain());
		initFunction = CompositeInitFunction.create(analysis1.getInitFunction(), analysis2.getInitFunction());
		transferFunction = CompositeTransferFunction.create(analysis1.getTransferFunction(),
				analysis2.getTransferFunction());
		actionFunction = CompositeActionFunction.create(analysis1.getActionFunction(), analysis2.getActionFunction());
	}

	public static <S1 extends State, S2 extends State, A extends Action, P1 extends Precision, P2 extends Precision> CompositeAnalysis<S1, S2, A, P1, P2> create(
			final Analysis<S1, A, P1> analysis1, final Analysis<S2, A, P2> analysis2) {
		return new CompositeAnalysis<>(analysis1, analysis2);
	}

	@Override
	public Domain<CompositeState<S1, S2>> getDomain() {
		return domain;
	}

	@Override
	public InitFunction<CompositeState<S1, S2>, CompositePrecision<P1, P2>> getInitFunction() {
		return initFunction;
	}

	@Override
	public TransferFunction<CompositeState<S1, S2>, A, CompositePrecision<P1, P2>> getTransferFunction() {
		return transferFunction;
	}

	@Override
	public LTS<? super CompositeState<S1, S2>, ? extends A> getActionFunction() {
		return actionFunction;
	}

}
