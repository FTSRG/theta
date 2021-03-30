package hu.bme.mit.theta.common;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

@FunctionalInterface
public interface QuintFunction<T, U, V, W, X, R> {

	R apply(T t, U u, V v, W w, X x);

	default <RR> QuintFunction<T, U, V, W, X, RR> andThen(final Function<? super R, ? extends RR> after) {
		checkNotNull(after);
		return (final T t, final U u, final V v, final W w, final X x) -> after.apply(apply(t, u, v, w, x));
	}
}
