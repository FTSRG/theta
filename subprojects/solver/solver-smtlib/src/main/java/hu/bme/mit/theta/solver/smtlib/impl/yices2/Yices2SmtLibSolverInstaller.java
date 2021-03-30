package hu.bme.mit.theta.solver.smtlib.impl.yices2;

import hu.bme.mit.theta.common.OsHelper;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstallerException;
import hu.bme.mit.theta.solver.smtlib.utils.Compress;
import hu.bme.mit.theta.solver.smtlib.utils.SemVer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static hu.bme.mit.theta.common.OsHelper.Architecture.X64;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.LINUX;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.MAC;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.WINDOWS;

public class Yices2SmtLibSolverInstaller extends SmtLibSolverInstaller.Default {
    private final List<SemVer.VersionDecoder> versions;

    public Yices2SmtLibSolverInstaller(final Logger logger) {
        super(logger);

        versions = new ArrayList<>();
        versions.add(SemVer.VersionDecoder.create(SemVer.of("2.6.2"))
            .addString(LINUX, X64, "x86_64-pc-linux-gnu-static-gmp")
            .addString(MAC, X64, "x86_64-apple-darwin18.7.0-static-gmp")
            .addString(WINDOWS, X64, "x86_64-pc-mingw32-static-gmp")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("2.5.4"))
            .addString(LINUX, X64, "x86_64-pc-linux-gnu-static-gmp")
            .addString(MAC, X64, "x86_64-apple-darwin16.7.0-static-gmp")
            .addString(WINDOWS, X64, "x86_64-pc-mingw32-static-gmp")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("2.5.3"))
            .addString(LINUX, X64, "x86_64-unknown-linux-gnu-static-gmp")
            .addString(MAC, X64, "x86_64-apple-darwin16.7.0-static-gmp")
            .addString(WINDOWS, X64, "x86_64-pc-mingw32-static-gmp")
            .build()
        );
    }

    @Override
    protected String getSolverName() {
        return "yices2";
    }

    @Override
    protected void installSolver(Path installDir, String version) throws SmtLibSolverInstallerException {
        final var semVer = SemVer.of(version);
        String archStr = null;

        for(final var versionDecoder : versions) {
            if(semVer.compareTo(versionDecoder.getVersion()) >= 0) {
                archStr = versionDecoder.getOsArchString(OsHelper.getOs(), OsHelper.getArch());
                break;
            }
        }
        if(archStr == null) {
            throw new SmtLibSolverInstallerException(String.format("Yices2 on operating system %s and architecture %s is not supported", OsHelper.getOs(), OsHelper.getArch()));
        }

        final var downloadUrl = URI.create(String.format(
            "https://yices.csl.sri.com/releases/%s/yices-%s-%s.%s",
            version, version, archStr, OsHelper.getOs().equals(WINDOWS) ? "zip" : "tar.gz"
        ));

        logger.write(Logger.Level.MAINSTEP, "Starting download (%s)...\n", downloadUrl.toString());

        try(final var inputStream = downloadUrl.toURL().openStream()) {
            if(OsHelper.getOs().equals(WINDOWS)) {
                Compress.extract(inputStream, installDir, Compress.CompressionType.ZIP);
            }
            else {
                Compress.extract(inputStream, installDir, Compress.CompressionType.TARGZ);
            }
            installDir.resolve("bin").resolve(getSolverBinaryName()).toFile().setExecutable(true, true);
        }
        catch (IOException e) {
            throw new SmtLibSolverInstallerException(e);
        }

        logger.write(Logger.Level.MAINSTEP, "Download finished\n");
    }

    @Override
    protected void uninstallSolver(Path installDir, String version) { }

    @Override
    protected String[] getDefaultSolverArgs(String version) {
        return new String[] { "--incremental" };
    }

    @Override
    public SolverFactory getSolverFactory(final Path installDir, final String version, final Path solverPath, final String[] solverArgs) throws SmtLibSolverInstallerException {
        final var solverFilePath = solverPath != null ? solverPath : installDir.resolve("bin").resolve(getSolverBinaryName());
        return Yices2SmtLibSolverFactory.create(solverFilePath, solverArgs);
    }

    @Override
    public List<String> getSupportedVersions() {
        return Arrays.asList(
            "2.6.2", "2.6.1", "2.6.0"
        );
    }

    private String getSolverBinaryName() {
        switch(OsHelper.getOs()) {
            case WINDOWS:
                return "yices-smt2.exe";
            case LINUX:
            case MAC:
                return "yices-smt2";
            default:
                throw new AssertionError();
        }
    }
}
