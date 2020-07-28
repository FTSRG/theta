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
package hu.bme.mit.theta.xcfa;

import hu.bme.mit.theta.xcfa.dsl.XcfaDslManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public final class XcfaDslTest {

	@Parameter(0)
	public String filepath;

	@Parameter(1)
	public int globalVarCount;

	@Parameter(2)
	public int processCount;

	@Parameter(3)
	public Integer[] processVarCount;

	@Parameter(4)
	public Integer[] processParamCount;

	@Parameter(5)
	public Integer[] procedureCount;

	@Parameter(6)
	public Integer[][] procedureVarCount;

	@Parameter(7)
	public Integer[][] procedureParamCount;

	@Parameter(8)
	public Integer[][] locCount;

	@Parameter(9)
	public Integer[][] edgeCount;

	@Parameter(10)
	public Integer[][][] statementCount;

	@Parameters()
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{

				{"/peterson.xcfa",                              //filepath
						6,                                      //globalVarCount
						3,                                      //processCount
						new Integer[]{0, 0, 0},                 //processVarCount
						new Integer[]{0, 0, 0},                 //processParamCount
						new Integer[]{3, 1, 1},                 //procedureCount
						new Integer[][]{{0, 0, 0}, {0}, {0}},   //procedureVarCount
						new Integer[][]{{0, 0, 0}, {0}, {0}},   //procedureParamCount
						new Integer[][]{{4, 2, 2}, {8}, {8}},   //locCount
						new Integer[][]{{3, 1, 1}, {8}, {8}},   //edgeCount
						new Integer[][][]{
								{{1, 1, 1}, {4}, {1}},
								{{3, 1, 1, 1, 1, 1, 1, 1}},
								{{3, 1, 1, 1, 1, 1, 1, 1}}
						}   //statementCount
				},
				{"/mtx_test.xcfa",
						2,
						2,
						new Integer[]{0, 0},
						new Integer[]{0, 0},
						new Integer[]{1, 1},
						new Integer[][]{{0}, {0}},
						new Integer[][]{{0}, {0}},
						new Integer[][]{{4}, {4}},
						new Integer[][]{{3}, {3}},
						new Integer[][][]{
								{{1, 1, 1}},
								{{1, 1, 1}}
						}
				}
		});
	}

	@Test
	public void test() throws IOException {
		final InputStream inputStream = getClass().getResourceAsStream(filepath);
		XCFA xcfa = XcfaDslManager.createXcfa(inputStream);
		Assert.assertEquals(globalVarCount, xcfa.getVars().size());
		Assert.assertEquals(processCount, xcfa.getProcesses().size());
		for (int i = 0; i < xcfa.getProcesses().size(); ++i) {
			XCFA.Process process = xcfa.getProcesses().get(i);
			Assert.assertEquals((long) processVarCount[i], process.getVars().size());
			Assert.assertEquals((long) processParamCount[i], process.getParams().size());
			Assert.assertEquals((long) procedureCount[i], process.getProcedures().size());
			for (int j = 0; j < process.getProcedures().size(); ++j) {
				XCFA.Process.Procedure procedure = process.getProcedures().get(j);
				Assert.assertEquals((long) procedureVarCount[i][j], procedure.getVars().size());
				Assert.assertEquals((long) procedureParamCount[i][j], procedure.getParams().size());
				Assert.assertEquals((long) locCount[i][j], procedure.getLocs().size());
				Assert.assertEquals((long) edgeCount[i][j], procedure.getEdges().size());
				for (int k = 0; k < procedure.getEdges().size(); ++k) {
					XCFA.Process.Procedure.Edge edge = procedure.getEdges().get(k);
					Assert.assertEquals((long) statementCount[i][j][k], edge.getStmts().size());
				}
			}
		}
	}

}
