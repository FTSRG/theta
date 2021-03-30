package hu.bme.mit.theta.solver.smtlib.impl.z3;

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

public class Z3SmtLibSolverInstaller extends SmtLibSolverInstaller.Default {
    private final List<SemVer.VersionDecoder> versions;

    public Z3SmtLibSolverInstaller(final Logger logger) {
        super(logger);

        versions = new ArrayList<>();
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.8.5"))
            .addString(LINUX, X64, "x64-ubuntu-16.04")
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .addString(MAC, X64, "x64-osx-10.14.6")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.8.5"))
            .addString(LINUX, X64, "x64-ubuntu-16.04")
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .addString(MAC, X64, "x64-osx-10.14.2")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.8.4"))
            .addString(LINUX, X64, "x64-ubuntu-16.04")
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .addString(MAC, X64, "x64-osx-10.14.1")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.8.3"))
            .addString(LINUX, X64, "x64-ubuntu-16.04")
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .addString(MAC, X64, "x64-osx-10.13.6")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.6.0"))
            .addString(LINUX, X64, "x64-ubuntu-16.04")
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .addString(MAC, X64, "x64-osx-10.11.6")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.4.0"))
            .addString(LINUX, X64, "x64-ubuntu-14.04")
            .addString(LINUX, X86, "x86-ubuntu-14.04")
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .addString(MAC, X64, "x64-osx-10.11.6")
            .build()
        );
        versions.add(SemVer.VersionDecoder.create(SemVer.of("4.3.2"))
            .addString(WINDOWS, X64, "x64-win")
            .addString(WINDOWS, X86, "x86-win")
            .build()
        );
    }

    @Override
    protected String getSolverName() {
        return "z3";
    }

    @Override
    protected void installSolver(final Path installDir, final String version) throws SmtLibSolverInstallerException {
        final var semVer = SemVer.of(version);
        String archStr = null;

        for(final var versionDecoder : versions) {
            if(semVer.compareTo(versionDecoder.getVersion()) >= 0) {
                archStr = versionDecoder.getOsArchString(OsHelper.getOs(), OsHelper.getArch());
                break;
            }
        }
        if(archStr == null) {
            throw new SmtLibSolverInstallerException(String.format("z3 on operating system %s and architecture %s is not supported", OsHelper.getOs(), OsHelper.getArch()));
        }

        final var downloadUrl = URI.create(String.format(
            "https://github.com/Z3Prover/z3/releases/download/z3-%s/z3-%s-%s.zip",
            version, version, archStr
        ));

        logger.write(Logger.Level.MAINSTEP, "Starting download (%s)...\n", downloadUrl.toString());
        try(final var inputStream = downloadUrl.toURL().openStream()) {
            Compress.extract(inputStream, installDir, Compress.CompressionType.ZIP);
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
        return new String[] { "-smt2", "-in" };
    }

    @Override
    public SolverFactory getSolverFactory(final Path installDir, final String version, final Path solverPath, final String[] solverArgs) throws SmtLibSolverInstallerException {
        final var solverFilePath = solverPath != null ? solverPath : installDir.resolve("bin").resolve(getSolverBinaryName());
        return Z3SmtLibSolverFactory.create(solverFilePath, solverArgs, SemVer.of(version).compareTo(SemVer.of("4.5.0")) <= 0);
    }

    @Override
    public List<String> getSupportedVersions() {
        return Arrays.asList(
            "4.8.9", "4.8.8", "4.8.7", "4.8.6", "4.8.5", "4.8.4", "4.8.3", "4.8.2", "4.8.1",
            "4.7.1", "4.6.0", "4.5.0", "4.4.1", "4.4.0", "4.3.2"
        );
    }

    private String getSolverBinaryName() {
        switch(OsHelper.getOs()) {
            case WINDOWS:
                return "z3.exe";
            case LINUX:
                return "z3";
            case MAC:
                return "z3.dmg";
            default:
                throw new AssertionError();
        }
    }
}
