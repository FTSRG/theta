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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static hu.bme.mit.theta.common.Utils.head;
import static hu.bme.mit.theta.common.Utils.tail;
import static hu.bme.mit.theta.core.decl.Decls.Param;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Exists;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Forall;
import static hu.bme.mit.theta.core.type.bvtype.BvExprs.BvType;
import static hu.bme.mit.theta.core.type.functype.FuncExprs.App;
import static hu.bme.mit.theta.core.type.functype.FuncExprs.Func;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;
import static java.lang.String.format;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import hu.bme.mit.theta.common.TernaryOperator;
import hu.bme.mit.theta.common.TriFunction;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.ParamDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.abstracttype.AddExpr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.abstracttype.GeqExpr;
import hu.bme.mit.theta.core.type.abstracttype.GtExpr;
import hu.bme.mit.theta.core.type.abstracttype.LeqExpr;
import hu.bme.mit.theta.core.type.abstracttype.LtExpr;
import hu.bme.mit.theta.core.type.abstracttype.MulExpr;
import hu.bme.mit.theta.core.type.anytype.IteExpr;
import hu.bme.mit.theta.core.type.arraytype.ArrayReadExpr;
import hu.bme.mit.theta.core.type.arraytype.ArrayType;
import hu.bme.mit.theta.core.type.arraytype.ArrayWriteExpr;
import hu.bme.mit.theta.core.type.booltype.AndExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.FalseExpr;
import hu.bme.mit.theta.core.type.booltype.IffExpr;
import hu.bme.mit.theta.core.type.booltype.ImplyExpr;
import hu.bme.mit.theta.core.type.booltype.NotExpr;
import hu.bme.mit.theta.core.type.booltype.OrExpr;
import hu.bme.mit.theta.core.type.booltype.TrueExpr;
import hu.bme.mit.theta.core.type.functype.FuncType;
import hu.bme.mit.theta.core.type.inttype.IntDivExpr;
import hu.bme.mit.theta.core.type.inttype.IntToRatExpr;
import hu.bme.mit.theta.core.type.inttype.IntModExpr;
import hu.bme.mit.theta.core.type.rattype.RatDivExpr;
import hu.bme.mit.theta.core.utils.BvUtils;
import hu.bme.mit.theta.core.utils.TypeUtils;
import static hu.bme.mit.theta.core.type.arraytype.ArrayExprs.Array;

final class Z3TermTransformer {
	private static final String PARAM_NAME_FORMAT = "_p%d";

	private final Z3SymbolTable symbolTable;
	private final Map<String, TriFunction<com.microsoft.z3.Expr, Model, List<Decl<?>>, Expr<?>>> environment;

	public Z3TermTransformer(final Z3SymbolTable symbolTable) {
		this.symbolTable = symbolTable;

		environment = new HashMap<>();
		environment.put("true", exprNullaryOperator(TrueExpr::getInstance));
		environment.put("false", exprNullaryOperator(FalseExpr::getInstance));
		environment.put("not", exprUnaryOperator(NotExpr::create));
		environment.put("or", exprMultiaryOperator(OrExpr::create));
		environment.put("and", exprMultiaryOperator(AndExpr::create));
		environment.put("=>", exprBinaryOperator(ImplyExpr::create));
		environment.put("iff", exprBinaryOperator(IffExpr::create));
		environment.put("=", exprBinaryOperator(EqExpr::create2));
		environment.put("<=", exprBinaryOperator(LeqExpr::create2));
		environment.put("<", exprBinaryOperator(LtExpr::create2));
		environment.put(">=", exprBinaryOperator(GeqExpr::create2));
		environment.put(">", exprBinaryOperator(GtExpr::create2));
		environment.put("+", exprMultiaryOperator(AddExpr::create2));
		environment.put("*", exprMultiaryOperator(MulExpr::create2));
		environment.put("div", exprBinaryOperator(IntDivExpr::create));
		environment.put("/", exprBinaryOperator(RatDivExpr::create));
		environment.put("if", exprTernaryOperator(IteExpr::create));
		environment.put("select", exprBinaryOperator(ArrayReadExpr::create));
		environment.put("store", exprTernaryOperator(ArrayWriteExpr::create));
		environment.put("to_real", exprUnaryOperator(IntToRatExpr::create));
		environment.put("mod", exprBinaryOperator(IntModExpr::create));
	}

