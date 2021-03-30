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

import com.microsoft.z3.Context;

import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;

final class Z3TransformationManager {

	private final Z3TypeTransformer typeTransformer;
	private final Z3DeclTransformer declTransformer;
	private final Z3ExprTransformer exprTransformer;

	public Z3TransformationManager(final Z3SymbolTable symbolTable, final Context context) {
		this.typeTransformer = new Z3TypeTransformer(this, context);
		this.declTransformer = new Z3DeclTransformer(this, symbolTable, context);
		this.exprTransformer = new Z3ExprTransformer(this, context);
	}

	public com.microsoft.z3.Sort toSort(final Type type) {
		return typeTransformer.toSort(type);
	}

	public com.microsoft.z3.FuncDecl toSymbol(final Decl<?> decl) {
		return declTransformer.toSymbol(decl);
	}

	public com.microsoft.z3.Expr toTerm(final Expr<?> expr) {
		return exprTransformer.toTerm(expr);
	}

	public void reset() {
		typeTransformer.reset();
		// declTransformer does not have to be resetted
		exprTransformer.reset();
	}
}
