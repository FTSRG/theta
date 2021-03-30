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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import hu.bme.mit.theta.core.decl.ConstDecl;

final class Z3SymbolTable {

	private final BiMap<ConstDecl<?>, com.microsoft.z3.FuncDecl> constToSymbol;

	public Z3SymbolTable() {
		constToSymbol = Maps.synchronizedBiMap(HashBiMap.create());
	}

	public boolean definesConst(final ConstDecl<?> constDecl) {
		return constToSymbol.containsKey(constDecl);
	}

	public boolean definesSymbol(final com.microsoft.z3.FuncDecl symbol) {
		return constToSymbol.inverse().containsKey(symbol);
	}

	public com.microsoft.z3.FuncDecl getSymbol(final ConstDecl<?> constDecl) {
		checkArgument(definesConst(constDecl), "Declaration " + constDecl + " not found in symbol table");
		return constToSymbol.get(constDecl);
	}

	public ConstDecl<?> getConst(final com.microsoft.z3.FuncDecl symbol) {
		checkArgument(definesSymbol(symbol), "Symbol " + symbol + " not found in symbol table");
		return constToSymbol.inverse().get(symbol);
	}

	public void put(final ConstDecl<?> constDecl, final com.microsoft.z3.FuncDecl symbol) {
		checkNotNull(constDecl);
		checkNotNull(symbol);
		checkState(!constToSymbol.containsKey(constDecl), "Constant " + constDecl + " not found.");
		constToSymbol.put(constDecl, symbol);
	}

	public void clear() {
		constToSymbol.clear();
	}

}