	public Expr<?> toExpr(final com.microsoft.z3.Expr term) {
		return transform(term, null, new ArrayList<>());
	}

	public Expr<?> toFuncLitExpr(final FuncDecl funcDecl, final Model model,
								 final List<Decl<?>> vars) {
		checkNotNull(model, "Unsupported function '" + funcDecl.getName() + "' in Z3 back-transformation.");
		final com.microsoft.z3.FuncInterp funcInterp = model.getFuncInterp(funcDecl);
		final List<ParamDecl<?>> paramDecls = transformParams(vars, funcDecl.getDomain());
		pushParams(vars, paramDecls);
		final Expr<?> funcLitExpr = transformFuncInterp(funcInterp, model, vars);
		popParams(vars, paramDecls);
		return funcLitExpr;
	}

	public Expr<?> toArrayLitExpr(final FuncDecl funcDecl, final Model model, final List<Decl<?>> vars) {
		final com.microsoft.z3.FuncInterp funcInterp = model.getFuncInterp(funcDecl);
		final List<Tuple2<Expr<?>, Expr<?>>> entryExprs = createEntryExprs(funcInterp, model, vars);
		final Expr<?> elseExpr = transform(funcInterp.getElse(), model, vars);

		final ArraySort sort = (ArraySort) funcDecl.getRange();

		return createArrayLitExpr(sort, entryExprs, elseExpr);
	}

	private Expr<?> createArrayLitExpr(ArraySort sort, List<Tuple2<Expr<?>, Expr<?>>> entryExprs, Expr<?> elseExpr) {
		return this.createIndexValueArrayLitExpr(transformSort(sort.getDomain()), transformSort(sort.getRange()), entryExprs, elseExpr);
	}

	@SuppressWarnings("unchecked")
	private <I extends Type, E extends Type> Expr<?> createIndexValueArrayLitExpr(I indexType, E elemType, List<Tuple2<Expr<?>, Expr<?>>> entryExprs, Expr<?> elseExpr) {
		return Array(entryExprs.stream().map(entry -> Tuple2.of((Expr<I>) entry.get1(), (Expr<E>) entry.get2())).collect(Collectors.toUnmodifiableList()),
				(Expr<E>)elseExpr,
				ArrayType.of(indexType, elemType));
	}

	////////

	private Expr<?> transform(final com.microsoft.z3.Expr term, final Model model,
							  final List<Decl<?>> vars) {
		if (term.isIntNum()) {
			return transformIntLit(term);

		} else if (term.isRatNum()) {
			return transformRatLit(term);

		// BitVecNum is not BVNumeral? Potential bug?
		} else if (/* term.isBVNumeral() */ term instanceof com.microsoft.z3.BitVecNum) {
			return transformBvLit(term);

		} else if (term.isConstantArray()) {
			return transformArrLit(term, model, vars);

		} else if (term.isApp()) {
			return transformApp(term, model, vars);

		} else if (term.isQuantifier()) {
			final com.microsoft.z3.Quantifier quantifier = (com.microsoft.z3.Quantifier) term;
			return transformQuantifier(quantifier, model, vars);

		} else if (term.isVar()) {
			return transformVar(term, vars);

		} else {
			return transformUnsupported(term, model, vars);
		}
	}

	////

	private Expr<?> transformIntLit(final com.microsoft.z3.Expr term) {
		final com.microsoft.z3.IntNum intNum = (com.microsoft.z3.IntNum) term;
		final var value = intNum.getBigInteger();
		return Int(value);
	}

