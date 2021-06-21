package hu.bme.mit.theta.xta.testgen;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XtaTest<S extends XtaState<? extends State>, A extends XtaAction> {
	private final String name;
	private List<Double> delays;
	private final ArgTrace<S, A> trace;

	public XtaTest(String name, List<Double> delays, ArgTrace<S, A> trace) {
		this.name = name;
		this.delays = delays;
		this.trace = trace;
	}

	public String getName() {
		return name;
	}

	public List<Double> getDelays() {
		return delays;
	}

	public void setDelays(List<Double> delays) { this.delays = delays; }

	public ArgTrace<S, A> getTrace() {
		return trace;
	}

	public int getLength() { return trace.nodes().size(); }

	public double getTotalTime() {
		return delays.stream().mapToDouble(Double::doubleValue).sum();
	}

	public Set<XtaProcess.Loc> getLocs() {
		Set<XtaProcess.Loc> locs = new HashSet<>();
		for (ArgNode<S, A> node : trace.nodes()) {
			locs.addAll(node.getState().getLocs());
		}
		return locs;
	}
}