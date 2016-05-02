package hu.bme.mit.inf.ttmc.formalism.ta.constr.impl;

import static hu.bme.mit.inf.ttmc.core.expr.impl.Exprs.Int;
import static hu.bme.mit.inf.ttmc.core.expr.impl.Exprs.Lt;
import static hu.bme.mit.inf.ttmc.core.expr.impl.Exprs.Sub;

import hu.bme.mit.inf.ttmc.core.expr.LtExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ClockRefExpr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.Clock;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffLtConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.utils.ConstrVisitor;

final class DiffLtConstrImpl extends AbstractDiffConstr implements DiffLtConstr {

	private static final int HASH_SEED = 8377;

	private static final String OPERATOR_LABEL = "<";

	private volatile LtExpr expr = null;

	DiffLtConstrImpl(final Clock leftClock, final Clock rightClock, final int bound) {
		super(leftClock, rightClock, bound);
	}

	@Override
	public LtExpr asExpr() {
		LtExpr result = expr;
		if (result == null) {
			final ClockRefExpr leftRef = getLeftClock().asDecl().getRef();
			final ClockRefExpr rightRef = getRightClock().asDecl().getRef();
			result = Lt(Sub(leftRef, rightRef), Int(getBound()));
			expr = result;
		}
		return result;
	}

	@Override
	public <P, R> R accept(final ConstrVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof DiffLtConstr) {
			final DiffLtConstr that = (DiffLtConstr) obj;
			return this.getBound() == that.getBound() && this.getLeftClock().equals(that.getLeftClock())
					&& this.getRightClock().equals(that.getRightClock());
		} else {
			return false;
		}
	}

	@Override
	protected int getHashSeed() {
		return HASH_SEED;
	}

	@Override
	protected String getOperatorLabel() {
		return OPERATOR_LABEL;
	}

}
