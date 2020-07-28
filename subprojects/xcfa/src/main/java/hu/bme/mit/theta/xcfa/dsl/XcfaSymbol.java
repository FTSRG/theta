/*
 * Copyright 2019 Budapest University of Technology and Economics
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

import hu.bme.mit.theta.common.dsl.Scope;
import hu.bme.mit.theta.common.dsl.Symbol;
import hu.bme.mit.theta.common.dsl.SymbolTable;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class XcfaSymbol extends InstantiatableSymbol<XCFA> implements Scope {

	private final SymbolTable symbolTable;
	private final List<XcfaVariableSymbol> vars;
	private final List<XcfaProcessSymbol> processes;
	private XCFA xcfa = null;

	XcfaSymbol(final XcfaDslParser.SpecContext context) {
		symbolTable = new SymbolTable();
		vars = new ArrayList<>();
		processes = new ArrayList<>();
		context.varDecls.forEach(varDeclContext -> {
			XcfaVariableSymbol var;
			symbolTable.add(var = new XcfaVariableSymbol(varDeclContext));
			vars.add(var);
		});
		context.processDecls.forEach(processDeclContext -> {
			XcfaProcessSymbol proc;
			symbolTable.add(proc = new XcfaProcessSymbol(this, processDeclContext));
			processes.add(proc);
		});
	}


	public XCFA instantiate() {
		if (xcfa != null) return xcfa;
		XCFA.Builder builder = XCFA.builder();
		vars.forEach(xcfaVariableSymbol -> builder.createVar(xcfaVariableSymbol.instantiate()));
		processes.forEach(xcfaProcessSymbol -> {
			XCFA.Process process;
			builder.addProcess(process = xcfaProcessSymbol.instantiate());
			if (xcfaProcessSymbol.isMain()) builder.setMainProcess(process);
		});
		return xcfa = builder.build();
	}


	@Override
	public Optional<? extends Scope> enclosingScope() {
		return Optional.empty();
	}

	@Override
	public Optional<? extends Symbol> resolve(String name) {
		return symbolTable.get(name);
	}

	@Override
	public String getName() {
		return "";
	}
}
