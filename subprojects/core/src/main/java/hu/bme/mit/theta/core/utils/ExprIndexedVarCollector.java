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
package hu.bme.mit.theta.core.utils;

import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.IndexedConstDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.anytype.RefExpr;

final class ExprIndexedVarCollector {

	private ExprIndexedVarCollector() {
	}

	static void collectIndexedVars(final Expr<?> expr, final IndexedVars.Builder builder) {
		if (expr instanceof RefExpr) {
			final RefExpr<?> ref = (RefExpr<?>) expr;
			final Decl<?> decl = ref.getDecl();
			if (decl instanceof IndexedConstDecl) {
				final IndexedConstDecl<?> indexedConstDecl = (IndexedConstDecl<?>) decl;
				builder.add(indexedConstDecl);
				return;
			}
		}

		expr.getOps().stream().forEach(op -> collectIndexedVars(op, builder));
	}

}
