package hu.bme.mit.inf.ttmc.formalism.ta.constr.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.inf.ttmc.formalism.ta.constr.Clock;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffConstr;

public abstract class AbstractDiffConstr implements DiffConstr {

	private final Clock leftClock;
	private final Clock rightClock;
	private final int bound;

	private volatile int hashCode = 0;

	protected AbstractDiffConstr(final Clock leftClock, final Clock rightClock, final int bound) {
		this.leftClock = checkNotNull(leftClock);
		this.rightClock = checkNotNull(rightClock);
		this.bound = bound;
	}

	@Override
	public final Clock getLeftClock() {
		return leftClock;
	}

	@Override
	public final Clock getRightClock() {
		return rightClock;
	}

	@Override
	public final int getBound() {
		return bound;
	}

	@Override
	public final int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = getHashSeed();
			result = 31 * result + leftClock.hashCode();
			result = 31 * result + rightClock.hashCode();
			result = 31 * result + bound;
			hashCode = result;
		}
		return result;
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("CC");
		sb.append("(");
		sb.append(leftClock.getName());
		sb.append(" - ");
		sb.append(rightClock.getName());
		sb.append(" ");
		sb.append(getOperatorLabel());
		sb.append(" ");
		sb.append(bound);
		sb.append(")");
		return sb.toString();
	}

	protected abstract int getHashSeed();

	protected abstract String getOperatorLabel();

}
