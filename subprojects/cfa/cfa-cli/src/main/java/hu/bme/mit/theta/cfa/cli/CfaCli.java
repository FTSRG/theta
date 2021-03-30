/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.cfa.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult.Unsafe;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarStatistics;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.cfa.analysis.CfaTraceConcretizer;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfig;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Domain;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Encoding;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.InitPrec;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.PrecGranularity;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.PredSplit;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Refinement;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Search;
import hu.bme.mit.theta.cfa.analysis.utils.CfaVisualizer;
import hu.bme.mit.theta.cfa.dsl.CfaDslManager;
import hu.bme.mit.theta.common.CliUtils;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.Logger.Level;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.common.table.BasicTableWriter;
import hu.bme.mit.theta.common.table.TableWriter;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.smtlib.SmtLibSolverManager;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A command line interface for running a CEGAR configuration on a CFA.
 */
public class CfaCli {
	private static final String JAR_NAME = "theta-cfa-cli.jar";
	private final String[] args;
	private final TableWriter writer;

	@Parameter(names = "--domain", description = "Abstract domain")
	Domain domain = Domain.PRED_CART;

	@Parameter(names = "--refinement", description = "Refinement strategy")
	Refinement refinement = Refinement.SEQ_ITP;

	@Parameter(names = "--search", description = "Search strategy")
	Search search = Search.BFS;

	@Parameter(names = "--predsplit", description = "Predicate splitting (for predicate abstraction)")
	PredSplit predSplit = PredSplit.WHOLE;

	@Parameter(names = "--solver", description = "Sets the underlying SMT solver to use for both the abstraction and the refinement process. Enter in format <solver_name>:<solver_version>, see theta-smtlib-cli.jar for more details. Enter \"Z3\" to use the legacy z3 solver.")
	String solver = "Z3";

	@Parameter(names = "--abstraction-solver", description = "Sets the underlying SMT solver to use for the abstraction process. Enter in format <solver_name>:<solver_version>, see theta-smtlib-cli.jar for more details. Enter \"Z3\" to use the legacy z3 solver.")
	String abstractionSolver;

	@Parameter(names = "--refinement-solver", description = "Sets the underlying SMT solver to use for the refinement process. Enter in format <solver_name>:<solver_version>, see theta-smtlib-cli.jar for more details. Enter \"Z3\" to use the legacy z3 solver.")
	String refinementSolver;

	@Parameter(names = "--home", description = "The path of the solver registry")
	String home = Path.of(System.getProperty("user.home"), ".theta").toAbsolutePath().toString();

	@Parameter(names = "--model", description = "Path of the input CFA model", required = true)
	String model;

	@Parameter(names = "--errorloc", description = "Error (target) location")
	String errorLoc = "";

	@Parameter(names = "--precgranularity", description = "Precision granularity")
	PrecGranularity precGranularity = PrecGranularity.GLOBAL;

	@Parameter(names = "--encoding", description = "Block encoding")
	Encoding encoding = Encoding.LBE;

	@Parameter(names = "--maxenum", description = "Maximal number of explicitly enumerated successors (0: unlimited)")
	Integer maxEnum = 10;

	@Parameter(names = "--initprec", description = "Initial precision of abstraction")
	InitPrec initPrec = InitPrec.EMPTY;

	@Parameter(names = "--prunestrategy", description = "Strategy for pruning the ARG after refinement")
	PruneStrategy pruneStrategy = PruneStrategy.LAZY;

	@Parameter(names = "--loglevel", description = "Detailedness of logging")
	Logger.Level logLevel = Level.SUBSTEP;

	@Parameter(names = "--benchmark", description = "Benchmark mode (only print metrics)")
	Boolean benchmarkMode = false;

	@Parameter(names = "--cex", description = "Write concrete counterexample to a file")
	String cexfile = null;

	@Parameter(names = "--header", description = "Print only a header (for benchmarks)", help = true)
	boolean headerOnly = false;

