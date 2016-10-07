package hu.bme.mit.theta.analysis.tcfa.pred;

import static hu.bme.mit.theta.core.decl.impl.Decls.Var;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Eq;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Int;
import static hu.bme.mit.theta.core.type.impl.Types.Int;

import java.util.Collections;

import org.junit.Test;

import hu.bme.mit.theta.analysis.algorithm.LifoWaitlist;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.WaitlistBasedAbstractor;
import hu.bme.mit.theta.analysis.automaton.AutomatonPrecision;
import hu.bme.mit.theta.analysis.automaton.AutomatonState;
import hu.bme.mit.theta.analysis.pred.PredPrecision;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.pred.SimplePredPrecision;
import hu.bme.mit.theta.analysis.tcfa.TcfaAction;
import hu.bme.mit.theta.analysis.tcfa.TcfaAnalyis;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.common.visualization.GraphVizWriter;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.IntType;
import hu.bme.mit.theta.formalism.tcfa.TcfaEdge;
import hu.bme.mit.theta.formalism.tcfa.TcfaLoc;
import hu.bme.mit.theta.formalism.tcfa.instances.FischerTcfa;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public class TcfaPredTest {

	@Test
	public void test() {
		final VarDecl<IntType> vlock = Var("lock", Int());
		final FischerTcfa fischer = new FischerTcfa(1, 1, 2, vlock);

		final Solver solver = Z3SolverFactory.getInstace().createSolver();

		final TcfaAnalyis<PredState, PredPrecision> analysis = TcfaAnalyis.create(fischer.getInitial(),
				TcfaPredAnalysis.create(solver));

		final PredPrecision subPrecision = SimplePredPrecision
				.create(Collections.singleton(Eq(vlock.getRef(), Int(0))));
		final AutomatonPrecision<PredPrecision, TcfaLoc, TcfaEdge> precision = AutomatonPrecision
				.create(l -> subPrecision);

		final Abstractor<AutomatonState<PredState, TcfaLoc, TcfaEdge>, TcfaAction, AutomatonPrecision<PredPrecision, TcfaLoc, TcfaEdge>> abstractor = new WaitlistBasedAbstractor<>(
				analysis, s -> s.getLoc().equals(fischer.getCritical()), new LifoWaitlist<>());

		abstractor.init(precision);
		abstractor.check(precision);

		System.out.println(new GraphVizWriter().writeString(ArgVisualizer.visualize(abstractor.getARG())));

		System.out.println("\n\nCounterexample(s):");
		System.out.println(abstractor.getARG().getCexs());

	}

}
