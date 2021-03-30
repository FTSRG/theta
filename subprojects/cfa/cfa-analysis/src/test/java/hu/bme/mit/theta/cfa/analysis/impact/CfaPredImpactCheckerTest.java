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
package hu.bme.mit.theta.cfa.analysis.impact;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import hu.bme.mit.theta.solver.Solver;
import org.junit.Test;

import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.lts.CfaLbeLts;
import hu.bme.mit.theta.cfa.dsl.CfaDslManager;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public final class CfaPredImpactCheckerTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		// Arrange
		final CFA cfa = CfaDslManager.createCfa(new FileInputStream("src/test/resources/counter5_true.cfa"));

		final Solver abstractionSolver = Z3SolverFactory.getInstance().createSolver();
		final ItpSolver refinementSolver = Z3SolverFactory.getInstance().createItpSolver();

		final PredImpactChecker checker = PredImpactChecker.create(CfaLbeLts.of(cfa.getErrorLoc().get()), cfa.getInitLoc(),
				l -> l.equals(cfa.getErrorLoc().get()), abstractionSolver, refinementSolver);

		// Act
		final SafetyResult<? extends ExprState, ? extends ExprAction> status = checker.check(UnitPrec.getInstance());

		// Assert
		assertTrue(status.isSafe());

		final ARG<? extends ExprState, ? extends ExprAction> arg = status.getArg();
		arg.minimize();

		final ArgChecker argChecker = ArgChecker.create(abstractionSolver);
		assertTrue(argChecker.isWellLabeled(arg));

		System.out.println(GraphvizWriter.getInstance().writeString(ArgVisualizer.getDefault().visualize(arg)));
	}
}
