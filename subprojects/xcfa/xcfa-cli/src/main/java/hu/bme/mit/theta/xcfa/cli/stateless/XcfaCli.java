/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.bme.mit.theta.xcfa.cli.stateless;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.cfa.analysis.CfaTraceConcretizer;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfig;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder;
import hu.bme.mit.theta.common.CliUtils;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xcfa.XcfaUtils;
import hu.bme.mit.theta.xcfa.model.XCFA;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class XcfaCli {
	private static final String JAR_NAME = "theta-xcfa-cli.jar";
	private final String[] args;

	@Parameter(names = "--model", description = "Path of the input XCFA model", required = true)
	File model;

	@Parameter(names = "--print-xcfa", description = "Print XCFA (as a dotfile) and exit.")
	boolean printxcfa;

	@Parameter(names = "--print-cfa", description = "Print CFA and exit.")
	boolean printcfa;

	@Parameter(names = "--timeout", description = "Seconds until timeout (not precise)")
	Long timeS = Long.MAX_VALUE;

	@Parameter(names = "--version", description = "Display version", help = true)
	boolean versionInfo = false;

	@Parameter(names = "--cex", description = "Write concrete counterexample to a file")
	String cexfile = null;

	public XcfaCli(final String[] args) {
		this.args = args;
	}

	public static void main(final String[] args) {
		final XcfaCli mainApp = new XcfaCli(args);
		mainApp.run();
	}

	private void run() {
		try {
			JCommander.newBuilder().addObject(this).programName(JAR_NAME).build().parse(args);
		} catch (final ParameterException ex) {
			System.out.println("Invalid parameters, details:");
			System.out.println(ex.getMessage());
			ex.usage();
			return;
		}

		if (versionInfo) {
			CliUtils.printVersion(System.out);
			return;
		}

		try {
			final Stopwatch sw = Stopwatch.createStarted();
			final XCFA xcfa = XcfaUtils.fromFile(model);

			if (printxcfa) {
				System.out.println(xcfa.toDot());
				return;
			} else if (printcfa) {
				CFA cfa = xcfa.createCFA();
				File cfafile = new File(model.getAbsolutePath() + ".cfa");
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(cfafile))) {
					bw.write(cfa.toString());
				}
				System.out.println("PARSING SUCCESSFUL");
				System.out.println("CFA-data name " + model.getName().split("\\.")[0]);
				System.out.println("CFA-data varCount " + cfa.getVars().size());
				System.out.println("CFA-data varCount " + cfa.getLocs().size());
				return;
			}

			CFA cfa = xcfa.createCFA();
			final CfaConfig<?, ?, ?> configuration = buildConfiguration(cfa, cfa.getErrorLoc().get());
			final SafetyResult<?, ?> status = check(configuration);

			if (status.isUnsafe() && cexfile != null) {
				writeCex(status.asUnsafe());
			}

			sw.stop();
			System.out.println(sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
		} catch (final Throwable ex) {
			ex.printStackTrace();
		}
	}
	private CfaConfig<?, ?, ?> buildConfiguration(final CFA cfa, final CFA.Loc errLoc) throws Exception {
		try {
			return new CfaConfigBuilder(CfaConfigBuilder.Domain.PRED_CART, CfaConfigBuilder.Refinement.SEQ_ITP, Z3SolverFactory.getInstance())
					.precGranularity(CfaConfigBuilder.PrecGranularity.GLOBAL).search(CfaConfigBuilder.Search.BFS)
					.predSplit(CfaConfigBuilder.PredSplit.WHOLE).encoding(CfaConfigBuilder.Encoding.LBE).maxEnum(10).initPrec(CfaConfigBuilder.InitPrec.EMPTY)
					.pruneStrategy(PruneStrategy.LAZY).logger(new ConsoleLogger(Logger.Level.SUBSTEP)).build(cfa, errLoc);
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

	private void writeCex(final SafetyResult.Unsafe<?, ?> status) throws FileNotFoundException {
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
