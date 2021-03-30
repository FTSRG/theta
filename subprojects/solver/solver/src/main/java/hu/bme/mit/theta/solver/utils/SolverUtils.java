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
package hu.bme.mit.theta.solver.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;

public final class SolverUtils {

	private SolverUtils() {
	}

	public static boolean entails(final Solver solver, final Expr<BoolType> antecedent,
								  final Expr<BoolType> consequent) {
		checkNotNull(solver);
		checkNotNull(antecedent);
		checkNotNull(consequent);
		try (WithPushPop wpp = new WithPushPop(solver)) {
			solver.add(antecedent);
			solver.add(Not(consequent));
			return solver.check().isUnsat();
		}
	}

	public static boolean entails(final Solver solver, final Iterable<? extends Expr<BoolType>> antecedents,
								  final Iterable<? extends Expr<BoolType>> consequents) {
		checkNotNull(solver);
		checkNotNull(antecedents);
		checkNotNull(consequents);
		try (WithPushPop wpp = new WithPushPop(solver)) {
			antecedents.forEach(antecedent -> solver.add(antecedent));
			consequents.forEach(consequent -> solver.add(Not(consequent)));
			return solver.check().isUnsat();
		}
	}

	public static Stream<Valuation> models(final SolverFactory factory, final Expr<BoolType> expr) {
		return models(factory, expr, m -> Not(m.toExpr()));
	}

	public static Stream<Valuation> models(final SolverFactory factory, final Expr<BoolType> expr,
										   final Function<? super Valuation, ? extends Expr<BoolType>> feedback) {
		final Iterable<Valuation> iterable = () -> new ModelIterator(factory, expr, feedback);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	private static final class ModelIterator implements Iterator<Valuation> {
		private final Solver solver;
		private final Function<? super Valuation, ? extends Expr<BoolType>> feedback;

		private ModelIterator(final SolverFactory factory, final Expr<BoolType> expr,
							  final Function<? super Valuation, ? extends Expr<BoolType>> feedback) {
			checkNotNull(expr);
			checkNotNull(factory);
			this.feedback = checkNotNull(feedback);

			solver = factory.createSolver();
			solver.add(expr);
		}

		@Override
		public boolean hasNext() {
			return solver.check().isSat();
		}

		@Override
		public Valuation next() {
			if (!hasNext()) throw new NoSuchElementException("Formula is UNSAT");
			final Valuation model = solver.getModel();
			solver.add(feedback.apply(model));
			return model;
		}
	}

}