	@Parameter(names = "--visualize", description = "Visualize CFA to this file without running the algorithm")
	String visualize = null;

	@Parameter(names = "--metrics", description = "Print metrics about the CFA without running the algorithm")
	boolean metrics = false;

	@Parameter(names = "--stacktrace", description = "Print full stack trace in case of exception")
	boolean stacktrace = false;

	@Parameter(names = "--version", description = "Display version", help = true)
	boolean versionInfo = false;

	private Logger logger;

	public CfaCli(final String[] args) {
		this.args = args;
		writer = new BasicTableWriter(System.out, ",", "\"", "\"");
	}

	public static void main(final String[] args) {
		final CfaCli mainApp = new CfaCli(args);
		mainApp.run();
	}

	private void run() {
		try {
			JCommander.newBuilder().addObject(this).programName(JAR_NAME).build().parse(args);
			logger = benchmarkMode ? NullLogger.getInstance() : new ConsoleLogger(logLevel);
		} catch (final ParameterException ex) {
			System.out.println("Invalid parameters, details:");
			System.out.println(ex.getMessage());
			ex.usage();
			return;
		}

		if (headerOnly) {
			printHeader();
			return;
		}

		if (versionInfo) {
			CliUtils.printVersion(System.out);
			return;
		}

		try {
			final var homePath = Path.of(home);
			final var smtLibSolverManager = SmtLibSolverManager.create(homePath, logger);

			final Stopwatch sw = Stopwatch.createStarted();
			final CFA cfa = loadModel();

			if (visualize != null) {
				final Graph graph = CfaVisualizer.visualize(cfa);
				GraphvizWriter.getInstance().writeFileAutoConvert(graph, visualize);
				return;
			}

			if (metrics) {
				CfaMetrics.printMetrics(logger, cfa);
				return;
			}

			CFA.Loc errLoc = null;
			if (cfa.getErrorLoc().isPresent()) {
				errLoc = cfa.getErrorLoc().get();
			}
			if (!errorLoc.isEmpty()) {
				errLoc = null;
				for (CFA.Loc running : cfa.getLocs()) {
					if (running.getName().equals(errorLoc)) {
						errLoc = running;
					}
				}
				checkNotNull(errLoc, "Location '" + errorLoc + "' not found in CFA");
			}
			checkNotNull(errLoc, "Error location must be specified in CFA or as argument");

			final SolverFactory abstractionSolverFactory;
			if(abstractionSolver != null && !abstractionSolver.equals("Z3")) {
				final var s = abstractionSolver.split("\\.");
				abstractionSolverFactory = smtLibSolverManager.getSolverFactory(s[0], s[1]);
			}
			else if(!solver.equals("Z3")) {
				final var s = solver.split("\\.");
				abstractionSolverFactory = smtLibSolverManager.getSolverFactory(s[0], s[1]);
			}
			else {
				abstractionSolverFactory = Z3SolverFactory.getInstance();
			}


			final SolverFactory refinementSolverFactory;
			if(refinementSolver != null && !refinementSolver.equals("Z3")) {
				final var s = refinementSolver.split("\\.");
				refinementSolverFactory = smtLibSolverManager.getSolverFactory(s[0], s[1]);
			}
			else if(!solver.equals("Z3")) {
				final var s = solver.split("\\.");
				refinementSolverFactory = smtLibSolverManager.getSolverFactory(s[0], s[1]);
			}
			else {
				refinementSolverFactory = Z3SolverFactory.getInstance();
			}

			final CfaConfig<?, ?, ?> configuration = buildConfiguration(cfa, errLoc, abstractionSolverFactory, refinementSolverFactory);
			final SafetyResult<?, ?> status = check(configuration);
			sw.stop();
			printResult(status, sw.elapsed(TimeUnit.MILLISECONDS));
			if (status.isUnsafe() && cexfile != null) {
				writeCex(status.asUnsafe());
			}
		} catch (final Throwable ex) {
			printError(ex);
			System.exit(1);
		}
	}

