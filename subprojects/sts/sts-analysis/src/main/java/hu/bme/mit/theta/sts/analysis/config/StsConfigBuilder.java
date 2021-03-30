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
package hu.bme.mit.theta.sts.analysis.config;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgNodeComparators;
import hu.bme.mit.theta.analysis.algorithm.ArgNodeComparators.ArgNodeComparator;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.abstractor.StopCriterions;
import hu.bme.mit.theta.analysis.expl.ExplAnalysis;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStatePredicate;
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec;
import hu.bme.mit.theta.analysis.expl.VarsRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprStatePredicate;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceBwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceFwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceSeqItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceUnsatCoreChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.JoiningPrecRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.MultiExprTraceRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.SingleExprTraceRefiner;
import hu.bme.mit.theta.analysis.pred.ExprSplitters;
import hu.bme.mit.theta.analysis.pred.ExprSplitters.ExprSplitter;
import hu.bme.mit.theta.analysis.pred.ItpRefToPredPrec;
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredAbstractors.PredAbstractor;
import hu.bme.mit.theta.analysis.pred.PredAnalysis;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.waitlist.PriorityWaitlist;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.analysis.StsAction;
import hu.bme.mit.theta.sts.analysis.StsLts;
import hu.bme.mit.theta.sts.analysis.initprec.StsEmptyInitPrec;
import hu.bme.mit.theta.sts.analysis.initprec.StsInitPrec;
import hu.bme.mit.theta.sts.analysis.initprec.StsPropInitPrec;

import java.util.function.Predicate;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;

public final class StsConfigBuilder {

	public enum Domain {
		EXPL, PRED_BOOL, PRED_CART, PRED_SPLIT
	}

	public enum Refinement {
		FW_BIN_ITP, BW_BIN_ITP, SEQ_ITP, MULTI_SEQ, UNSAT_CORE
	}

	public enum Search {
		BFS(ArgNodeComparators.combine(ArgNodeComparators.targetFirst(), ArgNodeComparators.bfs())),

		DFS(ArgNodeComparators.combine(ArgNodeComparators.targetFirst(), ArgNodeComparators.dfs()));

		public final ArgNodeComparator comparator;

		private Search(final ArgNodeComparator comparator) {
			this.comparator = comparator;
		}

	}

	public enum PredSplit {
		WHOLE(ExprSplitters.whole()),

		CONJUNCTS(ExprSplitters.conjuncts()),

		ATOMS(ExprSplitters.atoms());

		public final ExprSplitter splitter;

		private PredSplit(final ExprSplitter splitter) {
			this.splitter = splitter;
		}
	}

	public enum InitPrec {
		EMPTY(new StsEmptyInitPrec()), PROP(new StsPropInitPrec());

		public final StsInitPrec builder;

		private InitPrec(final StsInitPrec builder) {
			this.builder = builder;
		}

	}

	private Logger logger = NullLogger.getInstance();
	private final SolverFactory solverFactory;
	private final Domain domain;
	private final Refinement refinement;
	private Search search = Search.BFS;
	private PredSplit predSplit = PredSplit.WHOLE;
	private InitPrec initPrec = InitPrec.EMPTY;
	private PruneStrategy pruneStrategy = PruneStrategy.LAZY;

	public StsConfigBuilder(final Domain domain, final Refinement refinement, final SolverFactory solverFactory) {
		this.domain = domain;
		this.refinement = refinement;
		this.solverFactory = solverFactory;
	}

	public StsConfigBuilder logger(final Logger logger) {
		this.logger = logger;
		return this;
	}

	public StsConfigBuilder search(final Search search) {
		this.search = search;
		return this;
	}

	public StsConfigBuilder predSplit(final PredSplit predSplit) {
		this.predSplit = predSplit;
		return this;
	}

	public StsConfigBuilder initPrec(final InitPrec initPrec) {
		this.initPrec = initPrec;
		return this;
	}

	public StsConfigBuilder pruneStrategy(final PruneStrategy pruneStrategy) {
		this.pruneStrategy = pruneStrategy;
		return this;
	}

