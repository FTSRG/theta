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

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static java.util.Collections.emptySet;

import java.util.function.Predicate;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.impl.PrecMappingAnalysis;
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredAnalysis;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.cfa.CFA.Loc;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaAnalysis;
import hu.bme.mit.theta.cfa.analysis.CfaPrec;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.cfa.analysis.prec.GlobalCfaPrec;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;

public final class PredImpactChecker implements SafetyChecker<CfaState<PredState>, CfaAction, UnitPrec> {

	private final ImpactChecker<CfaState<PredState>, CfaAction, UnitPrec> checker;

	private PredImpactChecker(final LTS<? super CfaState<PredState>, ? extends CfaAction> lts, final Loc initLoc,
							  final Predicate<? super Loc> targetLocs,
							  final Solver abstractionSolver, final ItpSolver refinementSolver) {
		checkNotNull(lts);
		checkNotNull(initLoc);
		checkNotNull(abstractionSolver);
		checkNotNull(refinementSolver);

		final Analysis<PredState, ExprAction, PredPrec> predAnalysis = PredAnalysis.create(abstractionSolver,
				PredAbstractors.booleanSplitAbstractor(abstractionSolver), True());

		final CfaPrec<PredPrec> fixedPrec = GlobalCfaPrec.create(PredPrec.of(emptySet()));

		final Analysis<CfaState<PredState>, CfaAction, CfaPrec<PredPrec>> cfaAnalysis = CfaAnalysis.create(initLoc,
				predAnalysis);

		final Analysis<CfaState<PredState>, CfaAction, UnitPrec> analysis = PrecMappingAnalysis.create(cfaAnalysis,
				np -> fixedPrec);

		final Predicate<CfaState<?>> target = s -> targetLocs.test(s.getLoc());

		final ArgBuilder<CfaState<PredState>, CfaAction, UnitPrec> argBuilder = ArgBuilder.create(lts, analysis,
				target);

		final ImpactRefiner<CfaState<PredState>, CfaAction> refiner = PredImpactRefiner.create(refinementSolver);

		checker = ImpactChecker.create(argBuilder, refiner, CfaState::getLoc);
	}

	public static PredImpactChecker create(final LTS<? super CfaState<PredState>, ? extends CfaAction> lts,
										   final Loc initLoc, final Predicate<? super Loc> targetLocs,
										   final Solver abstractionSolver, final ItpSolver refinementSolver) {
		return new PredImpactChecker(lts, initLoc, targetLocs, abstractionSolver, refinementSolver);
	}

	@Override
	public SafetyResult<CfaState<PredState>, CfaAction> check(final UnitPrec prec) {
		return checker.check(prec);
	}

}
