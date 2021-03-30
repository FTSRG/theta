package hu.bme.mit.theta.solver.smtlib.impl.smtinterpol;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTransformationManager;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolver;

import java.nio.file.Path;

public class SMTInterpolSmtLibSolverFactory implements SolverFactory {
    private final Path solverPath;
    private final String[] args;

    private SMTInterpolSmtLibSolverFactory(Path solverPath, String[] args) {
        this.solverPath = solverPath;
        this.args = args;
    }

    public static SMTInterpolSmtLibSolverFactory create(Path solverPath, String[] args) {
        return new SMTInterpolSmtLibSolverFactory(solverPath, args);
    }

    @Override
    public Solver createSolver() {
        final var symbolTable = new GenericSmtLibSymbolTable();
        final var transformationManager = new GenericSmtLibTransformationManager(symbolTable);
        final var termTransformer = new GenericSmtLibTermTransformer(symbolTable);
        final var solverBinary = new GenericSmtLibSolverBinary(getJavaBinary(), getSolverArgs());

        return new SmtLibSolver(symbolTable, transformationManager, termTransformer, solverBinary, false);
    }

    @Override
    public UCSolver createUCSolver() {
        final var symbolTable = new GenericSmtLibSymbolTable();
        final var transformationManager = new GenericSmtLibTransformationManager(symbolTable);
        final var termTransformer = new GenericSmtLibTermTransformer(symbolTable);
        final var solverBinary = new GenericSmtLibSolverBinary(getJavaBinary(), getSolverArgs());

        return new SmtLibSolver(symbolTable, transformationManager, termTransformer, solverBinary, true);
    }

    @Override
    public ItpSolver createItpSolver() {
        final var symbolTable = new GenericSmtLibSymbolTable();
        final var transformationManager = new GenericSmtLibTransformationManager(symbolTable);
        final var termTransformer = new GenericSmtLibTermTransformer(symbolTable);
        final var solverBinary = new GenericSmtLibSolverBinary(getJavaBinary(), getSolverArgs());

        return new SMTInterpolSmtLibItpSolver(symbolTable, transformationManager, termTransformer, solverBinary);
    }

    private Path getJavaBinary() {
        return Path.of(System.getProperty("java.home")).resolve("bin").resolve("java");
    }

    private String[] getSolverArgs() {
        final var solverArgs = new String[args.length + 2];
        solverArgs[0] = "-jar";
        solverArgs[1] = solverPath.toAbsolutePath().toString();
        for(var i = 0; i < args.length; i++) {
            solverArgs[i + 2] = args[i];
        }
        return solverArgs;
    }
}
