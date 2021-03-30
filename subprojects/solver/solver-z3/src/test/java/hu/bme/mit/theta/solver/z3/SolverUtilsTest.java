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
package hu.bme.mit.theta.solver.z3;

import static hu.bme.mit.theta.core.decl.Decls.Const;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Gt;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;

import java.util.stream.Stream;

import hu.bme.mit.theta.core.type.booltype.BoolLitExpr;
import org.junit.Assert;
import org.junit.Test;

import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.utils.SolverUtils;

public class SolverUtilsTest {

	@Test
	public void testModels() {
		// Arrange
		final SolverFactory factory = Z3SolverFactory.getInstance();

		final ConstDecl<IntType> cx = Const("x", Int());
		final ConstDecl<IntType> cy = Const("y", Int());
		final Expr<IntType> x = cx.getRef();
		final Expr<IntType> y = cy.getRef();
		final Expr<BoolType> expr = Gt(x, y);
		final Stream<Valuation> models = SolverUtils.models(factory, expr);

		// Act
		models.limit(5).forEach(m -> Assert.assertTrue(((BoolLitExpr)(expr.eval(m))).getValue()));
	}

}
