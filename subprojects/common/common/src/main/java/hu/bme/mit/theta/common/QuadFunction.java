package hu.bme.mit.theta.common;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

@FunctionalInterface
public interface QuadFunction<T, U, V, W, R> {

	R apply(T t, U u, V v, W w);

	default <RR> QuadFunction<T, U, V, W, RR> andThen(final Function<? super R, ? extends RR> after) {
		checkNotNull(after);
		return (final T t, final U u, final V v, final W w) -> after.apply(apply(t, u, v, w));
	}
}
