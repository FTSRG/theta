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
package hu.bme.mit.theta.xcfa;

import hu.bme.mit.theta.xcfa.dsl.XcfaDslManager;
import hu.bme.mit.theta.xcfa.model.XCFA;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;
import hu.bme.mit.theta.xcfa.model.XcfaProcess;
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

				{"/simple.xcfa",                                //filepath
						0,                                      //globalVarCount
						1,                                      //processCount
						new Integer[]{0},                       //processVarCount
						new Integer[]{0},                       //processParamCount
						new Integer[]{1},                       //procedureCount
						new Integer[][]{{1}},                   //procedureVarCount
						new Integer[][]{{0}},                   //procedureParamCount
						new Integer[][]{{3}},                   //locCount
						new Integer[][]{{3}},                   //edgeCount
						new Integer[][][]{{{1, 2, 1}}}	        //statementCount
				}
		});
	}

	@Test
	public void test() throws IOException {
		final InputStream inputStream = getClass().getResourceAsStream(filepath);
		XCFA xcfa = XcfaDslManager.createXcfa(inputStream);
		inputStream.close();
		Assert.assertEquals(globalVarCount, xcfa.getGlobalVars().size());
		Assert.assertEquals(processCount, xcfa.getProcesses().size());
		for (int i = 0; i < xcfa.getProcesses().size(); ++i) {
			XcfaProcess process = xcfa.getProcesses().get(i);
			Assert.assertEquals((long) processVarCount[i], process.getThreadLocalVars().size());
			Assert.assertEquals((long) processParamCount[i], process.getParams().size());
			Assert.assertEquals((long) procedureCount[i], process.getProcedures().size());
			for (int j = 0; j < process.getProcedures().size(); ++j) {
				XcfaProcedure procedure = process.getProcedures().get(j);
				Assert.assertEquals((long) procedureVarCount[i][j], procedure.getLocalVars().size());
				Assert.assertEquals((long) procedureParamCount[i][j], procedure.getParams().size());
				Assert.assertEquals((long) locCount[i][j], procedure.getLocs().size());
				Assert.assertEquals((long) edgeCount[i][j], procedure.getEdges().size());
				for (int k = 0; k < procedure.getEdges().size(); ++k) {
					XcfaEdge edge = procedure.getEdges().get(k);
					Assert.assertEquals((long) statementCount[i][j][k], edge.getStmts().size());
				}
			}
		}
	}

}
