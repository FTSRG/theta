package hu.bme.mit.theta.analysis.expr.refinement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprTraceUtils;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.BasicSubstitution;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprSimplifier;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.SpState;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.core.utils.WpState;
import hu.bme.mit.theta.solver.Solver;
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
import static hu.bme.mit.theta.core.decl.Decls.Param;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Exists;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Forall;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * An ExprTraceChecker that generates new predicates based on the Newton-style algorithms described in
 * Daniel Dietsch, Matthias Heizmann, Betim Musa, Alexander Nutz, and Andreas Podelski. 2017.
 * Craig vs. Newton in software model checking. In <i>Proceedings of the 2017 11th Joint Meeting on Foundations
 * of Software Engineering</i> (<i>ESEC/FSE 2017</i>). Association for Computing Machinery, New York, NY, USA,
 * 487–497. DOI:https://doi.org/10.1145/3106237.3106307
 */
public class ExprTraceNewtonChecker implements ExprTraceChecker<ItpRefutation> {
    private enum AssertionGeneratorMethod { SP, WP }

    private final UCSolver solver;
    private final Expr<BoolType> init;
    private final Expr<BoolType> target;

    private final boolean IT; // Whether to abstract the trace or not
    private final AssertionGeneratorMethod SPorWP; // Whether to use pre- or postconditions
    private final boolean LV; // Whether to project the assertions to live variables

    private ExprTraceNewtonChecker(
        final Expr<BoolType> init, final Expr<BoolType> target, final UCSolver solver,
        boolean it, AssertionGeneratorMethod sPorWP, boolean lv
    ) {
        this.solver = checkNotNull(solver);
        this.init = checkNotNull(init);
        this.target = checkNotNull(target);
        this.IT = it;
        this.SPorWP = checkNotNull(sPorWP);
        this.LV = lv;
    }

    public static ExprTraceNewtonCheckerITBuilder create(
        final Expr<BoolType> init, final Expr<BoolType> target, final UCSolver solver
    ) {
        return new ExprTraceNewtonCheckerITBuilder(solver, init, target);
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
        var ftrace = flattenTrace(trace); // Moves the expressions in the states to the corresponting actions as assumptions

        final int stateCount = ftrace.getStates().size();
        final List<VarIndexing> indexings = new ArrayList<>(stateCount);
        indexings.add(VarIndexing.all(0));

        final Valuation model;
        final Collection<Expr<BoolType>> unsatCore;
        final boolean concretizable;

        try (WithPushPop wpp = new WithPushPop(solver)) {
            for (int i = 1; i < stateCount; ++i) {
                var curIndexing = indexings.get(i - 1);
                for(var stmt : ftrace.getAction(i - 1).getStmts()) {
                    var stmtUnfoldResult = StmtUtils.toExpr(stmt, VarIndexing.all(0));
                    solver.track(PathUtils.unfold(stmtUnfoldResult.getExprs().iterator().next(), curIndexing));
                    curIndexing = curIndexing.add(stmtUnfoldResult.getIndexing());
                }
                indexings.add(curIndexing);
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
                NewtonAction.of(
                    Stream.of(initStream, stateStream, actionStream, targetStream).flatMap(e -> e).collect(toList())
                )
            );
        }

        return ExprTraceUtils.traceFrom(flattenedActions);
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
        Trace<? extends ExprState, ? extends StmtAction> trace
    ) {
        if(IT) {
            trace = computeAbstractTrace(unsatCore, trace);
        }

        final List<Expr<BoolType>> assertions;
        if(SPorWP == AssertionGeneratorMethod.SP) {
            assertions = computeAssertionsFromTraceWithStrongestPostcondition(trace);
        }
        else if(SPorWP == AssertionGeneratorMethod.WP) {
            assertions = computeAssertionsFromTraceWithWeakestPrecondition(trace);
        }
        else {
            throw new AssertionError("There should be no other option");
        }

        return ExprTraceStatus.infeasible(ItpRefutation.sequence(assertions));
    }

    /*
     * State abstraction
     */

    private Trace<? extends ExprState, ? extends StmtAction> computeAbstractTrace(
        final Collection<Expr<BoolType>> unsatCore,
        final Trace<? extends ExprState, ? extends StmtAction> trace
    ) {
        final var stateCount = trace.getStates().size();
        var curIndexing = VarIndexing.all(0);

        final var actions = new ArrayList<NewtonAction>();

        for (int i = 1; i < stateCount; ++i) {
            final var stmts = new ArrayList<Stmt>();
            for(final var stmt : trace.getAction(i - 1).getStmts()) {
                final var stmtUnfoldResult = StmtUtils.toExpr(stmt, VarIndexing.all(0));
                final var stmtExpr = PathUtils.unfold(stmtUnfoldResult.getExprs().iterator().next(), curIndexing);

                if(unsatCore.contains(stmtExpr)) {
                    stmts.add(stmt);
                }
                else {
                    stmts.add(computeAbstractStmt(stmt));
                }

                curIndexing = curIndexing.add(stmtUnfoldResult.getIndexing());
            }
            actions.add(NewtonAction.of(stmts));
        }

        return Trace.of(trace.getStates(), actions);
    }

