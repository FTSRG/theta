package hu.bme.mit.inf.ttmc.constraint.expr.impl;

import hu.bme.mit.inf.ttmc.constraint.expr.TrueExpr;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.constraint.type.impl.Types;
import hu.bme.mit.inf.ttmc.constraint.utils.ExprVisitor;

final class TrueExprImpl extends AbstractBoolLitExpr implements TrueExpr {

	private static final int HASH_SEED = 242181;

	private static final String OPERATOR = "True";

	TrueExprImpl() {
	}

	@Override
	public boolean getValue() {
		return true;
	}

	@Override
	public BoolType getType() {
		return Types.Bool();
	}

	@Override
	public <P, R> R accept(final ExprVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public int hashCode() {
		return HASH_SEED;
	}

	@Override
	public boolean equals(final Object obj) {
		return (obj instanceof TrueExpr);
	}

	@Override
	public String toString() {
		return OPERATOR;
	}

}
