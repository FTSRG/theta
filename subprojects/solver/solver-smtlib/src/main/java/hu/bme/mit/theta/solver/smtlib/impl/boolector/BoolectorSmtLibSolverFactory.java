package hu.bme.mit.theta.solver.smtlib.impl.boolector;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverFactory;

import java.nio.file.Path;

public class BoolectorSmtLibSolverFactory extends GenericSmtLibSolverFactory {
    private BoolectorSmtLibSolverFactory(Path solverPath, String[] args) {
        super(solverPath, args);
    }

    public static BoolectorSmtLibSolverFactory create(Path solverPath, String[] args) {
        return new BoolectorSmtLibSolverFactory(solverPath, args);
    }

    @Override
    public UCSolver createUCSolver() {
        throw new UnsupportedOperationException("Boolector does not support unsat cores");
    }

    @Override
    public ItpSolver createItpSolver() {
        throw new UnsupportedOperationException("Boolector does not support interpolation");
    }
}
