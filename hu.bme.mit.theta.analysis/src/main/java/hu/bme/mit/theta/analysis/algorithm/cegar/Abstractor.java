package hu.bme.mit.theta.analysis.algorithm.cegar;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Precision;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;

public interface Abstractor<S extends State, A extends Action, P extends Precision> {

	ARG<S, A> init(final P precision);

	AbstractorStatus check(ARG<S, A> arg, P precision);
}