	private void printHeader() {
		Stream.of("Result", "TimeMs", "AlgoTimeMs", "AbsTimeMs", "RefTimeMs", "Iterations",
				"ArgSize", "ArgDepth", "ArgMeanBranchFactor", "CexLen").forEach(writer::cell);
		writer.newRow();
	}

	private CFA loadModel() throws Exception {
		try (InputStream inputStream = new FileInputStream(model)) {
			try {
				return CfaDslManager.createCfa(inputStream);
			} catch (final Exception ex) {
				throw new Exception("Could not parse CFA: " + ex.getMessage(), ex);
			}
		}
	}

	private CfaConfig<?, ?, ?> buildConfiguration(final CFA cfa, final CFA.Loc errLoc, final SolverFactory abstractionSolverFactory, final SolverFactory refinementSolverFactory) throws Exception {
		try {
			return new CfaConfigBuilder(domain, refinement, abstractionSolverFactory, refinementSolverFactory)
					.precGranularity(precGranularity).search(search)
					.predSplit(predSplit).encoding(encoding).maxEnum(maxEnum).initPrec(initPrec)
					.pruneStrategy(pruneStrategy).logger(logger).build(cfa, errLoc);
		} catch (final Exception ex) {
			throw new Exception("Could not create configuration: " + ex.getMessage(), ex);
		}
	}

	private SafetyResult<?, ?> check(CfaConfig<?, ?, ?> configuration) throws Exception {
		try {
			return configuration.check();
		} catch (final Exception ex) {
			String message = ex.getMessage() == null ? "(no message)" : ex.getMessage();
			throw new Exception("Error while running algorithm: " + ex.getClass().getSimpleName() + " " + message, ex);
		}
	}

	private void printResult(final SafetyResult<?, ?> status, final long totalTimeMs) {
		final CegarStatistics stats = (CegarStatistics) status.getStats().get();
		if (benchmarkMode) {
			writer.cell(status.isSafe());
			writer.cell(totalTimeMs);
			writer.cell(stats.getAlgorithmTimeMs());
			writer.cell(stats.getAbstractorTimeMs());
			writer.cell(stats.getRefinerTimeMs());
			writer.cell(stats.getIterations());
			writer.cell(status.getArg().size());
			writer.cell(status.getArg().getDepth());
			writer.cell(status.getArg().getMeanBranchingFactor());
			if (status.isUnsafe()) {
				writer.cell(status.asUnsafe().getTrace().length() + "");
			} else {
				writer.cell("");
			}
			writer.newRow();
		}
	}

	private void printError(final Throwable ex) {
		final String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (benchmarkMode) {
			writer.cell("[EX] " + ex.getClass().getSimpleName() + ": " + message);
			writer.newRow();
		} else {
			logger.write(Level.RESULT, "%s occurred, message: %s%n", ex.getClass().getSimpleName(), message);
			if (stacktrace) {
				final StringWriter errors = new StringWriter();
				ex.printStackTrace(new PrintWriter(errors));
				logger.write(Level.RESULT, "Trace:%n%s%n", errors.toString());
			} else {
				logger.write(Level.RESULT, "Use --stacktrace for stack trace%n");
			}
		}
	}

	private void writeCex(final Unsafe<?, ?> status) throws FileNotFoundException {
		@SuppressWarnings("unchecked") final Trace<CfaState<?>, CfaAction> trace = (Trace<CfaState<?>, CfaAction>) status.getTrace();
		final Trace<CfaState<ExplState>, CfaAction> concrTrace = CfaTraceConcretizer.concretize(trace, Z3SolverFactory.getInstance());
		final File file = new File(cexfile);
		PrintWriter printWriter = null;
		try {
			printWriter = new PrintWriter(file);
			printWriter.write(concrTrace.toString());
		} finally {
			if (printWriter != null) {
				printWriter.close();
			}
		}
	}
}
