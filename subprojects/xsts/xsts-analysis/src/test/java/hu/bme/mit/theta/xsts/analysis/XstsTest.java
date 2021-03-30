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
package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.Logger.Level;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfig;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfigBuilder;
import hu.bme.mit.theta.xsts.dsl.XstsDslManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
public class XstsTest {

	@Parameterized.Parameter(value = 0)
	public String filePath;

	@Parameterized.Parameter(value = 1)
	public String propPath;

	@Parameterized.Parameter(value = 2)
	public boolean safe;

	@Parameterized.Parameter(value = 3)
	public XstsConfigBuilder.Domain domain;

	@Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

				{ "src/test/resources/model/trafficlight.xsts", "src/test/resources/property/green_and_red.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/trafficlight.xsts", "src/test/resources/property/green_and_red.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/trafficlight.xsts", "src/test/resources/property/green_and_red.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/trafficlight_v2.xsts", "src/test/resources/property/green_and_red.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/trafficlight_v2.xsts", "src/test/resources/property/green_and_red.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/trafficlight_v2.xsts", "src/test/resources/property/green_and_red.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/counter5.xsts", "src/test/resources/property/x_between_0_and_5.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/counter5.xsts", "src/test/resources/property/x_between_0_and_5.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/counter5.xsts", "src/test/resources/property/x_between_0_and_5.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/counter5.xsts", "src/test/resources/property/x_eq_5.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/counter5.xsts", "src/test/resources/property/x_eq_5.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/counter5.xsts", "src/test/resources/property/x_eq_5.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/x_and_y.xsts", "src/test/resources/property/x_geq_y.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/x_powers.xsts", "src/test/resources/property/x_even.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/cross_with.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PRED_CART},

//				{ "src/test/resources/model/cross_with.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.EXPL},

//				{ "src/test/resources/model/cross_with.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/cross_without.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/cross_without.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.EXPL},

//				{ "src/test/resources/model/cross_without.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/choices.xsts", "src/test/resources/property/choices.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/choices.xsts", "src/test/resources/property/choices.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/choices.xsts", "src/test/resources/property/choices.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/literals.xsts", "src/test/resources/property/literals.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/literals.xsts", "src/test/resources/property/literals.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/literals.xsts", "src/test/resources/property/literals.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/cross3.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PRED_CART},

//				{ "src/test/resources/model/cross3.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.EXPL},

//				{ "src/test/resources/model/cross3.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/sequential.xsts", "src/test/resources/property/sequential.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/sequential.xsts", "src/test/resources/property/sequential.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/sequential.xsts", "src/test/resources/property/sequential.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/sequential.xsts", "src/test/resources/property/sequential2.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/sequential.xsts", "src/test/resources/property/sequential2.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/sequential.xsts", "src/test/resources/property/sequential2.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine2.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine2.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine2.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine3.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine3.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/on_off_statemachine.xsts", "src/test/resources/property/on_off_statemachine3.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_5.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_5.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_5.prop", false, XstsConfigBuilder.Domain.PROD},

//				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_50.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_50.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_50.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_51.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_51.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_51.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/count_up_down.xsts", "src/test/resources/property/count_up_down.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/count_up_down.xsts", "src/test/resources/property/count_up_down.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/count_up_down.xsts", "src/test/resources/property/count_up_down.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/count_up_down.xsts", "src/test/resources/property/count_up_down2.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/count_up_down.xsts", "src/test/resources/property/count_up_down2.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/count_up_down.xsts", "src/test/resources/property/count_up_down2.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/bhmr2007.xsts", "src/test/resources/property/bhmr2007.prop", true, XstsConfigBuilder.Domain.PRED_CART},

//				{ "src/test/resources/model/bhmr2007.xsts", "src/test/resources/property/bhmr2007.prop", true, XstsConfigBuilder.Domain.EXPL},

//				{ "src/test/resources/model/bhmr2007.xsts", "src/test/resources/property/bhmr2007.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/css2003.xsts", "src/test/resources/property/css2003.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/css2003.xsts", "src/test/resources/property/css2003.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/css2003.xsts", "src/test/resources/property/css2003.prop", true, XstsConfigBuilder.Domain.PROD},

//				{ "src/test/resources/model/ort.xsts", "src/test/resources/property/x_gt_2.prop", false, XstsConfigBuilder.Domain.PRED_CART},

//				{ "src/test/resources/model/ort2.xsts", "src/test/resources/property/ort2.prop", true, XstsConfigBuilder.Domain.PRED_CART},

//				{ "src/test/resources/model/crossroad_composite.xsts", "src/test/resources/property/both_green.prop", true, XstsConfigBuilder.Domain.EXPL}

				{ "src/test/resources/model/array_counter.xsts", "src/test/resources/property/array_10.prop", false, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/array_counter.xsts", "src/test/resources/property/array_10.prop", false, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/array_counter.xsts", "src/test/resources/property/array_10.prop", false, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/array_counter.xsts", "src/test/resources/property/array_10.prop", false, XstsConfigBuilder.Domain.PROD_AUTO},

				{ "src/test/resources/model/array_constant.xsts", "src/test/resources/property/array_constant.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/array_constant.xsts", "src/test/resources/property/array_constant.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/array_constant.xsts", "src/test/resources/property/array_constant.prop", true, XstsConfigBuilder.Domain.PROD},

				{ "src/test/resources/model/array_constant.xsts", "src/test/resources/property/array_constant.prop", true, XstsConfigBuilder.Domain.PROD_AUTO},

				{ "src/test/resources/model/localvars.xsts", "src/test/resources/property/localvars.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/localvars.xsts", "src/test/resources/property/localvars.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/localvars.xsts", "src/test/resources/property/localvars.prop", true, XstsConfigBuilder.Domain.PROD_AUTO},

				{ "src/test/resources/model/localvars2.xsts", "src/test/resources/property/localvars2.prop", true, XstsConfigBuilder.Domain.PRED_CART},

				{ "src/test/resources/model/localvars2.xsts", "src/test/resources/property/localvars2.prop", true, XstsConfigBuilder.Domain.EXPL},

				{ "src/test/resources/model/localvars2.xsts", "src/test/resources/property/localvars2.prop", true, XstsConfigBuilder.Domain.PROD_AUTO}


		});
	}

	@Test
	public void test() throws IOException {

		final Logger logger = new ConsoleLogger(Level.SUBSTEP);

		XSTS xsts;
		try (InputStream inputStream = new SequenceInputStream(new FileInputStream(filePath), new FileInputStream(propPath))) {
			xsts = XstsDslManager.createXsts(inputStream);
		}

		final XstsConfig<?, ?, ?> configuration = new XstsConfigBuilder(domain, XstsConfigBuilder.Refinement.SEQ_ITP, Z3SolverFactory.getInstance()).initPrec(XstsConfigBuilder.InitPrec.CTRL).predSplit(XstsConfigBuilder.PredSplit.CONJUNCTS).maxEnum(250).logger(logger).build(xsts);
		final SafetyResult<?, ?> status = configuration.check();
		if (safe) {
			assertTrue(status.isSafe());
		} else {
			assertTrue(status.isUnsafe());
		}
	}

}
