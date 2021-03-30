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
package hu.bme.mit.theta.analysis.expr;

import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Prime;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Add;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Eq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Geq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import hu.bme.mit.theta.solver.UCSolver;
import org.junit.Before;
import org.junit.Test;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceBwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceFwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceSeqItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceStatus;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceUnsatCoreChecker;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public final class ExprTraceCheckersTest {
	private Collection<ExprTraceChecker<?>> traceCheckers;

	@Before
	public void before() {
		final ItpSolver itpSolver = Z3SolverFactory.getInstance().createItpSolver();
		final UCSolver ucSolver = Z3SolverFactory.getInstance().createUCSolver();
		traceCheckers = new ArrayList<>();
		traceCheckers.add(ExprTraceSeqItpChecker.create(True(), True(), itpSolver));
		traceCheckers.add(ExprTraceFwBinItpChecker.create(True(), True(), itpSolver));
		traceCheckers.add(ExprTraceBwBinItpChecker.create(True(), True(), itpSolver));
		traceCheckers.add(ExprTraceUnsatCoreChecker.create(True(), True(), ucSolver));
	}

	@Test
	public void testFeasable() {
		// Arrange
		final Expr<IntType> x = Var("x", Int()).getRef();
		final Expr<BoolType> trans = Eq(Prime(x), Add(x, Int(1)));

		final ExprAction actionMock = mock(ExprAction.class);
		doReturn(trans).when(actionMock).toExpr();
		when(actionMock.nextIndexing()).thenReturn(VarIndexing.all(1));

		final List<ExprAction> actions = Arrays.asList(actionMock, actionMock, actionMock);
		final Trace<ExprState, ExprAction> trace = ExprTraceUtils.traceFrom(actions);

		for (final ExprTraceChecker<?> checker : traceCheckers) {
			// Act
			final ExprTraceStatus<?> status = checker.check(trace);
			// Assert
			assertTrue(status.isFeasible());
			assertFalse(status.isInfeasible());
		}
	}

	@Test
	public void testInfeasable() {
		// Arrange
		final Expr<IntType> x = Var("x", Int()).getRef();
		final Expr<BoolType> trans1 = Eq(Prime(x), Int(0));
		final Expr<BoolType> trans2 = Geq(x, Int(1));

		final ExprAction action1Mock = mock(ExprAction.class);
		doReturn(trans1).when(action1Mock).toExpr();
		when(action1Mock.nextIndexing()).thenReturn(VarIndexing.all(1));

		final ExprAction action2Mock = mock(ExprAction.class);
		doReturn(trans2).when(action2Mock).toExpr();
		when(action2Mock.nextIndexing()).thenReturn(VarIndexing.all(0));

		final List<ExprAction> actions = Arrays.asList(action1Mock, action2Mock);
		final Trace<ExprState, ExprAction> trace = ExprTraceUtils.traceFrom(actions);

		for (final ExprTraceChecker<?> checker : traceCheckers) {
			// Act
			final ExprTraceStatus<?> status = checker.check(trace);
			// Assert
			assertTrue(status.isInfeasible());
			assertFalse(status.isFeasible());
		}
	}

}
