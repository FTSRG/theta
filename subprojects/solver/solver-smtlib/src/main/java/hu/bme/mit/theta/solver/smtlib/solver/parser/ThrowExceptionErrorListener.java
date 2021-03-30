package hu.bme.mit.theta.solver.smtlib.solver.parser;

import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolverException;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ThrowExceptionErrorListener extends BaseErrorListener implements ANTLRErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        throw new SmtLibSolverException(String.format("Invalid Expression: %s", msg), e);
    }
}