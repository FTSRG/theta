package hu.bme.mit.theta.xta.testgen;

import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.IndexedConstDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.abstracttype.AddExpr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.abstracttype.LeqExpr;
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.core.type.booltype.AndExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.rattype.RatLitExpr;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.utils.WithPushPop;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.analysis.lazy.LazyXtaStatistics;
import hu.bme.mit.theta.xta.analysis.lazy.LazyXtaTestgenStatistics;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public final class XtaTestGenerator<S extends XtaState<?>, A extends XtaAction> {
	private final ARG<S, A> arg;
	private final XtaSystem system;
	private final Solver solver;
	private final Logger logger;
	private final LazyXtaTestgenStatistics stats;

	private ConstDecl<RatType> totalTime = null;

	public XtaTestGenerator(ARG<S, A> arg, XtaSystem system, Solver solver, Logger logger, LazyXtaStatistics stats) {
		this.arg = arg;
		this.system = system;
		this.solver = solver;
		this.logger = logger;
		this.stats = new LazyXtaTestgenStatistics(stats);
	}

	public LazyXtaTestgenStatistics getStats() {
		return stats;
	}

	public Set<? extends XtaTest<S, A>> generateTests() {
		stats.startTestgen();

		Map<XtaProcess.Loc, XtaTest<S, A>> locTests = new HashMap<>();
		int testCnt = 1;

		int locCount = system.getProcesses().stream()
				.map(p -> p.getLocs().size())
				.mapToInt(Integer::intValue)
				.sum();

		Set<ArgNode<S, A>> nodesToProcess = arg.getInitNodes().collect(Collectors.toSet());
		Set<ArgNode<S, A>> nextNodes = new HashSet<>();

		while (locTests.size() < locCount && !nodesToProcess.isEmpty()) {
			for (ArgNode<S, A> node : nodesToProcess) {
				//SPIN 2018 paper Lemma 2.: we have nothing to do with excluded nodes
				if (node.isExcluded())
					continue;

				XtaTest<S, A> test = generateTest(node, testCnt++, locTests.keySet());
				int newLocs = (int) test.getLocs().stream()
						.filter(l -> !locTests.containsKey(l))
						.count();
				if (newLocs > 0) {
					for (XtaProcess.Loc loc : test.getLocs()) {
						locTests.put(loc, test);
					}
				}

				if (locTests.size() == locCount)
					break;

				nextNodes.addAll(node.children().collect(Collectors.toList()));
			}
			nodesToProcess.clear();
			nodesToProcess.addAll(nextNodes);
			nextNodes.clear();
		}

		var testSet = new HashSet<>(locTests.values());
		for (var test : testSet) {
			concretizeTest(test);
		}

		stats.stopTestgen();
		stats.setTestCases(testSet.size());
		testSet.forEach(t -> stats.addTestCaseLength(t.getLength()));

		return testSet;
	}

	private void concretizeTest(XtaTest<S, A> test) {
		ArgTrace<S, A> trace = test.getTrace();
		List<Double> delays = calculateDelays(trace);
		test.setDelays(delays);
	}

	private XtaTest<S, A> generateTest(ArgNode<S, A> node, int testCnt, Set<XtaProcess.Loc> doneLocs) {
		stats.testCaseGenerated();

		ArgTrace<S, A> trace = ArgTrace.to(node);
		List<XtaProcess.Loc> locs = trace.nodes().stream()
				.flatMap(n -> n.getState().getLocs().stream())
				.filter (l -> !doneLocs.contains(l))
				.collect(Collectors.toList());
		String locNames = locs.stream().map(XtaProcess.Loc::getName).collect(Collectors.joining("_"));
		return new XtaTest<>(String.format("TRACE__%d__%s", testCnt, locNames), null, trace);
	}

	private List<Double> calculateDelays(ArgTrace<S, A> trace) {
		Double[] delays = new Double[trace.nodes().size()];
		try (WithPushPop wpp = new WithPushPop(solver)) {
			int nodeCount = trace.nodes().size();
			List<VarIndexing> indexing = new ArrayList<>(nodeCount);
			indexing.add(VarIndexing.all(0));

			addInitialClockConstraints(indexing);
			addInitialNodeConstraint(trace, indexing);

			assert solver.check().isSat() : "Initial state of the trace is not feasible.";

			addTraceConstraints(trace, indexing);
			addSumOfDelaysConstraint(trace);

			boolean sat = solver.check().isSat();
			if (sat) {
				Valuation bestValuation = reduceDelays();
				extractDelays(bestValuation, delays);
			}
		}
		return List.of(delays);
	}

	private void addInitialClockConstraints(List<VarIndexing> indexing) {
		//initial clock constraints: every clock:0 should equal delay:0
		List<Expr<BoolType>> clockTerms = new ArrayList<>();
		RefExpr<RatType> delayRef = RefExpr.of(XtaAction.DELAY);
		for (var cv : system.getClockVars()) {
			RefExpr<RatType> clockVarRef = RefExpr.of(cv);
			clockTerms.add(EqExpr.create2(clockVarRef, delayRef));
		}
		Expr<BoolType> initialClockConstraint = PathUtils.unfold(AndExpr.of(clockTerms), indexing.get(0));
		solver.add(initialClockConstraint);
	}

	private void addInitialNodeConstraint(ArgTrace<S,A> trace, List<VarIndexing> indexing) {
		Expr<BoolType> initialConstraint = PathUtils.unfold(trace.nodes().get(0).getState().toExpr(), indexing.get(0));
		solver.add(initialConstraint);
	}

	private void addTraceConstraints(ArgTrace<S, A> trace, List<VarIndexing> indexing) {
		for (int i = 1; i < trace.nodes().size(); i++) {
			XtaState<?> st = trace.nodes().get(i).getState();
			XtaAction a = trace.edges().get(i - 1).getAction();

			indexing.add(indexing.get(i - 1).add(a.nextIndexing()));

			Expr<BoolType> actionConstraint = PathUtils.unfold(a.toExpr(), indexing.get(i - 1));
			solver.add(actionConstraint);

			Expr<BoolType> stateConstraint = PathUtils.unfold(st.toExpr(), indexing.get(i));
			solver.add(stateConstraint);
		}
	}

	private void addSumOfDelaysConstraint(ArgTrace<S,A> trace) {
		totalTime = Decls.Const("__total__time__", RatType.getInstance());
		RefExpr<RatType> totalTimeRef = RefExpr.of(totalTime);
		RefExpr<RatType> delayRef = RefExpr.of(XtaAction.DELAY);
		List<Expr<RatType>> delayRefs = new ArrayList<>();
		for (int i = 0; i < trace.nodes().size(); i++) {
			delayRefs.add(PathUtils.unfold(delayRef, i));
		}
		EqExpr<?> totalTimeEq = EqExpr.create2(AddExpr.create2(delayRefs), totalTimeRef);
		solver.add(totalTimeEq);
	}

	private Valuation reduceDelays() {
		RatLitExpr min = RatLitExpr.of(BigInteger.ZERO, BigInteger.ONE);
		RatLitExpr max = getSolverTotalTime();
		RatLitExpr newMax = min.add(max.sub(min).div(RatLitExpr.of(BigInteger.TWO, BigInteger.ONE)));

		Valuation bestVal = solver.getModel();
		logger.write(Logger.Level.INFO, "Current best solution: %f%n", getDoubleValue(max));

		while (max.sub(newMax).geq(RatLitExpr.of(BigInteger.ONE, BigInteger.TWO)).getValue()) {
			logger.write(Logger.Level.INFO, "Looking for solution between %f and %f%n", getDoubleValue(min), getDoubleValue(newMax));
			solver.push();
			Expr<BoolType> newAssertion = LeqExpr.create2(RefExpr.of(totalTime), newMax);
			solver.add(newAssertion);

			if (solver.check().isSat()) {
				max = getSolverTotalTime();
				bestVal = solver.getModel();
				logger.write(Logger.Level.INFO, "Found better solution: %f%n", getDoubleValue(max));
			} else {
				logger.write(Logger.Level.INFO, "Did not find better solution.");
				min = newMax;
			}
			newMax = min.add(max.sub(min).div(RatLitExpr.of(BigInteger.TWO, BigInteger.ONE)));
			solver.pop();
		}
		return bestVal;
	}

	private RatLitExpr getSolverTotalTime() {
		return (RatLitExpr) solver.getModel().toMap().get(totalTime);
	}

	private void extractDelays(Valuation valuation, Double[] delays) {
		var valMap = valuation.toMap();

		for (var entry : valMap.entrySet()) {
			if (entry.getKey() instanceof IndexedConstDecl) {
				IndexedConstDecl<?> icd = (IndexedConstDecl<?>) entry.getKey();
				if (icd.getVarDecl().equals(XtaAction.DELAY)) {
					RatLitExpr val = (RatLitExpr) entry.getValue();
					delays[icd.getIndex()] = getDoubleValue(val);
				}
			}
		}
	}

	private double getDoubleValue(RatLitExpr rat) {
		return rat.getNum().doubleValue() / rat.getDenom().doubleValue();
	}
}