	private Expr<?> transformRatLit(final com.microsoft.z3.Expr term) {
		final com.microsoft.z3.RatNum ratNum = (com.microsoft.z3.RatNum) term;
		final var num = ratNum.getNumerator().getBigInteger();
		final var denom = ratNum.getDenominator().getBigInteger();
		return Rat(num, denom);
	}

	private Expr<?> transformArrLit(final com.microsoft.z3.Expr term, final Model model,
									final List<Decl<?>> vars) {
		final ArrayExpr arrayExpr = (ArrayExpr) term;
		final ArraySort sort = (ArraySort) arrayExpr.getSort();
		return createArrayLitExpr(sort, Arrays.asList(), transform(arrayExpr.getArgs()[0], model, vars));
	}

	private Expr<?> transformBvLit(final com.microsoft.z3.Expr term) {
		final com.microsoft.z3.BitVecNum bvNum = (com.microsoft.z3.BitVecNum) term;

		BigInteger value = bvNum.getBigInteger();

		return BvUtils.bigIntegerToNeutralBvLitExpr(value, bvNum.getSortSize());
	}

	private Expr<?> transformApp(final com.microsoft.z3.Expr term, final Model model, final List<Decl<?>> vars) {

		final FuncDecl funcDecl = term.getFuncDecl();
		final String symbol = funcDecl.getName().toString();

		if (environment.containsKey(symbol)) {
			return environment.get(symbol).apply(term, model, vars);
		} else {
			final Expr<?> funcExpr;
			if (symbolTable.definesSymbol(funcDecl)) {
				funcExpr = symbolTable.getConst(funcDecl).getRef();
			} else {
				funcExpr = toFuncLitExpr(funcDecl, model, vars);
			}
			return transformFuncApp(funcExpr, term.getArgs(), model, vars);
		}
	}

	private Expr<?> transformFuncInterp(final com.microsoft.z3.FuncInterp funcInterp,
										final Model model, final List<Decl<?>> vars) {
		checkArgument(funcInterp.getArity() == 1);
		final ParamDecl<?> paramDecl = (ParamDecl<?>) vars.get(vars.size() - 1);
		final Expr<?> op = createFuncLitExprBody(paramDecl, funcInterp, model, vars);
		return Func(paramDecl, op);
	}

	private Expr<?> createFuncLitExprBody(final ParamDecl<?> paramDecl, final com.microsoft.z3.FuncInterp funcInterp,
										  final Model model, final List<Decl<?>> vars) {
		final List<Tuple2<Expr<?>, Expr<?>>> entryExprs = createEntryExprs(funcInterp, model, vars);
		final Expr<?> elseExpr = transform(funcInterp.getElse(), model, vars);
		return createNestedIteExpr(paramDecl, entryExprs, elseExpr);
	}

	private Expr<?> createNestedIteExpr(final ParamDecl<?> paramDecl, final List<Tuple2<Expr<?>, Expr<?>>> entryExprs,
										final Expr<?> elseExpr) {
		if (entryExprs.isEmpty()) {
			return elseExpr;
		} else {
			final Tuple2<Expr<?>, Expr<?>> head = head(entryExprs);
			final List<Tuple2<Expr<?>, Expr<?>>> tail = tail(entryExprs);
			final Expr<BoolType> cond = EqExpr.create2(paramDecl.getRef(), head.get1());
			final Expr<?> then = head.get2();
			final Expr<?> elze = createNestedIteExpr(paramDecl, tail, elseExpr);
			return IteExpr.create(cond, then, elze);
		}
	}

