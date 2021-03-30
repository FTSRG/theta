package hu.bme.mit.theta.solver.smtlib.solver.installer;

import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.SolverFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public interface SmtLibSolverInstaller {

    void install(Path home, String version, String name) throws SmtLibSolverInstallerException;

    void install(Path home, String version, String name, Path solverPath) throws SmtLibSolverInstallerException;

    void uninstall(Path home, String version) throws SmtLibSolverInstallerException;

    void rename(Path home, String version, String name) throws SmtLibSolverInstallerException;

    String getInfo(Path home, String version) throws SmtLibSolverInstallerException;

    Path getArgsFile(Path home, String version) throws SmtLibSolverInstallerException;

    SolverFactory getSolverFactory(Path home, String version) throws SmtLibSolverInstallerException;

    List<String> getSupportedVersions() throws SmtLibSolverInstallerException;

    List<String> getInstalledVersions(Path home) throws SmtLibSolverInstallerException;

    abstract class Default implements SmtLibSolverInstaller {
        protected final Logger logger;

        public Default(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public final void install(final Path home, final String version, final String name) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkVersion(version);
            checkName(name);

            doInstall(home, version, name, null);
        }

        @Override
        public final void install(final Path home, final String version, final String name, final Path solverPath) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkVersion(version);
            checkName(name);
            checkNotNull(solverPath);

            doInstall(home, version, name, solverPath);
        }

        private void doInstall(final Path home, final String version, final String name, final Path solverPath) throws SmtLibSolverInstallerException {
            final var installDir = getInstallDir(home, name);
            if(Files.exists(installDir)) {
                throw new SmtLibSolverInstallerException("There is already a solver instance with this name installed");
            }

            try {
                logger.write(Logger.Level.MAINSTEP, "Beginning installation...\n");

                Files.createDirectory(installDir);

                if(solverPath != null) {
                    final var solverFilePath = solverFile(installDir);
                    Files.writeString(solverFilePath, solverPath.toAbsolutePath().toString(), StandardCharsets.UTF_8);
                }

                final var solverArgsPath = argsFile(installDir);
                Files.writeString(solverArgsPath, String.join("\n", getDefaultSolverArgs(version)), StandardCharsets.UTF_8);

                final var solverInfoPath = infoFile(installDir);
                Files.writeString(
                    solverInfoPath,
                    String.format("solver=%s\n", getSolverName()) +
                        String.format("version=%s\n", version) +
                        String.format("name=%s\n", name) +
                        (solverPath != null ? String.format("binary=%s\n", solverPath.toAbsolutePath().toString()) : ""),
                    StandardCharsets.UTF_8
                );

                if(solverPath == null) {
                    installSolver(installDir, version);
                }

                logger.write(Logger.Level.MAINSTEP, "Installation finished\n");
            }
            catch (SmtLibSolverInstallerException e) {
                uninstall(home, version);
                throw e;
            }
            catch (IOException e) {
                throw new SmtLibSolverInstallerException(String.format("Error: %s", e.getMessage()), e);
            }
        }

        @Override
        public final void uninstall(Path home, String version) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkName(version);

            final var installDir = getInstallDir(home, version);
            if(!Files.exists(installDir)) {
                throw new SmtLibSolverInstallerException("The version is not installed");
            }

            try {
                logger.write(Logger.Level.MAINSTEP, "Beginning uninstallation...\n");

                uninstallSolver(installDir, version);

                deleteDirectory(installDir.toFile());

                logger.write(Logger.Level.MAINSTEP, "Uninstallation finished\n");
            }
            catch (IOException e) {
                throw new SmtLibSolverInstallerException(String.format("Error: %s", e.getMessage()), e);
            }
        }

        @Override
        public void rename(final Path home, final String version, final String name) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkVersion(version);
            checkName(name);

            final var installDir = getInstallDir(home, version);
            if(!Files.exists(installDir)) {
                throw new SmtLibSolverInstallerException("The version is not installed");
            }

            final var newInstallDir = getInstallDir(home, name);
            if(Files.exists(newInstallDir)) {
                throw new SmtLibSolverInstallerException("The chosen name is already used");
            }

            try {
                Files.move(installDir, newInstallDir);
            }
            catch (IOException e) {
                throw new SmtLibSolverInstallerException(String.format("Error renaming solver: %s", e.getMessage()), e);
            }
        }

        @Override
        public final SolverFactory getSolverFactory(final Path home, final String version) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkVersion(version);

            final var installDir = home.resolve(version);
            if(!Files.exists(installDir)) {
                throw new SmtLibSolverInstallerException("The version is not installed");
            }

            try {
                final Path solverPath;
                final var solverFilePath = solverFile(installDir);
                if(Files.exists(solverFilePath)) {
                    solverPath = Path.of(Files.readAllLines(solverFilePath, StandardCharsets.UTF_8).get(0));
                }
                else {
                    solverPath = null;
                }

                final var solverArgsPath = argsFile(installDir);
                final var solverArgs = Files.readAllLines(solverArgsPath, StandardCharsets.UTF_8).toArray(String[]::new);

                return getSolverFactory(installDir, version, solverPath, solverArgs);
            }
            catch (IOException e) {
                throw new SmtLibSolverInstallerException(String.format("Error: %s", e.getMessage()), e);
            }
        }

        @Override
        public final String getInfo(Path home, String version) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkName(version);

            final var installDir = getInstallDir(home, version);
            if(!Files.exists(installDir)) {
                throw new SmtLibSolverInstallerException("The version is not installed");
            }

            try {
                final var solverInfoPath = infoFile(installDir);
                final var solverInfoStr = Files.readString(solverInfoPath, StandardCharsets.UTF_8);
                final var solverArgsPath = argsFile(installDir);
                final var solverArgs = Files.readAllLines(solverArgsPath, StandardCharsets.UTF_8).toArray(String[]::new);

                return solverInfoStr + String.format("args=%s\n", String.join(" ", solverArgs));
            }
            catch (IOException e) {
                throw new SmtLibSolverInstallerException(String.format("Error: %s", e.getMessage()), e);
            }
        }

        @Override
        public final Path getArgsFile(Path home, String version) throws SmtLibSolverInstallerException {
            checkNotNull(home);
            checkArgument(Files.exists(home));
            checkName(version);

            final var installDir = home.resolve(version);
            if(!Files.exists(installDir)) {
                throw new SmtLibSolverInstallerException("The version is not installed");
            }

            return argsFile(installDir);
        }

        @Override
        public final List<String> getInstalledVersions(Path home) {
            checkNotNull(home);

            if(Files.exists(home)) {
                final var installedDirs = home.toFile()
                    .list((current, name) -> new File(current, name).isDirectory());

                assert installedDirs != null;
                return Arrays.asList(installedDirs);
            }
            else {
                return Collections.emptyList();
            }
        }

        protected abstract String getSolverName() throws SmtLibSolverInstallerException;
        protected abstract void installSolver(final Path installDir, final String version) throws SmtLibSolverInstallerException;
        protected abstract void uninstallSolver(final Path installDir, final String version) throws SmtLibSolverInstallerException;
        protected abstract SolverFactory getSolverFactory(final Path installDir, final String version, final Path solverPath, final String[] args) throws SmtLibSolverInstallerException;
        protected abstract String[] getDefaultSolverArgs(final String version) throws SmtLibSolverInstallerException;

        protected final void checkName(final String name) throws SmtLibSolverInstallerException {
            if(!name.matches("^[a-zA-Z0-9_.-]+$") || name.matches("latest")) {
                throw new SmtLibSolverInstallerException("Unsupported name format: " + name);
            }
        }

        protected final void checkVersion(final String version) throws SmtLibSolverInstallerException {
            if(!version.matches("^[a-zA-Z0-9_.-]+$")) {
                throw new SmtLibSolverInstallerException("Unsupported version format: " + version);
            }
        }

        protected final Path getInstallDir(final Path home, final String version) {
            return home.resolve(version);
        }

        private Path solverFile(final Path installDir) {
            return installDir.resolve("solver.txt");
        }

        protected final Path argsFile(final Path installDir) {
            return installDir.resolve("solver-args.txt");
        }

        protected final Path infoFile(final Path installDir) {
            return installDir.resolve("solver-info.txt");
        }

        private void deleteDirectory(File directoryToBeDeleted) throws IOException {
            File[] allContents = directoryToBeDeleted.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    deleteDirectory(file);
                }
            }

            Files.delete(directoryToBeDeleted.toPath());
        }
    }
}
