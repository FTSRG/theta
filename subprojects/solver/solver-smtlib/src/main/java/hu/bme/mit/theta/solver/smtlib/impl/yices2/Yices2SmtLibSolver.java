package hu.bme.mit.theta.solver.smtlib.impl.yices2;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.solver.SolverStatus;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolver;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolverException;
import hu.bme.mit.theta.solver.smtlib.solver.binary.SmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibModel;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibValuation;
import hu.bme.mit.theta.solver.smtlib.solver.parser.GetModelResponse;
import hu.bme.mit.theta.solver.smtlib.solver.parser.ThrowExceptionErrorListener;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;

public class Yices2SmtLibSolver extends SmtLibSolver {
    private Valuation model;
    private SolverStatus status;

    public Yices2SmtLibSolver(
        final SmtLibSymbolTable symbolTable, final SmtLibTransformationManager transformationManager,
        final SmtLibTermTransformer termTransformer, final SmtLibSolverBinary solverBinary,
        final boolean unsatCoreEnabled
    ) {
        super(symbolTable, transformationManager, termTransformer, solverBinary, unsatCoreEnabled);
    }

    @Override
    public SolverStatus check() {
        return status = super.check();
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

        final var modelValues = new HashMap<String, String>();
        for(final var ignored : declarationStack.toCollection()) {
            final var value = parseModelResponse(solverBinary.readResponse());
            modelValues.put(value.get1(), value.get2());
        }

        return new SmtLibValuation(symbolTable, transformationManager, termTransformer, new SmtLibModel(modelValues));
    }

    private Tuple2<String, String> parseModelResponse(final String response) {
        try {
            final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(response));
            final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowExceptionErrorListener());
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowExceptionErrorListener());

            final var term = parser.term().generic_term();
            final var id = GetModelResponse.extractString(term.qual_identifier());
            final var variable = GetModelResponse.extractString(term.term(0));
            final var value = GetModelResponse.extractString(term.term(1));

            if(id.equals("=")) {
                return Tuple2.of(variable, String.format("%s () (_ theta_type unknown) %s", variable, value));
            }

            throw new SmtLibSolverException("Could not parse solver output: " + response);
        }
        catch (Exception e) {
            throw new SmtLibSolverException("Could not parse solver output: " + response, e);
        }
    }
}