    private Stmt computeAbstractStmt(Stmt stmt) {
        return stmt.accept(new StmtVisitor<Void, Stmt>() {
            @Override
            public Stmt visit(SkipStmt stmt, Void param) {
                return SkipStmt.getInstance();
            }

            @Override
            public Stmt visit(AssumeStmt stmt, Void param) {
                return AssumeStmt.of(True());
            }

            @Override
            public <DeclType extends Type> Stmt visit(AssignStmt<DeclType> stmt, Void param) {
                return HavocStmt.of(stmt.getVarDecl());
            }

            @Override
            public <DeclType extends Type> Stmt visit(HavocStmt<DeclType> stmt, Void param) {
                return HavocStmt.of(stmt.getVarDecl());
            }

            @Override
            public Stmt visit(SequenceStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Stmt visit(NonDetStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Stmt visit(OrtStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }
        }, null);
    }

    /*
     * Assertion computation
     */

    private List<Expr<BoolType>> computeAssertionsFromTraceWithStrongestPostcondition(
        final Trace<? extends ExprState, ? extends StmtAction> trace
    ) {
        final int stateCount = trace.getStates().size();
        final List<Expr<BoolType>> assertions = new ArrayList<>(stateCount);

        assertions.add(True());
        var constCount = 0;
        for(var i = 1; i < stateCount; i++) {
            var spState = SpState.of(assertions.get(i - 1), constCount);
            for(var stmt : trace.getAction(i - 1).getStmts()) {
                spState = spState.sp(stmt);
            }
            assertions.add(ExprSimplifier.simplify(spState.getExpr(), ImmutableValuation.empty()));
            constCount = spState.getConstCount();
        }

        if(LV) {
            var allVariables = collectVariablesInTrace(trace);
            var futureLiveVariables = collectFutureLiveVariablesForTrace(trace);
            return IntStream.range(0, assertions.size())
                .mapToObj(i -> existentialProjection(assertions.get(i), futureLiveVariables.get(i), allVariables))
                .collect(Collectors.toUnmodifiableList());
        }
        else {
            return assertions;
        }
    }

    private List<Expr<BoolType>> computeAssertionsFromTraceWithWeakestPrecondition(
        final Trace<? extends ExprState, ? extends StmtAction> trace
    ) {
        final int stateCount = trace.getStates().size();
        final List<Expr<BoolType>> assertions = new ArrayList<>(Collections.nCopies(stateCount, null));

        assertions.set(stateCount - 1, True());
        var constCount = 0;
        for(var i = stateCount - 2; i >= 0; i--) {
            var wpState = WpState.of(assertions.get(i + 1), constCount);
            for(var stmt : Lists.reverse(trace.getAction(i).getStmts())) {
                wpState = wpState.wep(stmt);
            }
            assertions.set(i, ExprSimplifier.simplify(wpState.getExpr(), ImmutableValuation.empty()));
            constCount = wpState.getConstCount();
        }

        if(LV) {
            var allVariables = collectVariablesInTrace(trace);
            var pastLiveVariables = collectPastLiveVariablesForTrace(trace);
            return IntStream.range(0, assertions.size())
                .mapToObj(i -> universalProjection(assertions.get(i), pastLiveVariables.get(i), allVariables))
                .collect(Collectors.toUnmodifiableList());
        }
        else {
            return assertions;
        }
    }

    /*
     * Live variable collection
     */

    private Collection<VarDecl<?>> collectVariablesInTrace(final Trace<? extends ExprState, ? extends StmtAction> trace) {
        var variables = new HashSet<VarDecl<?>>();

        for(var state : trace.getStates()) {
            ExprUtils.collectVars(state.toExpr(), variables);
        }
        for(var action : trace.getActions()) {
            ExprUtils.collectVars(action.toExpr(), variables);
        }

        return variables;
    }

    private Collection<VarDecl<?>> stmtReadsVariables(final Stmt stmt) {
        return stmt.accept(new StmtVisitor<Void, Collection<VarDecl<?>>>() {
            @Override
            public Collection<VarDecl<?>> visit(SkipStmt stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public Collection<VarDecl<?>> visit(AssumeStmt stmt, Void param) {
                return ExprUtils.getVars(stmt.getCond());
            }

            @Override
            public <DeclType extends Type> Collection<VarDecl<?>> visit(AssignStmt<DeclType> stmt, Void param) {
                return ExprUtils.getVars(stmt.getExpr());
            }

            @Override
            public <DeclType extends Type> Collection<VarDecl<?>> visit(HavocStmt<DeclType> stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public Collection<VarDecl<?>> visit(SequenceStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<VarDecl<?>> visit(NonDetStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<VarDecl<?>> visit(OrtStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }
        }, null);
    }

    private Collection<VarDecl<?>> stmtWritesVariables(final Stmt stmt) {
        return stmt.accept(new StmtVisitor<Void, Collection<VarDecl<?>>>() {
            @Override
            public Collection<VarDecl<?>> visit(SkipStmt stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public Collection<VarDecl<?>> visit(AssumeStmt stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public <DeclType extends Type> Collection<VarDecl<?>> visit(AssignStmt<DeclType> stmt, Void param) {
                return Collections.singletonList(stmt.getVarDecl());
            }

            @Override
            public <DeclType extends Type> Collection<VarDecl<?>> visit(HavocStmt<DeclType> stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public Collection<VarDecl<?>> visit(SequenceStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<VarDecl<?>> visit(NonDetStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<VarDecl<?>> visit(OrtStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }
        }, null);
    }

    private Collection<VarDecl<?>> stmtHavocsVariables(final Stmt stmt) {
        return stmt.accept(new StmtVisitor<Void, Collection<VarDecl<?>>>() {
            @Override
            public Collection<VarDecl<?>> visit(SkipStmt stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public Collection<VarDecl<?>> visit(AssumeStmt stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public <DeclType extends Type> Collection<VarDecl<?>> visit(AssignStmt<DeclType> stmt, Void param) {
                return Collections.emptySet();
            }

            @Override
            public <DeclType extends Type> Collection<VarDecl<?>> visit(HavocStmt<DeclType> stmt, Void param) {
                return Collections.singletonList(stmt.getVarDecl());
            }

            @Override
            public Collection<VarDecl<?>> visit(SequenceStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<VarDecl<?>> visit(NonDetStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<VarDecl<?>> visit(OrtStmt stmt, Void param) {
                throw new UnsupportedOperationException();
            }
        }, null);
    }

    private Collection<VarDecl<?>> actionReadsVariables(final StmtAction action) {
        return action.getStmts().stream().flatMap(stmt -> stmtReadsVariables(stmt).stream()).collect(Collectors.toUnmodifiableSet());
    }

    private Collection<VarDecl<?>> actionWritesVariables(final StmtAction action) {
        return action.getStmts().stream().flatMap(stmt -> stmtWritesVariables(stmt).stream()).collect(Collectors.toUnmodifiableSet());
    }

    private Collection<VarDecl<?>> actionHavocsVariables(final StmtAction action) {
        return action.getStmts().stream().flatMap(stmt -> stmtHavocsVariables(stmt).stream()).collect(Collectors.toUnmodifiableSet());
    }

    private List<Collection<VarDecl<?>>> collectFutureLiveVariablesForTrace(final Trace<? extends ExprState, ? extends StmtAction> trace) {
        final var stateCount = trace.getStates().size();
        final var futureLiveVariables = new ArrayList<Collection<VarDecl<?>>>(Collections.nCopies(stateCount, null));

        futureLiveVariables.set(stateCount - 1, Collections.emptySet());
        for(var i = stateCount - 2; i >= 0; i--) {
            var vars = new HashSet<>(futureLiveVariables.get(i + 1));
            vars.addAll(actionReadsVariables(trace.getAction(i)));
            vars.removeAll(actionWritesVariables(trace.getAction(i)));
            vars.removeAll(actionHavocsVariables(trace.getAction(i)));
            futureLiveVariables.set(i, vars);
        }

        return futureLiveVariables;
    }

    private List<Collection<VarDecl<?>>> collectPastLiveVariablesForTrace(final Trace<? extends ExprState, ? extends StmtAction> trace) {
        final var stateCount = trace.getStates().size();
        final var pastLiveVariables = new ArrayList<Collection<VarDecl<?>>>(Collections.nCopies(stateCount, null));

        pastLiveVariables.set(0, Collections.emptySet());
        for(var i = 1; i < stateCount; i++) {
            var vars = new HashSet<>(pastLiveVariables.get(i - 1));
            vars.addAll(actionReadsVariables(trace.getAction(i - 1)));
            vars.addAll(actionWritesVariables(trace.getAction(i - 1)));
            vars.removeAll(actionHavocsVariables(trace.getAction(i - 1)));
            pastLiveVariables.set(i, vars);
        }

        return pastLiveVariables;
    }

    /*
     * Projection to live variables
     */

    private Expr<BoolType> existentialProjection(
        final Expr<BoolType> expr,
        final Collection<VarDecl<?>> variables,
        final Collection<VarDecl<?>> allVariables
    ) {
        var params = allVariables.stream()
            .filter(e -> !variables.contains(e))
            .map(e -> Tuple2.of(e, Param(e.getName(), e.getType())))
            .collect(Collectors.toUnmodifiableSet());

        var substitution = BasicSubstitution.builder()
            .putAll(params.stream().collect(toUnmodifiableMap(Tuple2::get1, e -> e.get2().getRef())))
            .build();

        return params.size() > 0
            ? Exists(params.stream().map(Tuple2::get2).collect(toList()), substitution.apply(expr))
            : expr;
    }

    private Expr<BoolType> universalProjection(
        final Expr<BoolType> expr,
        final Collection<VarDecl<?>> variables,
        final Collection<VarDecl<?>> allVariables
    ) {
        var params = allVariables.stream()
            .filter(e -> !variables.contains(e))
            .map(e -> Tuple2.of(e, Param(e.getName(), e.getType())))
            .collect(Collectors.toUnmodifiableSet());

        var substitution = BasicSubstitution.builder()
           .putAll(params.stream().collect(toUnmodifiableMap(Tuple2::get1, e -> e.get2().getRef())))
           .build();

        return params.size() > 0
            ? Forall(params.stream().map(Tuple2::get2).collect(toList()), substitution.apply(expr))
            : expr;
    }

    /*
     * Builder for ExprTraceNewtonChecker
     */

    public static class ExprTraceNewtonCheckerITBuilder {
        private final UCSolver solver;
        private final Expr<BoolType> init;
        private final Expr<BoolType> target;

        public ExprTraceNewtonCheckerITBuilder(UCSolver solver, Expr<BoolType> init, Expr<BoolType> target) {
            this.solver = solver;
            this.init = init;
            this.target = target;
        }

        public ExprTraceNewtonCheckerAssertBuilder withIT() {
            return new ExprTraceNewtonCheckerAssertBuilder(solver, init, target, true);
        }

        public ExprTraceNewtonCheckerAssertBuilder withoutIT() {
            return new ExprTraceNewtonCheckerAssertBuilder(solver, init, target, false);
        }
    }

    public static class ExprTraceNewtonCheckerAssertBuilder {
        private final UCSolver solver;
        private final Expr<BoolType> init;
        private final Expr<BoolType> target;

        private final boolean IT;

        public ExprTraceNewtonCheckerAssertBuilder(UCSolver solver, Expr<BoolType> init, Expr<BoolType> target, boolean it) {
            this.solver = solver;
            this.init = init;
            this.target = target;
            this.IT = it;
        }

        public ExprTraceNewtonCheckerLVBuilder withSP() {
            return new ExprTraceNewtonCheckerLVBuilder(solver, init, target, IT, AssertionGeneratorMethod.SP);
        }

        public ExprTraceNewtonCheckerLVBuilder withWP() {
            return new ExprTraceNewtonCheckerLVBuilder(solver, init, target, IT, AssertionGeneratorMethod.WP);
        }
    }

    public static class ExprTraceNewtonCheckerLVBuilder {
        private final UCSolver solver;
        private final Expr<BoolType> init;
        private final Expr<BoolType> target;

        private final boolean IT;
        private final AssertionGeneratorMethod SPorWP;

        public ExprTraceNewtonCheckerLVBuilder(UCSolver solver, Expr<BoolType> init, Expr<BoolType> target, boolean it, AssertionGeneratorMethod sPorWP) {
            this.solver = solver;
            this.init = init;
            this.target = target;
            this.IT = it;
            this.SPorWP = sPorWP;
        }

        public ExprTraceNewtonChecker withLV() {
            return new ExprTraceNewtonChecker(init, target, solver, IT, SPorWP, true);
        }

        public ExprTraceNewtonChecker withoutLV() {
            return new ExprTraceNewtonChecker(init, target, solver, IT, SPorWP, false);
        }
    }

    /*
     * Custom StmtAction to use when constructing helper traces
     */

    private static class NewtonAction extends StmtAction {
        private final List<Stmt> stmts;

        private NewtonAction(List<Stmt> stmts) {
            this.stmts = stmts;
        }

        public static NewtonAction of(List<Stmt> stmts) {
            return new NewtonAction(stmts);
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
