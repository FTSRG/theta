package hu.bme.mit.theta.xta.testgen;

import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.common.logging.Logger;

import java.util.Set;

public class XtaTestPrinter {
	public static void printTests(Set<? extends XtaTest<?, ?>> tests, Logger logger) {
		for (XtaTest<?, ?> test : tests)
			printTest(test, logger);
	}

	public static void printTest(XtaTest<?, ?> test, Logger logger) {
		logger.write(Logger.Level.RESULT, "%n========== %s ==========%n", test.getName());
		ArgTrace<?,?> trace = test.getTrace();

		printState(test, 0, logger);
		for (int i = 0; i < trace.edges().size(); i++) {
			logger.write(Logger.Level.RESULT, trace.edges().get(i).getAction().toString() + "\n");
			printState(test, i+1, logger);
		}
		logger.write(Logger.Level.RESULT, "Total time: %f%n", test.getTotalTime());
	}

	private static void printState(XtaTest<?, ?> test, int idx, Logger logger) {
		logger.write(Logger.Level.RESULT, test.getTrace().nodes().get(idx).getState().toString() + "\n");
		logger.write(Logger.Level.RESULT, "Delay: %f%n", test.getDelays().get(idx));
	}
}
