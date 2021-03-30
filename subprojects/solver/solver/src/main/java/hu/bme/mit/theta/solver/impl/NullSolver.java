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
package hu.bme.mit.theta.solver.impl;

import java.util.Collection;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverStatus;

public final class NullSolver implements Solver {

	private NullSolver() {
	}

	private static class LazyHolder {
		static final NullSolver INSTANCE = new NullSolver();
	}

	public static NullSolver getInstance() {
		return LazyHolder.INSTANCE;
	}

	@Override
	public void add(final Expr<BoolType> assertion) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SolverStatus check() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void push() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pop(final int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SolverStatus getStatus() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Valuation getModel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Expr<BoolType>> getAssertions() {
		throw new UnsupportedOperationException();
	}

}
