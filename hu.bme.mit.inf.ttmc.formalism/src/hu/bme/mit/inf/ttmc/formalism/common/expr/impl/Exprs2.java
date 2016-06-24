package hu.bme.mit.inf.ttmc.formalism.common.expr.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.formalism.common.expr.NullExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.PrimedExpr;

public final class Exprs2 {

	private static final NullExpr<?> NULL_EXPR;

	static {
		NULL_EXPR = new NullExprImpl<>();
	}

	private Exprs2() {
	}

	public static <T extends Type> PrimedExpr<T> Prime(final Expr<? extends T> op) {
		checkNotNull(op);
		return new PrimedExprImpl<>(op);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Type> NullExpr<T> Null() {
		return (NullExpr<T>) NULL_EXPR;
	}

}
