package hu.bme.mit.theta.solver.smtlib.impl.yices2;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverFactory;

import java.nio.file.Path;

public class Yices2SmtLibSolverFactory extends GenericSmtLibSolverFactory {

    private Yices2SmtLibSolverFactory(Path solverPath, String[] args) {
        super(solverPath, args);
    }

    public static Yices2SmtLibSolverFactory create(Path solverPath, String[] args) {
        return new Yices2SmtLibSolverFactory(solverPath, args);
    }

    @Override
    public ItpSolver createItpSolver() {
        throw new UnsupportedOperationException("Yices2 does not support interpolation");
    }
}
