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

import hu.bme.mit.theta.common.dsl.Symbol;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

public class XcfaEdge implements Instantiatable<hu.bme.mit.theta.xcfa.model.XcfaEdge> {

	private final XcfaLocationSymbol source;
	private final XcfaLocationSymbol target;
	private final List<XcfaStatement> stmts;
	private hu.bme.mit.theta.xcfa.model.XcfaEdge built = null;

	XcfaEdge(final XcfaProcedureSymbol scope, final XcfaDslParser.EdgeContext context) {
		Optional<? extends Symbol> opt = scope.resolve(context.source.getText());
		checkState(opt.isPresent(), "Could not resolve symbol " + context.source.getText());
		source = (XcfaLocationSymbol) opt.get();
		opt = scope.resolve(context.target.getText());
		checkState(opt.isPresent(), "Could not resolve symbol " + context.target.getText());
		target = (XcfaLocationSymbol) opt.get();

		stmts = new ArrayList<>();
		context.stmts.forEach(stmtContext -> stmts.add(new XcfaStatement(scope, stmtContext)));
	}

	@Override
	public hu.bme.mit.theta.xcfa.model.XcfaEdge instantiate() {
		if (built != null) return built;
		List<Stmt> stmts = new ArrayList<>();
		this.stmts.forEach(xcfaStatement -> stmts.add(xcfaStatement.instantiate()));
		return built = new hu.bme.mit.theta.xcfa.model.XcfaEdge(source.instantiate(), target.instantiate(), stmts);
	}

	public List<XcfaStatement> getStatements() {
		return stmts;
	}
}
