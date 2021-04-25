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
package hu.bme.mit.theta.core.dsl.impl;

import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;

public class StmtWriter implements StmtVisitor<Void, String> {

	private static String writeExpr(final Expr<?> expr) {
		return ExprWriter.instance().write(expr);
	}

	@Override
	public String visit(final SkipStmt stmt, final Void param) {
		return "skip";
	}

	@Override
	public String visit(final AssumeStmt stmt, final Void param) {
		return "assume " + writeExpr(stmt.getCond());
	}

	@Override
	public <DeclType extends Type> String visit(final AssignStmt<DeclType> stmt, final Void param) {
		return stmt.getVarDecl().getName() + " := " + writeExpr(stmt.getExpr());
	}

	@Override
	public <DeclType extends Type> String visit(final HavocStmt<DeclType> stmt, final Void param) {
		return "havoc " + stmt.getVarDecl().getName();
	}

	@Override
	public String visit(XcfaStmt xcfaStmt, Void param) {
		throw new UnsupportedOperationException("Not yet implemented"); //TODO
	}

	@Override
	public String visit(SequenceStmt stmt, Void param) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String visit(NonDetStmt stmt, Void param) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String visit(OrtStmt stmt, Void param) { throw new UnsupportedOperationException(); }

}
