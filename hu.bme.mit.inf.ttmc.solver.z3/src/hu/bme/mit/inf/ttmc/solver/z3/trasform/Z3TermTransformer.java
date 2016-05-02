package hu.bme.mit.inf.ttmc.solver.z3.trasform;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.RatNum;

import hu.bme.mit.inf.ttmc.core.decl.ConstDecl;
import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.expr.impl.Exprs;
import hu.bme.mit.inf.ttmc.core.type.ArrayType;
import hu.bme.mit.inf.ttmc.core.type.BoolType;
import hu.bme.mit.inf.ttmc.core.type.IntType;
import hu.bme.mit.inf.ttmc.core.type.RatType;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.core.utils.impl.ExprUtils;

public class Z3TermTransformer {

	final Z3SymbolTable symbolTable;

	final Cache<com.microsoft.z3.Expr, Expr<?>> termToExpr;

	private static final int CACHE_SIZE = 1000;

	public Z3TermTransformer(final Z3SymbolTable symbolTable) {
		this.symbolTable = symbolTable;

		termToExpr = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
	}

	public Expr<?> toExpr(final com.microsoft.z3.Expr term) {
		try {
			return termToExpr.get(term, (() -> transform(term)));
		} catch (final ExecutionException e) {
			throw new AssertionError();
		}
	}

	////////

	private Expr<?> transform(final com.microsoft.z3.Expr term) {
		if (term instanceof com.microsoft.z3.BoolExpr) {
			return transformBool((com.microsoft.z3.BoolExpr) term);

		} else if (term instanceof com.microsoft.z3.IntExpr) {
			return transformInt((com.microsoft.z3.IntExpr) term);

		} else if (term instanceof com.microsoft.z3.ArithExpr) {
			return transformArith((com.microsoft.z3.ArithExpr) term);

		} else if (term instanceof com.microsoft.z3.ArrayExpr) {
			return arrayToExpr((com.microsoft.z3.ArrayExpr) term);

		}

		throw new AssertionError("Unhandled case: " + term.getClass());
	}

	////////

	private Expr<?> transformBool(final com.microsoft.z3.BoolExpr term) {
		if (term.isTrue()) {
			return Exprs.True();

		} else if (term.isFalse()) {
			return Exprs.False();

		} else if (term.isConst()) {
			return toConst(term);

		} else if (term.isNot()) {
			final com.microsoft.z3.Expr opTerm = term.getArgs()[0];
			final Expr<? extends BoolType> op = ExprUtils.cast(toExpr(opTerm), BoolType.class);
			return Exprs.Not(op);

		} else if (term.isOr()) {
			final com.microsoft.z3.Expr[] opTerms = term.getArgs();
			final List<Expr<? extends BoolType>> ops = toExprListOfType(opTerms, BoolType.class);
			return Exprs.Or(ops);

		} else if (term.isAnd()) {
			final com.microsoft.z3.Expr[] opTerms = term.getArgs();
			final List<Expr<? extends BoolType>> ops = toExprListOfType(opTerms, BoolType.class);
			return Exprs.And(ops);

		} else if (term.isImplies()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends BoolType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), BoolType.class);
			final Expr<? extends BoolType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), BoolType.class);
			return Exprs.Imply(lExprstOp, rightOp);

		} else if (term.isIff()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends BoolType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), BoolType.class);
			final Expr<? extends BoolType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), BoolType.class);
			return Exprs.Iff(lExprstOp, rightOp);

		} else if (term.isEq()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<?> lExprstOp = toExpr(lExprstOpTerm);
			final Expr<?> rightOp = toExpr(rightOpTerm);
			return Exprs.Eq(lExprstOp, rightOp);

		} else if (term.isLE()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends RatType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), RatType.class);
			final Expr<? extends RatType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), RatType.class);
			return Exprs.Leq(lExprstOp, rightOp);

		} else if (term.isLT()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends RatType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), RatType.class);
			final Expr<? extends RatType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), RatType.class);
			return Exprs.Lt(lExprstOp, rightOp);

		} else if (term.isGE()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends RatType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), RatType.class);
			final Expr<? extends RatType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), RatType.class);
			return Exprs.Geq(lExprstOp, rightOp);

		} else if (term.isGT()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends RatType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), RatType.class);
			final Expr<? extends RatType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), RatType.class);
			return Exprs.Gt(lExprstOp, rightOp);
		}

		throw new AssertionError("Unhandled case: " + term.toString());
	}

	private Expr<?> transformInt(final com.microsoft.z3.IntExpr term) {
		if (term.isIntNum()) {
			final long value = ((IntNum) term).getInt64();
			return Exprs.Int(value);

		} else if (term.isConst()) {
			return toConst(term);

		} else if (term.isAdd()) {
			final com.microsoft.z3.Expr[] opTerms = term.getArgs();
			final List<Expr<? extends IntType>> ops = toExprListOfType(opTerms, IntType.class);
			return Exprs.Add(ops);

		} else if (term.isMul()) {
			final com.microsoft.z3.Expr[] opTerms = term.getArgs();
			final List<Expr<? extends IntType>> ops = toExprListOfType(opTerms, IntType.class);
			return Exprs.Mul(ops);

		} else if (term.isIDiv()) {
			final com.microsoft.z3.Expr lExprstOpTerm = term.getArgs()[0];
			final com.microsoft.z3.Expr rightOpTerm = term.getArgs()[1];
			final Expr<? extends IntType> lExprstOp = ExprUtils.cast(toExpr(lExprstOpTerm), IntType.class);
			final Expr<? extends IntType> rightOp = ExprUtils.cast(toExpr(rightOpTerm), IntType.class);
			return Exprs.IntDiv(lExprstOp, rightOp);

		}

		throw new AssertionError("Unhandled case: " + term.toString());
	}

	private Expr<?> transformArith(final com.microsoft.z3.ArithExpr term) {
		if (term.isRatNum()) {
			final long num = ((RatNum) term).getNumerator().getInt64();
			final long denom = ((RatNum) term).getDenominator().getInt64();
			return Exprs.Rat(num, denom);

		} else if (term.isConst()) {
			return toConst(term);

		} else if (term.isAdd()) {
			final com.microsoft.z3.Expr[] opTerms = term.getArgs();
			final List<Expr<? extends RatType>> ops = toExprListOfType(opTerms, RatType.class);
			return Exprs.Add(ops);

		} else if (term.isMul()) {
			final com.microsoft.z3.Expr[] opTerms = term.getArgs();
			final List<Expr<? extends RatType>> ops = toExprListOfType(opTerms, RatType.class);
			return Exprs.Mul(ops);
		}

		throw new AssertionError("Unhandled case: " + term.toString());
	}

	private Expr<ArrayType<?, ?>> arrayToExpr(final com.microsoft.z3.ArrayExpr term) {
		throw new UnsupportedOperationException();
	}

	////////

	private Expr<?> toConst(final com.microsoft.z3.Expr term) {
		final FuncDecl funcDecl = term.getFuncDecl();
		final ConstDecl<?> constDecl = symbolTable.getConst(funcDecl);
		return constDecl.getRef();
	}

	private <T extends Type> List<Expr<? extends T>> toExprListOfType(final com.microsoft.z3.Expr[] terms,
			final Class<T> metaType) {
		final List<Expr<? extends T>> result = new LinkedList<>();
		for (final com.microsoft.z3.Expr term : terms) {
			result.add(ExprUtils.cast(toExpr(term), metaType));
		}
		return result;
	}

}
