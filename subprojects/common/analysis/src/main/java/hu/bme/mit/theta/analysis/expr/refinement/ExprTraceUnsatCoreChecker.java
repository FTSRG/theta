/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.analysis.expr.refinement;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.IndexedVars;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.utils.WithPushPop;

/**
 * An ExprTraceChecker that generates an unsat core by checking the trace at
 * once.
 */
public final class ExprTraceUnsatCoreChecker implements ExprTraceChecker<VarsRefutation> {

	private final UCSolver solver;
	private final Expr<BoolType> init;
	private final Expr<BoolType> target;

	private ExprTraceUnsatCoreChecker(final Expr<BoolType> init, final Expr<BoolType> target, final UCSolver solver) {
		this.solver = checkNotNull(solver);
		this.init = checkNotNull(init);
		this.target = checkNotNull(target);
	}

	public static ExprTraceUnsatCoreChecker create(final Expr<BoolType> init, final Expr<BoolType> target,
												   final UCSolver solver) {
		return new ExprTraceUnsatCoreChecker(init, target, solver);
	}

	@Override
	public ExprTraceStatus<VarsRefutation> check(final Trace<? extends ExprState, ? extends ExprAction> trace) {
		checkNotNull(trace);
		final int stateCount = trace.getStates().size();

		final List<VarIndexing> indexings = new ArrayList<>(stateCount);
		indexings.add(VarIndexing.all(0));

		try (WithPushPop wpp = new WithPushPop(solver)) {
			solver.track(ExprUtils.getConjuncts(PathUtils.unfold(init, indexings.get(0))));
			solver.track(ExprUtils.getConjuncts(PathUtils.unfold(trace.getState(0).toExpr(), indexings.get(0))));
			assert solver.check().isSat() : "Initial state of the trace is not feasible";
			boolean concretizable = true;

			for (int i = 1; i < stateCount; ++i) {
				indexings.add(indexings.get(i - 1).add(trace.getAction(i - 1).nextIndexing()));
				solver.track(ExprUtils.getConjuncts(PathUtils.unfold(trace.getState(i).toExpr(), indexings.get(i))));
				solver.track(ExprUtils
						.getConjuncts(PathUtils.unfold(trace.getAction(i - 1).toExpr(), indexings.get(i - 1))));

				if (!solver.check().isSat()) {
					concretizable = false;
					break;
				}
			}

			if (concretizable) {
				solver.track(ExprUtils.getConjuncts(PathUtils.unfold(target, indexings.get(stateCount - 1))));
				concretizable = solver.check().isSat();
			}

			if (concretizable) {
				final Valuation model = solver.getModel();
				final ImmutableList.Builder<Valuation> builder = ImmutableList.builder();
				for (final VarIndexing indexing : indexings) {
					builder.add(PathUtils.extractValuation(model, indexing));
				}
				return ExprTraceStatus.feasible(Trace.of(builder.build(), trace.getActions()));
			} else {
				final Collection<Expr<BoolType>> unsatCore = solver.getUnsatCore();
				final IndexedVars indexedVars = ExprUtils.getVarsIndexed(unsatCore);
				return ExprTraceStatus.infeasible(VarsRefutation.create(indexedVars));
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
