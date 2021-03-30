package hu.bme.mit.theta.solver.smtlib.cli;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstallerException;
import hu.bme.mit.theta.solver.smtlib.SmtLibSolverManager;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class SmtLibCli {
    private static final String JAR_NAME = "theta-solver-smtlib-cli.jar";
    private final String[] args;

    private Logger logger;

    static class MainParams {
        @Parameter(names = "--home", description = "The path of the solver registry")
        String home = Path.of(System.getProperty("user.home"), ".theta").toAbsolutePath().toString();

        @Parameter(names = "--loglevel", description = "Detailedness of logging")
        Logger.Level logLevel = Logger.Level.MAINSTEP;

        @Parameter(names = "--stacktrace", description = "Prints the stacktrace in case of an error")
        private boolean stacktrace = false;

        @Parameter(names = "--help", help = true, description = "Prints this help message")
        private boolean help = false;
    }

    @Parameters(commandDescription = "Installs the solver")
    static class InstallCommand {
        static final String COMMAND = "install";

        @Parameter(description = "The solver to install (<solver_name>:<solver_version>)", validateWith = SolverNameAndVersionValidator.class, required = true)
        String solver;

        @Parameter(names = "--name", description = "Install the solver version under this custom name (<solver_name>:<name>), instead of the default (<solver_name>:<solver_version>)")
        String name;

        @Parameter(names = "--solver-path", description = "The path of the solver to install. The solver will not be downloaded, instead the binary on this path will be used. Caveat emptor: the version must be specified correctly, there is no automatic detection.")
        String solverPath;

        @Parameter(names = "--tempt-murphy", description = "Allows the installation of unsupported solver version")
        boolean temptMurphy = false;
    }

    @Parameters(commandDescription = "Installs a generic solver")
    static class InstallGenericCommand {
        static final String COMMAND = "install-generic";

        @Parameter(names = "--solver-path", description = "The path of the generic solver to install", required = true)
        String solverPath;

        @Parameter(names = "--solver-args", description = "The arguments of the generic solver to invoke with")
        String solverArgs;

        @Parameter(names = "--name", description = "Install the solver version under this custom name (<solver_name>:<name>), instead of the default (<solver_name>:<solver_version>)", required = true)
        String name;
    }

    @Parameters(commandDescription = "Uninstalls the solver")
    static class UninstallCommand {
        static final String COMMAND = "uninstall";

        @Parameter(description = "The solver to uninstall (<solver_name>:<solver_version>)", validateWith = SolverNameAndVersionValidator.class, required = true)
        String solver;
    }

    @Parameters(commandDescription = "Renames one installed solver version")
    static class RenameCommand {
        static final String COMMAND = "rename";

        @Parameter(description = "The solver to reinstall (<solver_name>:<solver_version>)", validateWith = SolverNameAndVersionValidator.class, required = true)
        String solver;

        @Parameter(names = "--name", description = "Rename the solver version to this custom name (<solver_name>:<name>).", required = true)
        String name;
    }

    @Parameters(commandDescription = "Prints info about the solver")
    static class GetInfoCommand {
        static final String COMMAND = "get-info";

        @Parameter(description = "The solver to print info about (<solver_name>:<solver_version>)", validateWith = SolverNameAndVersionValidator.class, required = true)
        String solver;
    }

    @Parameters(commandDescription = "Edits the runtime arguments passed to the solver")
    static class EditArgsCommand {
        static final String COMMAND = "edit-args";

        @Parameter(description = "The solver, whose runtime arguments are to be edited (<solver_name>:<solver_version>)", validateWith = SolverNameAndVersionValidator.class, required = true)
        String solver;

        @Parameter(names = "--print", description = "Print the path instead of opening it for editing")
        boolean print = false;
    }

    @Parameters(commandDescription = "Lists installed solvers and their versions")
    static class ListInstalledCommand {
        static final String COMMAND = "list-installed";

        @Parameter(description = "The solver, whose installed versions are to be listed (<solver_name>)", validateWith = SolverNameValidator.class)
        String solver;
    }

    @Parameters(commandDescription = "Lists supported solvers and their versions")
    static class ListSupportedCommand {
        static final String COMMAND = "list-supported";

        @Parameter(description = "The solver, whose supported versions are to be listed (<solver_name>)", validateWith = SolverNameValidator.class)
        String solver;
    }

    public SmtLibCli(final String[] args) {
        this.args = args;
    }

    public static void main(final String[] args) {
        final SmtLibCli mainApp = new SmtLibCli(args);
        mainApp.run();
    }

    private void run() {
        final var mainParams = new MainParams();
        final var installCommand = new InstallCommand();
        final var installGenericCommand = new InstallGenericCommand();
        final var uninstallCommand = new UninstallCommand();
        final var renameCommand = new RenameCommand();
        final var getInfoCommand = new GetInfoCommand();
        final var editArgsCommand = new EditArgsCommand();
        final var listInstalledCommand = new ListInstalledCommand();
        final var listSupportedCommand = new ListSupportedCommand();

        final var jc = JCommander.newBuilder()
            .addObject(mainParams)
            .addCommand(InstallCommand.COMMAND, installCommand)
            .addCommand(InstallGenericCommand.COMMAND, installGenericCommand)
            .addCommand(UninstallCommand.COMMAND, uninstallCommand)
            .addCommand(RenameCommand.COMMAND, renameCommand)
            .addCommand(GetInfoCommand.COMMAND, getInfoCommand)
            .addCommand(EditArgsCommand.COMMAND, editArgsCommand)
            .addCommand(ListInstalledCommand.COMMAND, listInstalledCommand)
            .addCommand(ListSupportedCommand.COMMAND, listSupportedCommand)
            .programName(JAR_NAME)
            .build();

        try {
            jc.parse(args);
            logger = new ConsoleLogger(mainParams.logLevel);
        } catch (final ParameterException ex) {
            System.out.println("Invalid parameters, details:");
            System.out.println(ex.getMessage());
            ex.usage();
            return;
        }

        if(mainParams.help) {
            jc.usage();
            return;
        }

        try {
            final var homePath = createIfNotExists(Path.of(mainParams.home));
            final var smtLibSolverManager = SmtLibSolverManager.create(homePath, logger);

            if(jc.getParsedCommand() == null) {
                logger.write(Logger.Level.RESULT, "Missing command\n");
                jc.usage();
                return;
            }

            switch(jc.getParsedCommand()) {
                case InstallCommand.COMMAND: {
                    final var solver = decodeVersionString(installCommand.solver);

                    if(solver.get1().equals(smtLibSolverManager.getGenericInstallerName())) {
                        logger.write(Logger.Level.RESULT, "To install a generic solver, use the \"%s\" command", InstallGenericCommand.COMMAND);
                        return;
                    }

                    if(installCommand.name != null) {
                        smtLibSolverManager.install(solver.get1(), solver.get2(), installCommand.name, installCommand.solverPath != null ? Path.of(installCommand.solverPath) : null, installCommand.temptMurphy);
                    }
                    else {
                        smtLibSolverManager.install(solver.get1(), solver.get2(), solver.get2(), installCommand.solverPath != null ? Path.of(installCommand.solverPath) : null, installCommand.temptMurphy);
                    }

                    return;
                }
                case InstallGenericCommand.COMMAND: {
                    smtLibSolverManager.installGeneric(
                        installGenericCommand.name,
                        Path.of(installGenericCommand.solverPath),
                        (installGenericCommand.solverArgs == null ? "" : installGenericCommand.solverArgs).split(" ")
                    );
                    return;
                }
                case UninstallCommand.COMMAND: {
                    final var solver = decodeVersionString(uninstallCommand.solver);
                    smtLibSolverManager.uninstall(solver.get1(), solver.get2());
                    return;
                }
                case RenameCommand.COMMAND: {
                    final var solver = decodeVersionString(renameCommand.solver);
                    smtLibSolverManager.rename(solver.get1(), solver.get2(), renameCommand.name);
                    return;
                }
                case GetInfoCommand.COMMAND: {
                    final var solver = decodeVersionString(getInfoCommand.solver);
                    final var info = smtLibSolverManager.getInfo(solver.get1(), solver.get2());
                    logger.write(Logger.Level.RESULT, "%s\n", info);
                    return;
                }
                case EditArgsCommand.COMMAND: {
                    final var solver = decodeVersionString(editArgsCommand.solver);
                    final var argsFilePath = smtLibSolverManager.getArgsFile(solver.get1(), solver.get2());

                    if(editArgsCommand.print) {
                        logger.write(Logger.Level.RESULT, String.format("%s\n", argsFilePath.toAbsolutePath().toString()));
                    }
                    else if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                        Desktop.getDesktop().edit(argsFilePath.toFile());
                    }
                    else {
                        logger.write(Logger.Level.MAINSTEP, "Open the following text file in your favourite editor, and edit the content:\n");
                        logger.write(Logger.Level.RESULT, String.format("%s\n", argsFilePath.toAbsolutePath().toString()));
                    }

                    return;
                }
                case ListInstalledCommand.COMMAND: {
                    if(listInstalledCommand.solver != null) {
                        logger.write(Logger.Level.MAINSTEP, "The currently installed versions of solver %s are: \n", listInstalledCommand.solver);
                        smtLibSolverManager.getInstalledVersions(listInstalledCommand.solver).forEach(version -> {
                            logger.write(Logger.Level.RESULT, "\t%s:%s\n", listInstalledCommand.solver, version);
                        });
                    }
                    else {
                        logger.write(Logger.Level.MAINSTEP, "The currently installed solvers are: \n");
                        smtLibSolverManager.getInstalledVersions().forEach(solver -> {
                            solver.get2().forEach(version -> {
                                logger.write(Logger.Level.RESULT, "\t%s:%s\n", solver.get1(), version);
                            });
                        });
                    }
                    return;
                }
                case ListSupportedCommand.COMMAND: {
                    if(listSupportedCommand.solver != null) {
                        logger.write(Logger.Level.MAINSTEP, "The currently supported versions of solver %s are: \n", listSupportedCommand.solver);
                        smtLibSolverManager.getSupportedVersions(listSupportedCommand.solver).forEach(version -> {
                            logger.write(Logger.Level.RESULT, "\t%s:%s\n", listSupportedCommand.solver, version);
                        });
                    }
                    else {
                        logger.write(Logger.Level.MAINSTEP, "The currently supported solvers are: \n");
                        smtLibSolverManager.getSupportedVersions().forEach(solver -> {
                            solver.get2().forEach(version -> {
                                logger.write(Logger.Level.RESULT, "\t%s:%s\n", solver.get1(), version);
                            });
                        });
                    }
                    return;
                }
                default: {
                    logger.write(Logger.Level.RESULT, "Unknown command\n");
                    jc.usage();
                    return;
                }
            }
        }
        catch (SmtLibSolverInstallerException e) {
            logger.write(Logger.Level.RESULT, "%s\n", e.getMessage());
            if(mainParams.stacktrace) {
                printError(e, true);
            }
        }
        catch (IOException e) {
            printError(e, mainParams.stacktrace);
        }
    }

    private static Tuple2<String, String> decodeVersionString(final String version) {
        final var versionArr = version.split(":");

        if(versionArr.length != 2) {
            throw new IllegalArgumentException("Invalid version string: " + version);
        }

        return Tuple2.of(versionArr[0], versionArr[1]);
    }

    private Path createIfNotExists(final Path path) throws IOException {
        if(!Files.exists(path)) {
            Files.createDirectory(path);
        }
        return path;
    }

    private void printError(final Throwable ex, final boolean printStackTrace) {
        final String message = ex.getMessage() == null ? "" : ex.getMessage();
        logger.write(Logger.Level.RESULT, "%s occurred, message: %s%n", ex.getClass().getSimpleName(), message);
        if (printStackTrace) {
            final StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.write(Logger.Level.RESULT, "Trace:%n%s%n", errors.toString());
        }
        else {
            logger.write(Logger.Level.RESULT, "Use --stacktrace for stack trace%n");
        }
    }

    public static class SolverNameValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            if(!value.matches("[a-zA-Z0-9]+")) {
                throw new ParameterException(
                    String.format("Invalid solver name in parameter %s", name)
                );
            }
        }
    }

    public static class SolverNameAndVersionValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            final var versionArr = value.split(":");

            if(versionArr.length != 2) {
                throw new ParameterException(String.format("Invalid version string %s in parameter %s", value, name));
            }

            if(!versionArr[0].matches("[a-zA-Z0-9]+") || !versionArr[1].matches("[a-zA-Z0-9-._]+")) {
                throw new ParameterException(
                    String.format("Invalid version string %s in parameter %s", value, name)
                );
            }
        }
    }
}
