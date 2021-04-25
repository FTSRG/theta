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
package hu.bme.mit.theta.xcfa.dsl;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.booltype.FalseExpr;
import hu.bme.mit.theta.core.type.booltype.TrueExpr;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.core.type.rattype.RatLitExpr;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslBaseVisitor;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.FalseExprContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.IntLitExprContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.PrimaryExprContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.RatLitExprContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.TrueExprContext;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;

final class XcfaLitExpression {

	private final PrimaryExprContext context;

	XcfaLitExpression(final PrimaryExprContext context) {
		this.context = checkNotNull(context);
	}

	public Expr<?> instantiate() {
		final LitExprCreatorVisitor visitor = new LitExprCreatorVisitor();
		final LitExpr<?> expr = context.accept(visitor);
		if (expr == null) {
			throw new AssertionError();
		} else {
			return expr;
		}
	}

	private static final class LitExprCreatorVisitor extends XcfaDslBaseVisitor<LitExpr<?>> {

		@Override
		public TrueExpr visitTrueExpr(final TrueExprContext ctx) {
			return True();
		}

		@Override
		public FalseExpr visitFalseExpr(final FalseExprContext ctx) {
			return False();
		}

		@Override
		public IntLitExpr visitIntLitExpr(final IntLitExprContext ctx) {
			final BigInteger value = new BigInteger(ctx.value.getText());
			return Int(value);
		}

		@Override
		public RatLitExpr visitRatLitExpr(final RatLitExprContext ctx) {
			final BigInteger num = new BigInteger(ctx.num.getText());
			final BigInteger denom = new BigInteger(ctx.denom.getText());
			return Rat(num, denom);
		}

	}

}
