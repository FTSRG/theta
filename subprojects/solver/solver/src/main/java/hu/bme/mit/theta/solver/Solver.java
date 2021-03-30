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
package hu.bme.mit.theta.solver;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;

/**
 * Common interface for SMT solvers.
 *
 * Use the {@link #add(Expr)} methods to add expressions to the solver.
 * Then use {@link #check()} method to check their satisfiability. The result can be queried by
 * {@link #getStatus()}. If the expressions are satisfiable, a satisfying assignment can be
 * obtained by {@link #getModel()}.
 *
 * The solver can also support incremental solving by {@link #push()} and {@link #pop()}.
 */
public interface Solver extends SolverBase {

	/**
	 * Add an expression to the solver.
	 *
	 * @param assertion Expression to be added
	 */
	void add(Expr<BoolType> assertion);

	/**
	 * Add a collection of expressions to the solver.
	 *
	 * @param assertions Expressions to be added
	 */
	default void add(final Iterable<? extends Expr<BoolType>> assertions) {
		for (final Expr<BoolType> assertion : assertions) {
			add(assertion);
		}
	}
}
