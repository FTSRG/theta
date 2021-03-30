package hu.bme.mit.theta.solver.smtlib.solver;

import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverStatus;
import hu.bme.mit.theta.solver.Stack;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.UnknownSolverStatusException;
import hu.bme.mit.theta.solver.impl.StackImpl;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.solver.parser.CheckSatResponse;
import hu.bme.mit.theta.solver.smtlib.solver.parser.GeneralResponse;
import hu.bme.mit.theta.solver.smtlib.solver.parser.GetModelResponse;
import hu.bme.mit.theta.solver.smtlib.solver.parser.GetUnsatCoreResponse;
import hu.bme.mit.theta.solver.smtlib.solver.parser.ThrowExceptionErrorListener;
import hu.bme.mit.theta.solver.smtlib.solver.binary.SmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibValuation;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class SmtLibSolver implements UCSolver, Solver {
    protected final SmtLibSymbolTable symbolTable;
    protected final SmtLibTransformationManager transformationManager;
    protected final SmtLibTermTransformer termTransformer;

    protected final SmtLibSolverBinary solverBinary;
    private final boolean unsatCoreEnabled;

    protected final Stack<Expr<BoolType>> assertions;
    protected final Map<String, Expr<BoolType>> assumptions;
    protected final Stack<ConstDecl<?>> declarationStack;

    private static final String ASSUMPTION_LABEL = "_LABEL_%d";
    private int labelNum = 0;

    private Valuation model;
    private Collection<Expr<BoolType>> unsatCore;
    private SolverStatus status;

    public SmtLibSolver(
        final SmtLibSymbolTable symbolTable, final SmtLibTransformationManager transformationManager,
        final SmtLibTermTransformer termTransformer, final SmtLibSolverBinary solverBinary, boolean unsatCoreEnabled
    ) {
        this.solverBinary = solverBinary;
        this.symbolTable = symbolTable;
        this.transformationManager = transformationManager;
        this.termTransformer = termTransformer;

        this.unsatCoreEnabled = unsatCoreEnabled;

        assertions = new StackImpl<>();
        assumptions = new HashMap<>();
        declarationStack = new StackImpl<>();

        init();
    }

    @Override
    public void add(Expr<BoolType> assertion) {
        final var consts = ExprUtils.getConstants(assertion);
        consts.removeAll(declarationStack.toCollection());
        declarationStack.add(consts);

        final var term = transformationManager.toTerm(assertion);

        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert %s)", term));

        clearState();
    }

    public void add(final Expr<BoolType> assertion, final String term) {
        final var consts = ExprUtils.getConstants(assertion);
        consts.removeAll(declarationStack.toCollection());
        declarationStack.add(consts);

        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert %s)", term));

        clearState();
    }

    @Override
    public void track(Expr<BoolType> assertion) {
        final var consts = ExprUtils.getConstants(assertion);
        consts.removeAll(declarationStack.toCollection());
        declarationStack.add(consts);

        final var term = transformationManager.toTerm(assertion);
        final var label = String.format(ASSUMPTION_LABEL, labelNum++);
        assumptions.put(label, assertion);

        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert (! %s :named %s))", term, label));

        clearState();
    }

    @Override
    public SolverStatus check() {
        solverBinary.issueCommand("(check-sat)");
        var res = parseResponse(solverBinary.readResponse());
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
        else if(res.isSpecific()) {
            final CheckSatResponse checkSatResponse = res.asSpecific();
            if(checkSatResponse.isSat()) {
                status = SolverStatus.SAT;
            }
            else if(checkSatResponse.isUnsat()) {
                status = SolverStatus.UNSAT;
            }
            else {
                throw new UnknownSolverStatusException();
            }
        }
        else {
            throw new AssertionError();
        }

        return status;
    }

    @Override
    public void push() {
        assertions.push();
        declarationStack.push();
        issueGeneralCommand("(push 1)");
    }

    @Override
    public void pop(int n) {
        assertions.pop(n);
        declarationStack.pop(n);
        issueGeneralCommand("(pop 1)");
        clearState();
    }

    @Override
    public void reset() {
        issueGeneralCommand("(reset)");
        clearState();
        init();
    }

    @Override
    public SolverStatus getStatus() {
        checkState(status != null, "Solver status is unknown.");
        return status;
    }

    @Override
    public Valuation getModel() {
        checkState(status == SolverStatus.SAT, "Cannot get model if status is not SAT.");

        if (model == null) {
            model = extractModel();
        }

        return model;
    }

    private Valuation extractModel() {
        assert status == SolverStatus.SAT;
        assert model == null;

        solverBinary.issueCommand("(get-model)");
        final var res = parseResponse(solverBinary.readResponse());
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
        else if(res.isSpecific()) {
            final GetModelResponse getModelResponse = res.asSpecific();
            return new SmtLibValuation(symbolTable, transformationManager, termTransformer, getModelResponse.getModel());
        }
        else {
            throw new AssertionError();
        }
    }

    @Override
    public Collection<Expr<BoolType>> getUnsatCore() {
        checkState(status == SolverStatus.UNSAT, "Cannot get unsat core if status is not UNSAT");

        if (unsatCore == null) {
            unsatCore = extractUnsatCore();
        }

        return Collections.unmodifiableCollection(unsatCore);
    }

    private Collection<Expr<BoolType>> extractUnsatCore() {
        assert status == SolverStatus.UNSAT;
        assert unsatCore == null;

        final Collection<Expr<BoolType>> unsatCore = new LinkedList<>();
        final Collection<String> unsatCoreLabels;

        solverBinary.issueCommand("(get-unsat-core)");
        final var res = parseResponse(solverBinary.readResponse());
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
        else if(res.isSpecific()) {
            final GetUnsatCoreResponse getUnsatCoreResponse = res.asSpecific();
            unsatCoreLabels = getUnsatCoreResponse.getLabels();
        }
        else {
            throw new AssertionError();
        }

        for(final var label : unsatCoreLabels) {
            final Expr<BoolType> assumption = assumptions.get(label);
            assert assumption != null;
            unsatCore.add(assumption);
        }

        return unsatCore;
    }

    @Override
    public Collection<Expr<BoolType>> getAssertions() {
        return assertions.toCollection();
    }

    private void init() {
        issueGeneralCommand("(set-option :print-success true)");
        issueGeneralCommand("(set-option :produce-models true)");
        if(unsatCoreEnabled) {
            issueGeneralCommand("(set-option :produce-unsat-cores true)");
        }
        issueGeneralCommand("(set-logic ALL)");
    }

    protected void clearState() {
        status = null;
        model = null;
        unsatCore = null;
    }

    protected final void issueGeneralCommand(String command) {
        solverBinary.issueCommand(command);
        var res = parseResponse(solverBinary.readResponse());
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
    }

    protected final GeneralResponse parseResponse(final String response) {
        try {
            final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(response));
            final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowExceptionErrorListener());
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowExceptionErrorListener());
            return GeneralResponse.fromContext(parser.response());
        }
        catch (Exception e) {
            throw new SmtLibSolverException("Could not parse solver output: " + response, e);
        }
    }

}
