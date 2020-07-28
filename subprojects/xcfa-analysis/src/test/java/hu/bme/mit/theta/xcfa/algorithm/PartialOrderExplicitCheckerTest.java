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
package hu.bme.mit.theta.xcfa.algorithm;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.dsl.XcfaDslManager;
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
public class PartialOrderExplicitCheckerTest {
    @Parameter()
    public String filepath;

    @Parameter(1)
    public Boolean shouldWork;

    @Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{"/partialorder-test2.xcfa", false},
                new Object[]{"/functions-global-local.xcfa", true},
                new Object[]{"/fibonacci.xcfa", true},
                new Object[]{"/simple-test.xcfa", true},
                new Object[]{"/deadlock.xcfa", false},
                // new Object[]{"/atomic.xcfa", true}, -> TODO add atomic operations
                new Object[]{"/gcd.xcfa", true},
                new Object[]{"/partialorder-test.xcfa", false},
                new Object[]{"/infiniteloop.xcfa", true},
                new Object[]{"/very-parallel.xcfa", true},
                new Object[]{"/mutex-test.xcfa", true},
                new Object[]{"/mutex-test2.xcfa", false},
                new Object[]{"/mutex-test3.xcfa", false},
                new Object[]{"/mutex-test4.xcfa", true}
        );
    }

    @Test
    public void test() throws IOException {
        final InputStream inputStream = getClass().getResourceAsStream(filepath);
        System.out.println("Testing " + filepath);
        XCFA xcfa = XcfaDslManager.createXcfa(inputStream);
        PartialOrderExplicitChecker checker = new PartialOrderExplicitChecker(xcfa);
        SafetyResult<? extends State, ? extends Action> result = checker.check();
        ExplicitCheckerTest.checkResult(result, shouldWork);
    }

}