	public StsConfig<? extends State, ? extends Action, ? extends Prec> build(final STS sts) {
		final LTS<State, StsAction> lts = StsLts.create(sts);
		final Expr<BoolType> init = sts.getInit();
		final Expr<BoolType> negProp = Not(sts.getProp());

		if (domain == Domain.EXPL) {
			final Solver analysisSolver = solverFactory.createSolver();
			final Predicate<ExplState> target = new ExplStatePredicate(negProp, analysisSolver);
			final Analysis<ExplState, ExprAction, ExplPrec> analysis = ExplAnalysis.create(analysisSolver, init);
			final ArgBuilder<ExplState, StsAction, ExplPrec> argBuilder = ArgBuilder.create(lts, analysis, target,
					true);
			final Abstractor<ExplState, StsAction, ExplPrec> abstractor = BasicAbstractor.builder(argBuilder)
					.waitlist(PriorityWaitlist.create(search.comparator))
					.stopCriterion(refinement == Refinement.MULTI_SEQ ? StopCriterions.fullExploration()
							: StopCriterions.firstCex())
					.logger(logger).build();

			Refiner<ExplState, StsAction, ExplPrec> refiner = null;

			switch (refinement) {
				case FW_BIN_ITP:
					refiner = SingleExprTraceRefiner.create(ExprTraceFwBinItpChecker.create(init, negProp, solverFactory.createItpSolver()),
							JoiningPrecRefiner.create(new ItpRefToExplPrec()), pruneStrategy, logger);
					break;
				case BW_BIN_ITP:
					refiner = SingleExprTraceRefiner.create(ExprTraceBwBinItpChecker.create(init, negProp, solverFactory.createItpSolver()),
							JoiningPrecRefiner.create(new ItpRefToExplPrec()), pruneStrategy, logger);
					break;
				case SEQ_ITP:
					refiner = SingleExprTraceRefiner.create(ExprTraceSeqItpChecker.create(init, negProp, solverFactory.createItpSolver()),
							JoiningPrecRefiner.create(new ItpRefToExplPrec()), pruneStrategy, logger);
					break;
				case MULTI_SEQ:
					refiner = MultiExprTraceRefiner.create(ExprTraceSeqItpChecker.create(init, negProp, solverFactory.createItpSolver()),
							JoiningPrecRefiner.create(new ItpRefToExplPrec()), pruneStrategy, logger);
					break;
				case UNSAT_CORE:
					refiner = SingleExprTraceRefiner.create(ExprTraceUnsatCoreChecker.create(init, negProp, solverFactory.createUCSolver()),
							JoiningPrecRefiner.create(new VarsRefToExplPrec()), pruneStrategy, logger);
					break;
				default:
					throw new UnsupportedOperationException(
							domain + " domain does not support " + refinement + " refinement.");
			}

			final SafetyChecker<ExplState, StsAction, ExplPrec> checker = CegarChecker.create(abstractor, refiner,
					logger);
			final ExplPrec prec = initPrec.builder.createExpl(sts);
			return StsConfig.create(checker, prec);

		} else if (domain == Domain.PRED_BOOL || domain == Domain.PRED_CART || domain == Domain.PRED_SPLIT) {
			final Solver analysisSolver = solverFactory.createSolver();
			PredAbstractor predAbstractor = null;
			switch (domain) {
				case PRED_BOOL:
					predAbstractor = PredAbstractors.booleanAbstractor(analysisSolver);
					break;
				case PRED_SPLIT:
					predAbstractor = PredAbstractors.booleanSplitAbstractor(analysisSolver);
					break;
				case PRED_CART:
					predAbstractor = PredAbstractors.cartesianAbstractor(analysisSolver);
					break;
				default:
					throw new UnsupportedOperationException(domain + " domain is not supported.");
			}
			final Predicate<ExprState> target = new ExprStatePredicate(negProp, analysisSolver);
			final Analysis<PredState, ExprAction, PredPrec> analysis = PredAnalysis.create(analysisSolver, predAbstractor,
					init);
			final ArgBuilder<PredState, StsAction, PredPrec> argBuilder = ArgBuilder.create(lts, analysis, target,
					true);
			final Abstractor<PredState, StsAction, PredPrec> abstractor = BasicAbstractor.builder(argBuilder)
					.waitlist(PriorityWaitlist.create(search.comparator))
					.stopCriterion(refinement == Refinement.MULTI_SEQ ? StopCriterions.fullExploration()
							: StopCriterions.firstCex())
					.logger(logger).build();

			ExprTraceChecker<ItpRefutation> exprTraceChecker = null;
			switch (refinement) {
				case FW_BIN_ITP:
					exprTraceChecker = ExprTraceFwBinItpChecker.create(init, negProp, solverFactory.createItpSolver());
					break;
				case BW_BIN_ITP:
					exprTraceChecker = ExprTraceBwBinItpChecker.create(init, negProp, solverFactory.createItpSolver());
					break;
				case SEQ_ITP:
					exprTraceChecker = ExprTraceSeqItpChecker.create(init, negProp, solverFactory.createItpSolver());
					break;
				case MULTI_SEQ:
					exprTraceChecker = ExprTraceSeqItpChecker.create(init, negProp, solverFactory.createItpSolver());
					break;
				default:
					throw new UnsupportedOperationException(
							domain + " domain does not support " + refinement + " refinement.");
			}
			Refiner<PredState, StsAction, PredPrec> refiner;
			if (refinement == Refinement.MULTI_SEQ) {
				refiner = MultiExprTraceRefiner.create(exprTraceChecker,
						JoiningPrecRefiner.create(new ItpRefToPredPrec(predSplit.splitter)), pruneStrategy, logger);
			} else {
				refiner = SingleExprTraceRefiner.create(exprTraceChecker,
						JoiningPrecRefiner.create(new ItpRefToPredPrec(predSplit.splitter)), pruneStrategy, logger);
			}

			final SafetyChecker<PredState, StsAction, PredPrec> checker = CegarChecker.create(abstractor, refiner,
					logger);

			final PredPrec prec = initPrec.builder.createPred(sts);
			return StsConfig.create(checker, prec);
		} else {
			throw new UnsupportedOperationException(domain + " domain is not supported.");
		}
	}
}
