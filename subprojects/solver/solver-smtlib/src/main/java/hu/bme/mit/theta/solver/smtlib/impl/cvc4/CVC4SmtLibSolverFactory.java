package hu.bme.mit.theta.solver.smtlib.impl.cvc4;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverFactory;

import java.nio.file.Path;

public class CVC4SmtLibSolverFactory extends GenericSmtLibSolverFactory {
    private CVC4SmtLibSolverFactory(Path solverPath, String[] args) {
        super(solverPath, args);
    }

    public static CVC4SmtLibSolverFactory create(Path solverPath, String[] args) {
        return new CVC4SmtLibSolverFactory(solverPath, args);
    }

    @Override
    public ItpSolver createItpSolver() {
        throw new UnsupportedOperationException("CVC4 does not support interpolation");
    }
}
