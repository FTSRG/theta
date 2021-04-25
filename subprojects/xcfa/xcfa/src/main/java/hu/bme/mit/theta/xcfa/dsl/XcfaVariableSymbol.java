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

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.VarDeclContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.decl.Decls.Var;

final class XcfaVariableSymbol extends InstantiatableSymbol<VarDecl<?>> {

	private final String name;
	private final XcfaType type;
	private final XcfaLitExpression initExpr;
	private VarDecl<?> var = null;

	XcfaVariableSymbol(final VarDeclContext context) {
		checkNotNull(context);
		name = context.ddecl.name.getText();
		type = new XcfaType(context.ddecl.ttype);
		if (context.initexpr != null) {
			initExpr = new XcfaLitExpression(context.initexpr);
		} else {
			initExpr = null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public VarDecl<?> instantiate() {
		if (var != null) return var;
		return var = Var(name, type.instantiate());
	}

	public XcfaLitExpression getInitExpr() {
		return initExpr;
	}
}
