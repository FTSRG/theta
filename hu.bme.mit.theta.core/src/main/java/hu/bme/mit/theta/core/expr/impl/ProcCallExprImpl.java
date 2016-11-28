package hu.bme.mit.theta.core.expr.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.ProcCallExpr;
import hu.bme.mit.theta.core.type.ProcType;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.ExprVisitor;

class ProcCallExprImpl<ReturnType extends Type> implements ProcCallExpr<ReturnType> {

	private final static int HASH_SEED = 1471;
	private volatile int hashCode = 0;

	private final Expr<? extends ProcType<? extends ReturnType>> proc;
	private final ImmutableList<Expr<?>> params;

	ProcCallExprImpl(final Expr<? extends ProcType<? extends ReturnType>> proc, final List<? extends Expr<?>> params) {
		this.proc = checkNotNull(proc);
		this.params = ImmutableList.copyOf(checkNotNull(params));
	}

	@Override
	public final Expr<? extends ProcType<? extends ReturnType>> getProc() {
		return proc;
	}

	@Override
	public final List<? extends Expr<? extends Type>> getParams() {
		return params;
	}

	@Override
	public final ReturnType getType() {
		return getProc().getType().getReturnType();
	}

	@Override
	public final <P, R> R accept(final ExprVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public final int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + proc.hashCode();
			result = 31 * result + params.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof ProcCallExpr<?>) {
			final ProcCallExpr<?> that = (ProcCallExpr<?>) obj;
			return this.getProc().equals(that.getProc()) && this.getParams().equals(that.getParams());
		} else {
			return false;
		}
	}

	@Override
	public final String toString() {
		return ObjectUtils.toStringBuilder("Call").add(proc).addAll(params).toString();
	}

}
