package hu.bme.mit.theta.solver.smtlib.impl.mathsat;

import hu.bme.mit.theta.common.OsHelper;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.utils.Compress;
import hu.bme.mit.theta.solver.smtlib.utils.SemVer;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstallerException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.common.OsHelper.Architecture.X64;
import static hu.bme.mit.theta.common.OsHelper.Architecture.X86;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.LINUX;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.MAC;
import static hu.bme.mit.theta.common.OsHelper.OperatingSystem.WINDOWS;

public class MathSATSmtLibSolverInstaller extends SmtLibSolverInstaller.Default {
    private final List<SemVer.VersionDecoder> versions;

    public MathSATSmtLibSolverInstaller(final Logger logger) {
        super(logger);

        versions = new ArrayList<>();
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.5.0"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(MAC, X64, "darwin-libcxx-x86_64")
            .addString(WINDOWS, X64, "win64-msvc")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.3.6"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(LINUX, X86, "linux-x86")
            .addString(MAC, X64, "darwin-libcxx-x86_64")
            .addString(WINDOWS, X64, "win64-msvc")
            .addString(WINDOWS, X86, "win32-msvc")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.2.12"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(LINUX, X86, "linux-x86")
            .addString(MAC, X64, "darwin-libcxx-x86_64")
            .addString(WINDOWS, X64, "win64-msvc18")
            .addString(WINDOWS, X86, "win32-msvc18")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.2.9"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(LINUX, X86, "linux-x86")
            .addString(MAC, X64, "darwin-libcxx-x86_64")
            .addString(WINDOWS, X64, "win64-msvc")
            .addString(WINDOWS, X86, "win32-msvc")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.2.6"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(LINUX, X86, "linux-x86")
            .addString(MAC, X64, "darwin-x86_64")
            .addString(WINDOWS, X64, "win64-msvc")
            .addString(WINDOWS, X86, "win32-msvc")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.1.10"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(LINUX, X86, "linux-x86")
            .addString(MAC, X64, "darwin-x86_64")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("5.1.1"))
            .addString(LINUX, X64, "linux-x86_64")
            .addString(LINUX, X86, "linux-x86")
            .build()
        );
    }

    @Override
    protected String getSolverName() {
        return "mathsat";
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
            throw new SmtLibSolverInstallerException(String.format("MathSAT on operating system %s and architecture %s is not supported", OsHelper.getOs(), OsHelper.getArch()));
        }

        final var downloadUrl = URI.create(String.format(
            "https://mathsat.fbk.eu/download.php?file=mathsat-%s-%s.%s",
            version, archStr, OsHelper.getOs().equals(WINDOWS) ? "zip" : "tar.gz"
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
    protected String[] getDefaultSolverArgs(String version) throws SmtLibSolverInstallerException {
        return new String[] { "-theory.bv.eager=false" };
    }

    @Override
    public SolverFactory getSolverFactory(final Path installDir, final String version, final Path solverPath, final String[] solverArgs) throws SmtLibSolverInstallerException {
        final var solverFilePath = solverPath != null ? solverPath : installDir.resolve("bin").resolve(getSolverBinaryName());
        return MathSATSmtLibSolverFactory.create(solverFilePath, solverArgs, SemVer.of(version).compareTo(SemVer.of("5.4.0")) >= 0);
    }

    @Override
    public List<String> getSupportedVersions() {
        return Arrays.asList(
            "5.6.5", "5.6.4", "5.6.3", "5.6.2", "5.6.1", "5.6.0",
            "5.5.4", "5.5.3", "5.5.2", "5.5.1", "5.5.0",
            "5.4.1", "5.4.0",
            "5.3.14", "5.3.13", "5.3.12", "5.3.11", "5.3.10", "5.3.9", "5.3.8", "5.3.7", "5.3.6", "5.3.5", "5.3.4", "5.3.3", "5.3.2", "5.3.1",
            "5.2.12", "5.2.11", "5.2.10", "5.2.9", "5.2.8", "5.2.7", "5.2.6", "5.2.5", "5.2.4", "5.2.3", "5.2.2", "5.2.1",
            "5.1.12", "5.1.11", "5.1.10", "5.1.9", "5.1.8", "5.1.7", "5.1.6", "5.1.5", "5.1.4", "5.1.3"
        );
    }

    private String getSolverBinaryName() {
        switch(OsHelper.getOs()) {
            case WINDOWS:
                return "mathsat.exe";
            case LINUX:
                return "mathsat";
            case MAC:
                return "mathsat.dmg";
            default:
                throw new AssertionError();
        }
    }
}
