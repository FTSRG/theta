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
package hu.bme.mit.theta.sts.analysis;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgNodeComparators;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.expl.ExplAnalysis;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.VarsRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprStatePredicate;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceUnsatCoreChecker;
import hu.bme.mit.theta.analysis.expr.refinement.JoiningPrecRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.SingleExprTraceRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.VarsRefutation;
import hu.bme.mit.theta.analysis.waitlist.PriorityWaitlist;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.Logger.Level;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.STS.Builder;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Predicate;

import static hu.bme.mit.theta.analysis.algorithm.ArgUtils.isWellLabeled;
import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Prime;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Imply;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Add;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Eq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Geq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Lt;
import static org.junit.Assert.assertTrue;

public class StsExplTest {

	@Test
	public void test() {

		final Logger logger = new ConsoleLogger(Level.VERBOSE);

		final VarDecl<IntType> vx = Var("x", Int());
		final Expr<IntType> x = vx.getRef();
		final VarDecl<IntType> vy = Var("y", Int());
		final Expr<IntType> y = vy.getRef();

		final int mod = 10;

		final Builder builder = STS.builder();

		builder.addInit(Eq(x, Int(0)));
		builder.addInit(Eq(y, Int(0)));
		builder.addTrans(And(Imply(Lt(x, Int(mod)), Eq(Prime(x), Add(x, Int(1)))),
				Imply(Geq(x, Int(mod)), Eq(Prime(x), Int(0)))));
		builder.addTrans(Eq(Prime(y), Int(0)));
		builder.setProp(Not(Eq(x, Int(mod))));

		final STS sts = builder.build();

		final Solver abstractionSolver = Z3SolverFactory.getInstance().createSolver();
		final UCSolver refinementSolver = Z3SolverFactory.getInstance().createUCSolver();

		final Analysis<ExplState, ExprAction, ExplPrec> analysis = ExplAnalysis.create(abstractionSolver, sts.getInit());
		final Predicate<ExprState> target = new ExprStatePredicate(Not(sts.getProp()), abstractionSolver);

		final ExplPrec prec = ExplPrec.of(Collections.singleton(vy));

		final LTS<State, StsAction> lts = StsLts.create(sts);

		final ArgBuilder<ExplState, StsAction, ExplPrec> argBuilder = ArgBuilder.create(lts, analysis, target);

		final Abstractor<ExplState, StsAction, ExplPrec> abstractor = BasicAbstractor.builder(argBuilder)
				.waitlist(PriorityWaitlist.create(ArgNodeComparators.bfs())).logger(logger).build();

		final ExprTraceChecker<VarsRefutation> exprTraceChecker = ExprTraceUnsatCoreChecker.create(sts.getInit(),
				Not(sts.getProp()), refinementSolver);

		final SingleExprTraceRefiner<ExplState, StsAction, ExplPrec, VarsRefutation> refiner = SingleExprTraceRefiner
				.create(exprTraceChecker, JoiningPrecRefiner.create(new VarsRefToExplPrec()), PruneStrategy.LAZY, logger);

		final SafetyChecker<ExplState, StsAction, ExplPrec> checker = CegarChecker.create(abstractor, refiner, logger);

		final SafetyResult<ExplState, StsAction> safetyStatus = checker.check(prec);

		final ARG<ExplState, StsAction> arg = safetyStatus.getArg();
		assertTrue(isWellLabeled(arg, abstractionSolver));

		// System.out.println(new
		// GraphvizWriter().writeString(ArgVisualizer.visualize(arg)));
	}

}
