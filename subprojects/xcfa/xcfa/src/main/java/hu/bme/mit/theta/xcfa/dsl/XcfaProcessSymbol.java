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

import com.google.common.base.Preconditions;
import hu.bme.mit.theta.common.dsl.Scope;
import hu.bme.mit.theta.common.dsl.Symbol;
import hu.bme.mit.theta.common.dsl.SymbolTable;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.ProcessDeclContext;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;
import hu.bme.mit.theta.xcfa.model.XcfaProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class XcfaProcessSymbol extends InstantiatableSymbol<XcfaProcess> implements Scope {

	private final XcfaSymbol scope;
	private final String name;
	private final boolean isMain;
	private final List<XcfaParamSymbol> params;
	private final List<XcfaVariableSymbol> vars;
	private final List<XcfaProcedureSymbol> procedures;
	private final SymbolTable symbolTable;
	private XcfaProcess process = null;

	XcfaProcessSymbol(final XcfaSymbol scope, final XcfaDslParser.ProcessDeclContext context) {
		this.scope = scope;
		name = context.id.getText();
		isMain = context.main != null;
		symbolTable = new SymbolTable();

		params = new ArrayList<>();
		vars = new ArrayList<>();
		procedures = new ArrayList<>();

		if (context.paramDecls != null) {
			context.paramDecls.decls.forEach(declContext -> {
				XcfaParamSymbol paramSymbol;
				symbolTable.add(paramSymbol = new XcfaParamSymbol(declContext));
				params.add(paramSymbol);
			});
		}
		if (context.varDecls != null) {
			context.varDecls.forEach(varDeclContext -> {
				XcfaVariableSymbol variableSymbol;
				symbolTable.add(variableSymbol = new XcfaVariableSymbol(varDeclContext));
				vars.add(variableSymbol);
			});
		}
		if (context.procedureDecls != null) {
			context.procedureDecls.forEach(procedureDeclContext -> {
				XcfaProcedureSymbol procedureSymbol;
				symbolTable.add(procedureSymbol = new XcfaProcedureSymbol(this, procedureDeclContext));
				procedures.add(procedureSymbol);
			});
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Optional<XcfaSymbol> enclosingScope() {
		return Optional.of(scope);
	}

	@Override
	public Optional<? extends Symbol> resolve(final String name) {
		final Optional<Symbol> symbol = symbolTable.get(name);
		if (symbol.isPresent()) {
			return symbol;
		} else {
			return scope.resolve(name);
		}
	}

	public XcfaProcess instantiate() {
		if (process != null) return process;
		XcfaProcess.Builder builder = XcfaProcess.builder();
		params.forEach(xcfaParamSymbol -> builder.createParam(xcfaParamSymbol.instantiate()));
		vars.forEach(xcfaVariableSymbol -> builder.createVar(xcfaVariableSymbol.instantiate(), (xcfaVariableSymbol.getInitExpr() == null ? null : (LitExpr<?>) xcfaVariableSymbol.getInitExpr().instantiate())));
		procedures.forEach(xcfaProcedureSymbol -> {
			XcfaProcedure procedure;
			builder.addProcedure(procedure = xcfaProcedureSymbol.instantiate());
			if (xcfaProcedureSymbol.isMain()) builder.setMainProcedure(procedure);
		});
		builder.setName(name);
		return process = builder.build();
	}

	public boolean isMain() {
		return isMain;
	}

	public void addContext(ProcessDeclContext context) {
		Preconditions.checkArgument(context.id.getText().equals(name));
		Preconditions.checkArgument((context.main != null) == isMain);

		if (context.paramDecls != null) {
			context.paramDecls.decls.forEach(declContext -> {
				XcfaParamSymbol paramSymbol;
				symbolTable.add(paramSymbol = new XcfaParamSymbol(declContext));
				params.add(paramSymbol);
			});
		}
		if (context.varDecls != null) {
			context.varDecls.forEach(varDeclContext -> {
				XcfaVariableSymbol variableSymbol;
				symbolTable.add(variableSymbol = new XcfaVariableSymbol(varDeclContext));
				vars.add(variableSymbol);
			});
		}
		if (context.procedureDecls != null) {
			context.procedureDecls.forEach(procedureDeclContext -> {
				XcfaProcedureSymbol procedureSymbol;
				symbolTable.add(procedureSymbol = new XcfaProcedureSymbol(this, procedureDeclContext));
				procedures.add(procedureSymbol);
			});
		}
	}
}
