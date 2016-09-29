package hu.bme.mit.theta.analysis.tcfa.zone;

import static hu.bme.mit.theta.core.decl.impl.Decls.Var;
import static hu.bme.mit.theta.core.type.impl.Types.Int;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorImpl;
import hu.bme.mit.theta.analysis.tcfa.TcfaAction;
import hu.bme.mit.theta.analysis.tcfa.TcfaAnalyis;
import hu.bme.mit.theta.analysis.tcfa.TcfaState;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.analysis.zone.ZonePrecision;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.visualization.GraphVizWriter;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.IntType;
import hu.bme.mit.theta.formalism.tcfa.instances.FischerTcfa;

public class TcfaZoneTest {

	@Test
	@Ignore
	public void test() {
		final VarDecl<IntType> vlock = Var("lock", Int());
		final FischerTcfa fischer = new FischerTcfa(1, 1, 2, vlock);

		final TcfaAnalyis<ZoneState, ZonePrecision> analyis = TcfaAnalyis.create(fischer.getInitial(),
				TcfaZoneAnalysis.getInstance());

		final ZonePrecision precision = ZonePrecision.create(Collections.singleton(fischer.getClock()));

		final Abstractor<TcfaState<ZoneState>, TcfaAction, ZonePrecision> abstractor = new AbstractorImpl<>(analyis,
				s -> s.getLoc().equals(fischer.getCritical()));

		abstractor.init(precision);
		abstractor.check(precision);

		System.out.println(new GraphVizWriter().writeString(ArgVisualizer.visualize(abstractor.getARG())));

	}

}
