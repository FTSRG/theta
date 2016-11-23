package hu.bme.mit.theta.analysis.expr;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.model.Model;
import hu.bme.mit.theta.core.model.impl.Valuation;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.PathUtils;
import hu.bme.mit.theta.core.utils.impl.VarIndexing;
import hu.bme.mit.theta.solver.Solver;

public class ExprTraceUnsatCoreChecker implements ExprTraceChecker<UnsatCoreRefutation> {

	private final Solver solver;
	private final Expr<? extends BoolType> init;
	private final Expr<? extends BoolType> target;

	private ExprTraceUnsatCoreChecker(final Expr<? extends BoolType> init, final Expr<? extends BoolType> target,
			final Solver solver) {
		this.solver = checkNotNull(solver);
		this.init = checkNotNull(init);
		this.target = checkNotNull(target);
	}

	public static ExprTraceUnsatCoreChecker create(final Expr<? extends BoolType> init,
			final Expr<? extends BoolType> target, final Solver solver) {
		return new ExprTraceUnsatCoreChecker(init, target, solver);
	}

	@Override
	public ExprTraceStatus2<UnsatCoreRefutation> check(final Trace<? extends ExprState, ? extends ExprAction> trace) {
		checkNotNull(trace);
		final int stateCount = trace.getStates().size();

		final List<VarIndexing> indexings = new ArrayList<>(stateCount);
		indexings.add(VarIndexing.all(0));

		int satPrefix = -1;
		solver.push();
		solver.track(PathUtils.unfold(init, indexings.get(0)));
		for (int i = 0; i < stateCount; ++i) {
			solver.track(PathUtils.unfold(trace.getState(i).toExpr(), indexings.get(i)));
			if (i > 0) {
				solver.track(PathUtils.unfold(trace.getAction(i - 1).toExpr(), indexings.get(i - 1)));
			}

			if (solver.check().isSat()) {
				satPrefix = i;
			} else {
				break;
			}

			if (i < stateCount - 1) {
				indexings.add(indexings.get(i).add(trace.getAction(i).nextIndexing()));
			}
		}

		boolean concretizable;

		if (satPrefix == stateCount - 1) {
			solver.track(PathUtils.unfold(target, indexings.get(stateCount - 1)));
			concretizable = solver.check().isSat();
		} else {
			concretizable = false;
		}

		assert 0 <= satPrefix && satPrefix < stateCount;

		ExprTraceStatus2<UnsatCoreRefutation> status = null;

		if (concretizable) {
			final Model model = solver.getModel();
			final ImmutableList.Builder<Valuation> builder = ImmutableList.builder();
			for (final VarIndexing indexing : indexings) {
				builder.add(PathUtils.extractValuation(model, indexing));
			}
			status = ExprTraceStatus2.feasible(builder.build());
		} else {
			final Set<Expr<? extends BoolType>> uc = solver.getUnsatCore().stream().map(p -> PathUtils.foldin(p, 0))
					.collect(Collectors.toSet());
			status = ExprTraceStatus2.infeasible(UnsatCoreRefutation.create(uc, satPrefix));
		}

		solver.pop();

		assert status != null;
		return status;
	}

}