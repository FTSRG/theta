package hu.bme.mit.theta.solver.smtlib.impl.generic;

import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstallerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class GenericSmtLibSolverInstaller extends SmtLibSolverInstaller.Default {
    private Path solverPath;
    private String[] solverArgs;

    public GenericSmtLibSolverInstaller(final Logger logger) {
        super(logger);
    }

    @Override
    protected String getSolverName() {
        return "generic";
    }

    public void install(final Path home, final String version, final Path solverPath, final String[] solverArgs) throws SmtLibSolverInstallerException {
        this.solverPath = solverPath;
        this.solverArgs = solverArgs;
        super.install(home, version, version, solverPath);
    }

    @Override
    protected void installSolver(Path installDir, String version) throws SmtLibSolverInstallerException {
        checkState(solverPath != null);
        try {
            final var solverFilePath = solverFile(installDir);
            Files.writeString(solverFilePath, solverPath.toAbsolutePath().toString(), StandardCharsets.UTF_8);

            final var solverInfoPath = infoFile(installDir);
            final var info = Files.readString(solverInfoPath, StandardCharsets.UTF_8);
            Files.writeString(
                solverInfoPath,
                info + String.format("binary=%s\n", solverPath.toAbsolutePath().toString()),
                StandardCharsets.UTF_8
            );

            solverPath = null;
        }
        catch (IOException e) {
            throw new SmtLibSolverInstallerException(String.format("Error: %s", e.getMessage()), e);
        }
    }

    @Override
    protected void uninstallSolver(Path installDir, String version) throws SmtLibSolverInstallerException {
        try {
            final var solverFilePath = solverFile(installDir);
            Files.delete(solverFilePath);
        }
        catch (IOException e) {
            throw new SmtLibSolverInstallerException(String.format("Error: %s", e.getMessage()), e);
        }
    }

    @Override
    public SolverFactory getSolverFactory(final Path installDir, final String version, final Path solverPath, final String[] solverArgs) {
        return GenericSmtLibSolverFactory.create(solverPath, solverArgs);
    }

    @Override
    public List<String> getSupportedVersions() {
        return Collections.emptyList();
    }

    @Override
    protected String[] getDefaultSolverArgs(String version) {
        checkState(solverArgs != null);
        final var tmp = solverArgs;
        solverArgs = null;
        return tmp;
    }

    private Path solverFile(final Path installDir) {
        return installDir.resolve("solver.txt");
    }
}
