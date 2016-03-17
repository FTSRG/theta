package hu.bme.mit.inf.ttmc.constraint.type.defaults;

import hu.bme.mit.inf.ttmc.constraint.type.RatType;
import hu.bme.mit.inf.ttmc.constraint.utils.TypeVisitor;

public abstract class AbstractRatType extends AbstractBaseType implements RatType {

	private static final int HASH_SEED = 385863;

	private static final String TYPE_LABEL = "Rat";

	@Override
	public final <P, R> R accept(final TypeVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public final int hashCode() {
		return HASH_SEED;
	}

	@Override
	public final boolean equals(final Object obj) {
		return (obj instanceof RatType);
	}

	@Override
	public final String toString() {
		return TYPE_LABEL;
	}

}