	private List<Tuple2<Expr<?>, Expr<?>>> createEntryExprs(final com.microsoft.z3.FuncInterp funcInterp,
															final Model model, final List<Decl<?>> vars) {
		final ImmutableList.Builder<Tuple2<Expr<?>, Expr<?>>> builder = ImmutableList.builder();
		for (final com.microsoft.z3.FuncInterp.Entry entry : funcInterp.getEntries()) {
			checkArgument(entry.getArgs().length == 1);
			final com.microsoft.z3.Expr term1 = entry.getArgs()[0];
			final com.microsoft.z3.Expr term2 = entry.getValue();
			final Expr<?> expr1 = transform(term1, model, vars);
			final Expr<?> expr2 = transform(term2, model, vars);
			builder.add(Tuple2.of(expr1, expr2));
		}
		return builder.build();
	}

	private Expr<?> transformQuantifier(final com.microsoft.z3.Quantifier term, final Model model,
										final List<Decl<?>> vars) {
		if (term.isUniversal()) {
			return transformForall(term, model, vars);

		} else if (term.isExistential()) {
			return transformExists(term, model, vars);

		} else {
			throw new AssertionError("Unhandled case: " + term.toString());
		}
	}

	private Expr<?> transformVar(final com.microsoft.z3.Expr term, final List<Decl<?>> vars) {
		final int index = term.getIndex();
		final Decl<?> decl = vars.get(vars.size() - index - 1);
		return decl.getRef();
	}

	private <P extends Type, R extends Type> Expr<?> transformFuncApp(final Expr<?> expr,
																	  final com.microsoft.z3.Expr[] argTerms, final Model model, final List<Decl<?>> vars) {
		Expr<?> result = expr;
		for (final com.microsoft.z3.Expr term : argTerms) {
			@SuppressWarnings("unchecked") final Expr<FuncType<P, R>> func = (Expr<FuncType<P, R>>) result;
			final Expr<P> arg = TypeUtils.cast(transform(term, model, vars), func.getType().getParamType());
			result = App(func, arg);
		}
		return result;
	}

	////

	private Expr<?> transformForall(final com.microsoft.z3.Expr term, final Model model,
									final List<Decl<?>> vars) {
		final com.microsoft.z3.Quantifier quantifier = (com.microsoft.z3.Quantifier) term;
		final com.microsoft.z3.BoolExpr opTerm = quantifier.getBody();
		final com.microsoft.z3.Sort[] sorts = quantifier.getBoundVariableSorts();
		final List<ParamDecl<?>> paramDecls = transformParams(vars, sorts);

		pushParams(vars, paramDecls);
		final Expr<BoolType> op = TypeUtils.cast(transform(opTerm, model, vars), Bool());
		popParams(vars, paramDecls);

		return Forall(paramDecls, op);
	}

	private Expr<?> transformExists(final com.microsoft.z3.Expr term, final Model model,
									final List<Decl<?>> vars) {
		final com.microsoft.z3.Quantifier quantifier = (com.microsoft.z3.Quantifier) term;
		final com.microsoft.z3.BoolExpr opTerm = quantifier.getBody();
		final com.microsoft.z3.Sort[] sorts = quantifier.getBoundVariableSorts();
		final List<ParamDecl<?>> paramDecls = transformParams(vars, sorts);

		pushParams(vars, paramDecls);
		final Expr<BoolType> op = TypeUtils.cast(transform(opTerm, model, vars), Bool());
		popParams(vars, paramDecls);

		return Exists(paramDecls, op);
	}

	private List<ParamDecl<?>> transformParams(final List<Decl<?>> vars, final com.microsoft.z3.Sort[] sorts) {
		final ImmutableList.Builder<ParamDecl<?>> builder = ImmutableList.builder();
		for (final com.microsoft.z3.Sort sort : sorts) {
			final ParamDecl<?> param = transformParam(vars, sort);
			builder.add(param);
		}
		final List<ParamDecl<?>> paramDecls = builder.build();
		return paramDecls;
	}

