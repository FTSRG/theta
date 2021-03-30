package hu.bme.mit.theta.analysis.expr.refinement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprTraceUtils;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprSimplifier;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.SpState;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.core.utils.WpState;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.utils.WithPushPop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;
import static java.util.stream.Collectors.toList;

/**
 * An ExprTraceChecker that generates new predicates based on the UCB algorithm by
 * Leucker, Martin & Markin, Grigory & Neuhäußer, Martin. (2015). A New Refinement
 * Strategy for CEGAR-Based Industrial Model Checking. 155-170. 10.1007/978-3-319-26287-1_10.
 */
public class ExprTraceUCBChecker implements ExprTraceChecker<ItpRefutation>  {

    private final UCSolver solver;
    private final Expr<BoolType> init;
    private final Expr<BoolType> target;

    private ExprTraceUCBChecker(final Expr<BoolType> init, final Expr<BoolType> target, final UCSolver solver) {
        this.solver = checkNotNull(solver);
        this.init = checkNotNull(init);
        this.target = checkNotNull(target);
    }

    public static ExprTraceUCBChecker create(
        final Expr<BoolType> init, final Expr<BoolType> target, final UCSolver solver
    ) {
        return new ExprTraceUCBChecker(init, target, solver);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ExprTraceStatus<ItpRefutation> check(final Trace<? extends ExprState, ? extends ExprAction> trace) {
        checkNotNull(trace);
        try {
            return check2((Trace<? extends ExprState, ? extends StmtAction>) trace);
        }
        catch(ClassCastException e) {
            throw new UnsupportedOperationException("Actions must be of type StmtAction", e);
        }
    }

private ExprTraceStatus<ItpRefutation> check2(final Trace<? extends ExprState, ? extends StmtAction> trace) {
        final var ftrace = flattenTrace(trace);
        final int stateCount = trace.getStates().size();

        final List<VarIndexing> indexings = new ArrayList<>(stateCount);
        indexings.add(VarIndexing.all(0));

        final Valuation model;
        final Collection<Expr<BoolType>> unsatCore;
        final boolean concretizable;

        try (WithPushPop wpp = new WithPushPop(solver)) {
            for (int i = 1; i < stateCount; ++i) {
                indexings.add(indexings.get(i - 1).add(trace.getAction(i - 1).nextIndexing()));
                solver.track(
                    ExprUtils.getConjuncts(
                        PathUtils.unfold(trace.getAction(i - 1).toExpr(), indexings.get(i - 1))
                    )
                );
            }

            concretizable = solver.check().isSat();

            if (concretizable) {
                model = solver.getModel();
                unsatCore = null;
            } else {
                model = null;
                unsatCore = solver.getUnsatCore();
            }
        }

        if (concretizable) {
            checkNotNull(model);
            return createCounterexample(model, indexings, trace);
        } else {
            checkNotNull(unsatCore);
            return createRefinement(unsatCore, indexings, ftrace);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private ExprTraceStatus.Feasible<ItpRefutation> createCounterexample(
        final Valuation model,
        final List<VarIndexing> indexings,
        final Trace<? extends ExprState, ? extends ExprAction> trace
    ) {
        final ImmutableList.Builder<Valuation> builder = ImmutableList.builder();
        for (final VarIndexing indexing : indexings) {
            builder.add(PathUtils.extractValuation(model, indexing));
        }
        return ExprTraceStatus.feasible(Trace.of(builder.build(), trace.getActions()));
    }

    private ExprTraceStatus.Infeasible<ItpRefutation> createRefinement(
        final Collection<Expr<BoolType>> unsatCore,
        final List<VarIndexing> indexings,
        final Trace<? extends ExprState, ? extends StmtAction> trace
    ) {
        final int stateCount = trace.getStates().size();
        final List<Expr<BoolType>> wps = calculateWpStates(trace, indexings);

        final List<Expr<BoolType>> predicates = new ArrayList<>();

        var constCount = 0;
        for(var i = 0; i < stateCount; i++) {
            try(final var wpp = new WithPushPop(solver)) {
                final List<Expr<BoolType>> dataRegion = new ArrayList<>();

                /* Calculate SP */
                if (i == 0) {
                    solver.track(True());
                    dataRegion.add(True());
                } else /* i > 0 */ {
                    var spState = SpState.of(PathUtils.foldin(predicates.get(i - 1), indexings.get(i - 1)), constCount);
                    for(var stmt : trace.getAction(i - 1).getStmts()) {
                        spState = spState.sp(stmt);
                    }

                    final var expr = PathUtils.unfold(spState.getExpr(), indexings.get(i));
                    constCount = spState.getConstCount();
                    solver.track(ExprUtils.getConjuncts(expr));
                    dataRegion.addAll(ExprUtils.getConjuncts(expr));
                }

                /* Add wp */
                solver.track(ExprUtils.getConjuncts(wps.get(i)));

                solver.check();
                assert solver.check().isUnsat(); // It must be unsat
                Collection<Expr<BoolType>> uc = solver.getUnsatCore();

                /* Keep only those expressions from uc that are not in the data region */
                final Collection<Expr<BoolType>> predicate = new ArrayList<>();
                for (var ucExpr : uc) {
                    if (!dataRegion.contains(ucExpr)) {
                        predicate.add(ucExpr);
                    }
                }

                /* Add the negated of the above expression as the new predicate */
                predicates.add(
                    ExprSimplifier.simplify(
                        Not(And(new HashSet<>(predicate))),
                        ImmutableValuation.empty()
                    )
                );
            }
        }
        return ExprTraceStatus.infeasible(
            ItpRefutation.sequence(
                IntStream.range(0, predicates.size())
                    .mapToObj(i -> PathUtils.foldin(predicates.get(i), indexings.get(i)))
                    .collect(Collectors.toUnmodifiableList())
            )
        );
    }

    private List<Expr<BoolType>> calculateWpStates(
        final Trace<? extends ExprState, ? extends StmtAction> trace,
        final List<VarIndexing> indexings
    ) {
        final int stateCount = trace.getStates().size();
        final List<Expr<BoolType>> wps = new ArrayList<>(Collections.nCopies(stateCount, null));

        var wpstate = WpState.of(target);
        wps.set(stateCount - 1, target);
        for(var i = stateCount - 1; i > 0; i--) {
            var action = trace.getAction(i - 1);

            for(var stmt : Lists.reverse(action.getStmts())) {
                wpstate = wpstate.wep(stmt);
            }

            wps.set(i - 1, PathUtils.unfold(wpstate.getExpr(), indexings.get(i - 1)));
        }

        return wps;
    }

    private Trace<? extends ExprState, ? extends StmtAction> flattenTrace(final Trace<? extends ExprState, ? extends StmtAction> trace) {
        final var stateCount = trace.getStates().size();
        final var flattenedActions = new ArrayList<StmtAction>(stateCount - 1);

        for(var i = 1; i < stateCount; i++) {
            var initStream =
                (i == 1)
                ? ExprUtils.getConjuncts(init).stream().map(AssumeStmt::of)
                : Stream.<AssumeStmt>empty();

            var stateStream = ExprUtils.getConjuncts(trace.getState(i - 1).toExpr()).stream().map(AssumeStmt::of);

            var actionStream = trace.getAction(i - 1).getStmts().stream();

            var targetStream =
                (i == stateCount - 1)
                ? Stream.concat(
                    ExprUtils.getConjuncts(trace.getState(i).toExpr()).stream().map(AssumeStmt::of),
                    ExprUtils.getConjuncts(target).stream().map(AssumeStmt::of)
                )
                : Stream.<AssumeStmt>empty();

            flattenedActions.add(
                UCBAction.of(
                    Stream.of(initStream, stateStream, actionStream, targetStream).flatMap(e -> e).collect(toList())
                )
            );
        }

        return ExprTraceUtils.traceFrom(flattenedActions);
    }

    /*
     * Custom StmtAction to use when constructing helper traces
     */

    private static class UCBAction extends StmtAction {
        private final List<Stmt> stmts;

        private UCBAction(List<Stmt> stmts) {
            this.stmts = stmts;
        }

        public static UCBAction of(List<Stmt> stmts) {
            return new UCBAction(stmts);
        }

        @Override
        public List<Stmt> getStmts() {
            return stmts;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(getClass().getSimpleName()).body().addAll(stmts).toString();
        }
    }
}
