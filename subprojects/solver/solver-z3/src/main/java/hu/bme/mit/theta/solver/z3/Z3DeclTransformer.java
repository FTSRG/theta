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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.microsoft.z3.Context;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.functype.FuncType;

final class Z3DeclTransformer {

	private final Z3TransformationManager transformer;
	private final Z3SymbolTable symbolTable;
	private final Context context;

	private int symbolCount;

	Z3DeclTransformer(final Z3TransformationManager transformer, final Z3SymbolTable symbolTable,
					  final Context context) {
		this.transformer = transformer;
		this.symbolTable = symbolTable;
		this.context = context;
		this.symbolCount = 0;
	}

	public com.microsoft.z3.FuncDecl toSymbol(final Decl<?> decl) {
		if (decl instanceof ConstDecl) {
			return transformConst((ConstDecl<?>) decl);
		} else {
			throw new UnsupportedOperationException("Cannot transform declaration: " + decl);
		}
	}

	private com.microsoft.z3.FuncDecl transformConst(final ConstDecl<?> decl) {
		final com.microsoft.z3.FuncDecl symbol;

		if (symbolTable.definesConst(decl)) {
			symbol = symbolTable.getSymbol(decl);
		} else {
			final Type type = decl.getType();

			final Tuple2<List<Type>, Type> extractedTypes = extractTypes(type);
			final List<Type> paramTypes = extractedTypes.get1();
			final Type returnType = extractedTypes.get2();

			final com.microsoft.z3.Sort returnSort = transformer.toSort(returnType);
			final com.microsoft.z3.Sort[] paramSorts = paramTypes.stream().map(t -> transformer.toSort(t))
					.toArray(size -> new com.microsoft.z3.Sort[size]);

			symbol = context.mkFuncDecl(symbolNameFor(decl), paramSorts, returnSort);
			symbolTable.put(decl, symbol);
		}

		return symbol;
	}

	private Tuple2<List<Type>, Type> extractTypes(final Type type) {
		if (type instanceof FuncType<?, ?>) {
			final FuncType<?, ?> funcType = (FuncType<?, ?>) type;

			final Type paramType = funcType.getParamType();
			final Type resultType = funcType.getResultType();

			checkArgument(!(paramType instanceof FuncType), "Parameter type most not be function");

			final Tuple2<List<Type>, Type> subResult = extractTypes(resultType);
			final List<Type> paramTypes = subResult.get1();
			final Type newResultType = subResult.get2();
			final List<Type> newParamTypes = ImmutableList.<Type>builder().add(paramType).addAll(paramTypes).build();
			final Tuple2<List<Type>, Type> result = Tuple2.of(newParamTypes, newResultType);

			return result;
		} else {
			return Tuple2.of(ImmutableList.of(), type);
		}
	}

	private String symbolNameFor(final Decl<?> decl) {
		return String.format("%s_%d", decl.getName(), symbolCount++);
	}

}
