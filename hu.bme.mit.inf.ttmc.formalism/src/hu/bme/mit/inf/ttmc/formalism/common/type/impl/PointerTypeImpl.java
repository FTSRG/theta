package hu.bme.mit.inf.ttmc.formalism.common.type.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import hu.bme.mit.inf.ttmc.core.expr.LitExpr;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.core.utils.TypeVisitor;
import hu.bme.mit.inf.ttmc.formalism.common.type.PointerType;

final class PointerTypeImpl<T extends Type> implements PointerType<T> {

	private static final String TYPE_LABEL = "Pointer";

	private static final int HASH_SEED = 6619;
	private volatile int hashCode = 0;

	private final T type;

	PointerTypeImpl(final T type) {
		this.type = checkNotNull(type);
	}

	@Override
	public T getType() {
		return type;
	}

	@Override
	public LitExpr<? extends Type> getAny() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public boolean isLeq(final Type other) {
		if (other instanceof PointerType) {
			final PointerType<?> that = (PointerType<?>) other;
			return this.getType().isLeq(that.getType());
		} else {
			return false;
		}
	}

	@Override
	public Optional<? extends Type> meet(final Type type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public Optional<? extends Type> join(final Type type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public <P, R> R accept(final TypeVisitor<? super P, ? extends R> visitor, final P param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + type.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof PointerType) {
			final PointerType<?> that = (PointerType<?>) obj;
			return this.getType().equals(that.getType());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(TYPE_LABEL);
		sb.append("(");
		sb.append(type);
		sb.append(")");
		return sb.toString();
	}

}
