package hu.bme.mit.theta.solver.smtlib.impl.cvc4;

import hu.bme.mit.theta.common.OsHelper;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.utils.SemVer;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstallerException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.common.OsHelper.Architecture.X64;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.LINUX;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.WINDOWS;

public class CVC4SmtLibSolverInstaller extends SmtLibSolverInstaller.Default {

    public CVC4SmtLibSolverInstaller(final Logger logger) {
        super(logger);
    }

    @Override
    protected String getSolverName() {
        return "cvc4";
    }

    @Override
    protected void installSolver(final Path installDir, final String version) throws SmtLibSolverInstallerException {

        try(
            final var inputChannel = Channels.newChannel(getDownloadUrl(version).openStream());
            final var outputChannel = new FileOutputStream(installDir.resolve(getSolverBinaryName()).toAbsolutePath().toString()).getChannel()
        ) {
            logger.write(Logger.Level.MAINSTEP, "Starting download (%s)...\n", getDownloadUrl(version).toString());
            outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
            installDir.resolve(getSolverBinaryName()).toFile().setExecutable(true, true);
        }
        catch (IOException e) {
            throw new SmtLibSolverInstallerException(e);
        }

        logger.write(Logger.Level.MAINSTEP, "Download finished\n");
    }

    @Override
    protected void uninstallSolver(final Path installDir, final String version) { }

    @Override
    protected String[] getDefaultSolverArgs(String version) {
        return new String[] {
            "--lang", "smt2",
            "--output-lang", "smt2",
            "--quiet",
            "--incremental"
        };
    }

    @Override
    public SolverFactory getSolverFactory(final Path installDir, final String version, final Path solverPath, final String[] solverArgs) throws SmtLibSolverInstallerException {
        final var solverFilePath = solverPath != null ? solverPath : installDir.resolve(getSolverBinaryName());
        return CVC4SmtLibSolverFactory.create(solverFilePath, solverArgs);
    }

    @Override
    public List<String> getSupportedVersions() {
        return Arrays.asList("1.8", "1.7", "1.6", "1.5", "1.4", "1.3", "1.2", "1.1", "1.0");
    }

    private URL getDownloadUrl(final String version) throws SmtLibSolverInstallerException, MalformedURLException {
        final var os = OsHelper.getOs();
        final var arch = OsHelper.getArch();

        final String archString;
        final String platformExtension;
        if(arch != X64) {
            throw new SmtLibSolverInstallerException("cvc4 is available only for x64 architecture");
        }
        else if(os != LINUX && os != WINDOWS) {
            throw new SmtLibSolverInstallerException("cvc4 is available only for Windows and Linux");
        }
        else if(os == LINUX) {
            archString = "x86_64-linux-opt";
            platformExtension = "";
        }
        else /* if(os == WINDOWS) */ {
            if(SemVer.of(version).compareTo(SemVer.of("1.6")) >= 0) {
                archString = "win64-opt";
                platformExtension = ".exe";
            }
            else {
                throw new SmtLibSolverInstallerException("Windows platform is only supported for version 1.6 and forward");
            }
        }

        return URI.create(String.format(
            "https://cvc4.cs.stanford.edu/downloads/builds/%s/cvc4-%s-%s%s",
            archString, version, archString, platformExtension
        )).toURL();
    }

    private String getSolverBinaryName() {
        switch(OsHelper.getOs()) {
            case WINDOWS:
                return "cvc4.exe";
            case LINUX:
                return "cvc4";
            default:
                throw new AssertionError();
        }
    }
}
