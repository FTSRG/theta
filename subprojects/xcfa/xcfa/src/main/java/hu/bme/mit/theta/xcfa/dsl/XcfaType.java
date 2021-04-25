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

import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.xcfa.SyntheticType;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslBaseVisitor;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.ArrayTypeContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.BoolTypeContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.IntTypeContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.RatTypeContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.TypeContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.arraytype.ArrayExprs.Array;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;

final class XcfaType implements Instantiatable<Type> {

	private final TypeContext context;
	private Type type = null;

	XcfaType(final TypeContext context) {
		this.context = checkNotNull(context);
	}

	public Type instantiate() {
		if (type != null) return type;
		final Type result = TypeCreatorVisitor.INSTANCE.visit(context);
		if (result == null) {
			throw new AssertionError();
		} else {
			return type = result;
		}
	}

	private static class TypeCreatorVisitor extends XcfaDslBaseVisitor<Type> {
		private static final TypeCreatorVisitor INSTANCE = new TypeCreatorVisitor();

		private TypeCreatorVisitor() {
		}

		@Override
		public Type visitBoolType(final BoolTypeContext ctx) {
			return Bool();
		}

		@Override
		public Type visitIntType(final IntTypeContext ctx) {
			return Int();
		}

		@Override
		public Type visitRatType(final RatTypeContext ctx) {
			return Rat();
		}

		@Override
		public Type visitArrayType(final ArrayTypeContext ctx) {
			final Type indexType = ctx.indexType.accept(this);
			final Type elemType = ctx.elemType.accept(this);
			return Array(indexType, elemType);
		}

	}

}
