package hu.bme.mit.theta.xta.analysis.lazy;

import com.google.common.base.Stopwatch;
import hu.bme.mit.theta.analysis.algorithm.Statistics;
import hu.bme.mit.theta.common.table.TableWriter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class LazyXtaTestgenStatistics extends Statistics {
	private final LazyXtaStatistics stats;

	private final Stopwatch testgenTimer;
	private long testCasesGenerated;
	private long testCases;
	private long testCasesTotalLength;

	public LazyXtaTestgenStatistics(LazyXtaStatistics stats) {
		this.stats = stats;

		testgenTimer = Stopwatch.createUnstarted();
		testCases = 0;
		testCasesTotalLength = 0;
		testCasesGenerated = 0;

		addStat("AlgorithmTimeInMs", this::getAlgorithmTimeInMs);
		addStat("ExpandTimeInMs", this::getExpandTimeInMs);
		addStat("CloseTimeInMs", this::getCloseTimeInMs);
		addStat("ExpandExplRefinementTimeInMs", this::getExpandExplRefinementTimeInMs);
		addStat("ExpandZoneRefinementTimeInMs", this::getExpandZoneRefinementTimeInMs);
		addStat("CloseExplRefinementTimeInMs", this::getCloseExplRefinementTimeInMs);
		addStat("CloseZoneRefinementTimeInMs", this::getCloseZoneRefinementTimeInMs);
		addStat("CoverageChecks", this::getCoverageChecks);
		addStat("CoverageAttempts", this::getCoverageAttempts);
		addStat("CoverageSuccesses", this::getCoverageSuccesses);
		addStat("ExplRefinementSteps", this::getExplRefinementSteps);
		addStat("ZoneRefinementSteps", this::getZoneRefinementSteps);
		addStat("ArgDepth", this::getArgDepth);
		addStat("ArgNodes", this::getArgNodes);
		addStat("ArgNodesExpanded", this::getArgNodesExpanded);

		addStat("TestCasesGenerated", this::getTestCasesGenerated);
		addStat("TestgenTimeInMs", this::getTestgenTimeInMs);
		addStat("TestCases", this::getTestCases);
		addStat("TestCasesTotalLength", this::getTestCasesTotalLength);
	}

	public long getAlgorithmTimeInMs() { return stats.getAlgorithmTimeInMs(); }

	public long getExpandTimeInMs() {
		return stats.getExpandTimeInMs();
	}

	public long getCloseTimeInMs() {
		return stats.getCloseTimeInMs();
	}

	public long getExpandExplRefinementTimeInMs() {
		return stats.getExpandExplRefinementTimeInMs();
	}

	public long getExpandZoneRefinementTimeInMs() {
		return stats.getExpandZoneRefinementTimeInMs();
	}

	public long getCloseExplRefinementTimeInMs() {
		return stats.getCloseExplRefinementTimeInMs();
	}

	public long getCloseZoneRefinementTimeInMs() {
		return stats.getCloseZoneRefinementTimeInMs();
	}

	public long getCoverageChecks() {
		return stats.getCoverageChecks();
	}

	public long getCoverageAttempts() {
		return stats.getCoverageAttempts();
	}

	public long getCoverageSuccesses() {
		return stats.getCoverageSuccesses();
	}

	public long getExplRefinementSteps() {
		return stats.getExplRefinementSteps();
	}

	public long getZoneRefinementSteps() {
		return stats.getZoneRefinementSteps();
	}

	public long getArgDepth() {
		return stats.getArgDepth();
	}

	public long getArgNodes() {
		return stats.getArgNodes();
	}

	public long getArgNodesExpanded() {
		return stats.getArgNodesExpanded();
	}


	public long getTestCasesGenerated() { return testCasesGenerated; }

	public long getTestgenTimeInMs() { return testgenTimer.elapsed(MILLISECONDS); }

	public long getTestCases() { return testCases; }

	public long getTestCasesTotalLength() { return testCasesTotalLength; }

	public static void writeHeader(final TableWriter writer) {
		writer.cell("AlgorithmTimeInMs");
		writer.cell("ExpandTimeInMs");
		writer.cell("CloseTimeInMs");
		writer.cell("ExpandExplRefinementTimeInMs");
		writer.cell("ExpandZoneRefinementTimeInMs");
		writer.cell("CloseExplRefinementTimeInMs");
		writer.cell("CloseZoneRefinementTimeInMs");
		writer.cell("CoverageChecks");
		writer.cell("CoverageAttempts");
		writer.cell("CoverageSuccesses");
		writer.cell("ExplRefinementSteps");
		writer.cell("ZoneRefinementSteps");
		writer.cell("ArgDepth");
		writer.cell("ArgNodes");
		writer.cell("ArgNodesExpanded");

		writer.cell("TestCasesGenerated");
		writer.cell("TestgenTimeInMs");
		writer.cell("TestCases");
		writer.cell("TestCasesTotalLength");

		writer.newRow();
	}

	public void writeData(final TableWriter writer) {
		writer.cell(getAlgorithmTimeInMs());
		writer.cell(getExpandTimeInMs());
		writer.cell(getCloseTimeInMs());
		writer.cell(getExpandExplRefinementTimeInMs());
		writer.cell(getExpandZoneRefinementTimeInMs());
		writer.cell(getCloseExplRefinementTimeInMs());
		writer.cell(getCloseZoneRefinementTimeInMs());
		writer.cell(getCoverageChecks());
		writer.cell(getCoverageAttempts());
		writer.cell(getCoverageSuccesses());
		writer.cell(getExplRefinementSteps());
		writer.cell(getZoneRefinementSteps());
		writer.cell(getArgDepth());
		writer.cell(getArgNodes());
		writer.cell(getArgNodesExpanded());

		writer.cell(testCasesGenerated);
		writer.cell(getTestgenTimeInMs());
		writer.cell(testCases);
		writer.cell(testCasesTotalLength);

		writer.newRow();
	}

	public void startTestgen() {
		testgenTimer.start();
	}

	public void stopTestgen() {
		testgenTimer.stop();
	}

	public void testCaseGenerated() {
		testCasesGenerated++;
	}

	public void setTestCases(long num) {
		testCases = num;
	}

	public void addTestCaseLength(long length) {
		testCasesTotalLength += length;
	}
}
