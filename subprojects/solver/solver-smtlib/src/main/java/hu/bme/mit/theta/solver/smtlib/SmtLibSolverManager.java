package hu.bme.mit.theta.solver.smtlib;

import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.smtlib.impl.boolector.BoolectorSmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.impl.princess.PrincessSmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.impl.smtinterpol.SMTInterpolSmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.impl.yices2.Yices2SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.solver.installer.SmtLibSolverInstallerException;
import hu.bme.mit.theta.solver.smtlib.impl.cvc4.CVC4SmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.impl.mathsat.MathSATSmtLibSolverInstaller;
import hu.bme.mit.theta.solver.smtlib.impl.z3.Z3SmtLibSolverInstaller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class SmtLibSolverManager {
    private static final Map<String, Class<? extends SmtLibSolverInstaller>> installerDeclarations = new HashMap<>();
    private static Tuple2<String, Class<? extends GenericSmtLibSolverInstaller>> genericInstallerDeclaration;

    public static <T extends SmtLibSolverInstaller> void registerInstaller(final String name, final Class<T> decl) {
        installerDeclarations.put(name, decl);
    }

    public static <T extends GenericSmtLibSolverInstaller> void registerGenericInstaller(final String name, final Class<T> decl) {
        checkState(genericInstallerDeclaration == null);
        genericInstallerDeclaration = Tuple2.of(name, decl);
    }

    static {
        registerInstaller("z3", Z3SmtLibSolverInstaller.class);
        registerInstaller("cvc4", CVC4SmtLibSolverInstaller.class);
        registerInstaller("mathsat", MathSATSmtLibSolverInstaller.class);
        registerInstaller("yices2", Yices2SmtLibSolverInstaller.class);
        registerInstaller("boolector", BoolectorSmtLibSolverInstaller.class);
        registerInstaller("smtinterpol", SMTInterpolSmtLibSolverInstaller.class);
        registerInstaller("princess", PrincessSmtLibSolverInstaller.class);
        registerGenericInstaller("generic", GenericSmtLibSolverInstaller.class);
    }

    private final Path home;
    private final Logger logger;

    private final Map<String, SmtLibSolverInstaller> installers;
    private final Tuple2<String, GenericSmtLibSolverInstaller> genericInstaller;

    private SmtLibSolverManager(final Path home, final Logger logger) {
        this.logger = logger;
        checkNotNull(home);
        checkArgument(Files.exists(home), "Home directory does not exist");

        this.home = home;

        try {
            this.genericInstaller = Tuple2.of(
                genericInstallerDeclaration.get1(),
                genericInstallerDeclaration.get2().getDeclaredConstructor(Logger.class).newInstance(logger)
            );

            this.installers = Stream.concat(
                Stream.of(this.genericInstaller),
                installerDeclarations.entrySet().stream()
                    .map(p -> {
                        try {
                            return Tuple2.of(p.getKey(), p.getValue().getDeclaredConstructor(Logger.class).newInstance(logger));
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    })
            ).collect(Collectors.toUnmodifiableMap(Tuple2::get1, Tuple2::get2));
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    public static SmtLibSolverManager create(final Path home, final Logger logger) throws IOException {
        createIfNotExists(home);
        return new SmtLibSolverManager(home, logger);
    }

    public String getGenericInstallerName() {
        return genericInstaller.get1();
    }

    public void install(final String solver, final String version, final String name, final Path solverPath, final boolean installUnsupported) throws SmtLibSolverInstallerException {
        checkArgument(!solver.equals(genericInstaller.get1()));

        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }
        else if(!getSupportedVersions(solver).contains(getVersionString(solver, version, false)) && !installUnsupported) {
            throw new SmtLibSolverInstallerException("Installing unsupported solvers is not enabled");
        }

        final var installDir = home.resolve(solver);
        try {
            if (!Files.exists(installDir)) {
                Files.createDirectory(installDir);
            }
        } catch (IOException e) {
            throw new SmtLibSolverInstallerException(e);
        }

        if(solverPath != null) {
            installers.get(solver).install(installDir, getVersionString(solver, version, false), getVersionString(solver, name, false), solverPath);
        }
        else {
            installers.get(solver).install(installDir, getVersionString(solver, version, false), getVersionString(solver, name, false));
        }
    }

    public void installGeneric(final String version, final Path solverPath, final String[] args) throws SmtLibSolverInstallerException {
        final var installDir = home.resolve(genericInstaller.get1());
        try {
            if (!Files.exists(installDir)) {
                Files.createDirectory(installDir);
            }
        } catch (IOException e) {
            throw new SmtLibSolverInstallerException(e);
        }
        genericInstaller.get2().install(installDir, version, solverPath, args);
    }

    public void uninstall(final String solver, final String version) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        installers.get(solver).uninstall(home.resolve(solver), getVersionString(solver, version, true));
    }

    public void rename(final String solver, final String version, final String name) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        installers.get(solver).rename(home.resolve(solver), getVersionString(solver, version, true), name);
    }

    public String getInfo(final String solver, final String version) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        return installers.get(solver).getInfo(home.resolve(solver), getVersionString(solver, version, true));
    }

    public Path getArgsFile(final String solver, final String version) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        return installers.get(solver).getArgsFile(home.resolve(solver), getVersionString(solver, version, true));
    }

    public SolverFactory getSolverFactory(final String solver, final String version) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        return installers.get(solver).getSolverFactory(home.resolve(solver), getVersionString(solver, version, true));
    }

    public List<String> getSupportedSolvers() {
        return installers.keySet().stream().collect(Collectors.toUnmodifiableList());
    }

    public List<Tuple2<String, List<String>>> getSupportedVersions() throws SmtLibSolverInstallerException {
        final var builder = ImmutableList.<Tuple2<String, List<String>>>builder();

        for(final var solver : getSupportedSolvers()) {
            builder.add(Tuple2.of(solver, getSupportedVersions(solver)));
        }

        return builder.build();
    }

    public List<String> getSupportedVersions(final String solver) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        return installers.get(solver).getSupportedVersions();
    }

    public List<Tuple2<String, List<String>>> getInstalledVersions() throws SmtLibSolverInstallerException {
        final var builder = ImmutableList.<Tuple2<String, List<String>>>builder();

        for(final var solver : getSupportedSolvers()) {
            builder.add(Tuple2.of(solver, getInstalledVersions(solver)));
        }

        return builder.build();
    }

    public List<String> getInstalledVersions(final String solver) throws SmtLibSolverInstallerException {
        if(!installers.containsKey(solver)) {
            throw new SmtLibSolverInstallerException(String.format("Unknown solver: %s", solver));
        }

        return installers.get(solver).getInstalledVersions(home.resolve(solver));
    }

    private String getVersionString(final String solver, final String version, final boolean installed) throws SmtLibSolverInstallerException {
        if(!version.equals("latest")) {
            return version;
        }
        else {
            final var supportedVersions = getSupportedVersions(solver);
            final var versions = installed ? getInstalledVersions(solver).stream().filter(supportedVersions::contains).collect(Collectors.toList()) : supportedVersions;
            if(versions.size() > 0) {
                return versions.get(0);
            }
            else {
                throw new SmtLibSolverInstallerException(String.format("There are no %s versions of solver: %s", installed ? "installed" : "supported", solver));
            }
        }
    }

    private static Path createIfNotExists(final Path path) throws IOException {
        if(!Files.exists(path)) {
            Files.createDirectory(path);
        }
        return path;
    }
}
