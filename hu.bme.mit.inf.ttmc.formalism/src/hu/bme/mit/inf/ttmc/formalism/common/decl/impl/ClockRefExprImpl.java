package hu.bme.mit.inf.ttmc.formalism.common.decl.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.inf.ttmc.core.type.RatType;
import hu.bme.mit.inf.ttmc.core.utils.ExprVisitor;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ClockDecl;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ClockRefExpr;

final class ClockRefExprImpl implements ClockRefExpr {

	private static final int HASH_SEED = 1951;

	private final ClockDecl decl;

	private volatile int hashCode = 0;

	ClockRefExprImpl(final ClockDecl clockDecl) {
		this.decl = checkNotNull(clockDecl);
	}

	@Override
	public ClockDecl getDecl() {
		return decl;
	}

	@Override
	public RatType getType() {
		return decl.getType();
	}

	@Override
	public <P, R> R accept(final ExprVisitor<? super P, ? extends R> visitor, final P param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public final int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + decl.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof ClockRefExpr) {
			final ClockRefExpr that = (ClockRefExpr) obj;
			return this.getDecl().equals(that.getDecl());
		} else {
			return false;
		}
	}

	@Override
	public final String toString() {
		return getDecl().getName();
	}

}
