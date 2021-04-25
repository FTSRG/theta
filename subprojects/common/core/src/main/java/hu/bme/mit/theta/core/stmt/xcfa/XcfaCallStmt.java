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

package hu.bme.mit.theta.core.stmt.xcfa;

import hu.bme.mit.theta.common.LispStringBuilder;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.type.Expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XcfaCallStmt extends XcfaStmt {
	private static final String STMT_LABEL = "call";
	private final LinkedHashMap<Expr<?>, Direction> params;
	private final Map<Direction, List<Expr<?>>> paramTypes;
	private final String procedure;
	public enum Direction{
		IN,
		OUT,
		INOUT
	}

	public XcfaCallStmt(LinkedHashMap<Expr<?>, Direction> params, String procedure) {
		this.params = params;
		this.paramTypes = new HashMap<>();
		this.paramTypes.put(Direction.IN, new ArrayList<>());
		this.paramTypes.put(Direction.OUT, new ArrayList<>());
		this.paramTypes.put(Direction.INOUT, new ArrayList<>());
		this.params.forEach((expr, direction) -> paramTypes.get(direction).add(expr));
		this.procedure = procedure;
	}

	@Override
	public <P, R> R accept(StmtVisitor<? super P, ? extends R> visitor, P param) {
		return visitor.visit(this, param);
	}

	@Override
	public <P, R> R accept(XcfaStmtVisitor<? super P, ? extends R> visitor, P param) {
		return visitor.visit(this, param);
	}

	public LinkedHashMap<Expr<?>, Direction> getParams() {
		return params;
	}

	public Map<Direction, List<Expr<?>>> getParamTypes() {
		return paramTypes;
	}

	public String getProcedure() {
		return procedure;
	}

	public XcfaCallStmt of(LinkedHashMap<Expr<?>, Direction> params, String procedure) {
		return new XcfaCallStmt(params, procedure);
	}

	@Override
	public String toString() {
		LispStringBuilder call = Utils.lispStringBuilder(STMT_LABEL).add(procedure);
		params.forEach((expr, direction) ->
			call.add(Utils.lispStringBuilder().add(expr).add(direction)));
		return call.toString();
	}
}