	private ParamDecl<?> transformParam(final List<Decl<?>> vars, final com.microsoft.z3.Sort sort) {
		final Type type = transformSort(sort);
		final ParamDecl<?> param = Param(format(PARAM_NAME_FORMAT, vars.size()), type);
		return param;
	}

	private Type transformSort(final com.microsoft.z3.Sort sort) {
		if (sort instanceof com.microsoft.z3.BoolSort) {
			return Bool();
		} else if (sort instanceof com.microsoft.z3.IntSort) {
			return Int();
		} else if (sort instanceof com.microsoft.z3.RealSort) {
			return Rat();
		} else if (sort instanceof com.microsoft.z3.BitVecSort) {
			final com.microsoft.z3.BitVecSort bvSort = (com.microsoft.z3.BitVecSort) sort;
			return BvType(bvSort.getSize());
		} else {
			throw new AssertionError("Unsupported sort: " + sort);
		}
	}

	private void pushParams(final List<Decl<?>> vars, final List<ParamDecl<?>> paramDecls) {
		vars.addAll(paramDecls);
	}

	private void popParams(final List<Decl<?>> vars, final List<ParamDecl<?>> paramDecls) {
		for (int i = 0; i < paramDecls.size(); i++) {
			vars.remove(vars.size() - 1);
		}
	}

	private Expr<?> transformUnsupported(final com.microsoft.z3.Expr term, final Model model,
										 final List<Decl<?>> vars) {
		throw new UnsupportedOperationException("Unsupported term: " + term);
	}

	////

	private TriFunction<com.microsoft.z3.Expr, Model, List<Decl<?>>, Expr<?>> exprNullaryOperator(
			final Supplier<Expr<?>> function) {
		return (term, model, vars) -> {
			final com.microsoft.z3.Expr[] args = term.getArgs();
			checkArgument(args.length == 0, "Number of arguments must be zero");
			return function.get();
		};
	}

	private TriFunction<com.microsoft.z3.Expr, Model, List<Decl<?>>, Expr<?>> exprUnaryOperator(
			final UnaryOperator<Expr<?>> function) {
		return (term, model, vars) -> {
			final com.microsoft.z3.Expr[] args = term.getArgs();
			checkArgument(args.length == 1, "Number of arguments must be one");
			final Expr<?> op = transform(args[0], model, vars);
			return function.apply(op);
		};
	}

	private TriFunction<com.microsoft.z3.Expr, Model, List<Decl<?>>, Expr<?>> exprBinaryOperator(
			final BinaryOperator<Expr<?>> function) {
		return (term, model, vars) -> {
			final com.microsoft.z3.Expr[] args = term.getArgs();
			checkArgument(args.length == 2, "Number of arguments must be two");
			final Expr<?> op1 = transform(args[0], model, vars);
			final Expr<?> op2 = transform(args[1], model, vars);
			return function.apply(op1, op2);
		};
	}

	private TriFunction<com.microsoft.z3.Expr, Model, List<Decl<?>>, Expr<?>> exprTernaryOperator(
			final TernaryOperator<Expr<?>> function) {
		return (term, model, vars) -> {
			final com.microsoft.z3.Expr[] args = term.getArgs();
			checkArgument(args.length == 3, "Number of arguments must be three");
			final Expr<?> op1 = transform(args[0], model, vars);
			final Expr<?> op2 = transform(args[1], model, vars);
			final Expr<?> op3 = transform(args[2], model, vars);
			return function.apply(op1, op2, op3);
		};
	}

	private TriFunction<com.microsoft.z3.Expr, Model, List<Decl<?>>, Expr<?>> exprMultiaryOperator(
			final Function<List<Expr<?>>, Expr<?>> function) {
		return (term, model, vars) -> {
			final com.microsoft.z3.Expr[] args = term.getArgs();
			final List<Expr<?>> ops = Stream.of(args).map(arg -> transform(arg, model, vars))
					.collect(toImmutableList());
			return function.apply(ops);
		};
	}

}
