package hu.bme.mit.theta.solver.smtlib.impl.z3;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverFactory;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTransformationManager;

import java.nio.file.Path;

public class Z3SmtLibSolverFactory extends GenericSmtLibSolverFactory {
    private final boolean itpSupported;

    private Z3SmtLibSolverFactory(Path solverPath, String[] args, boolean itpSupported) {
        super(solverPath, args);
        this.itpSupported = itpSupported;
    }

    public static Z3SmtLibSolverFactory create(Path solverPath, String[] args, boolean itpSupported) {
        return new Z3SmtLibSolverFactory(solverPath, args, itpSupported);
    }

    @Override
    public ItpSolver createItpSolver() {
        if(itpSupported) {
            final var symbolTable = new GenericSmtLibSymbolTable();
            final var transformationManager = new GenericSmtLibTransformationManager(symbolTable);
            final var termTransformer = new GenericSmtLibTermTransformer(symbolTable);
            final var solverBinary = new GenericSmtLibSolverBinary(solverPath, args);

            return new Z3SmtLibItpSolver(symbolTable, transformationManager, termTransformer, solverBinary);
        }
        else {
            throw new UnsupportedOperationException("Z3 interpolation supported below 4.5.0");
        }
    }
}
