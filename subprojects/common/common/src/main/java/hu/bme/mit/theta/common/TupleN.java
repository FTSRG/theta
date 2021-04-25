/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.common;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class TupleN<T> extends Tuple {

	private TupleN(final List<T> l) {
		super(l);
	}

	@SafeVarargs
	public static <T> TupleN<T> of(final T... content) {
		return new TupleN<T>(ImmutableList.copyOf(content));
	}

	public static <T> TupleN<T> of(final List<T> l) {
		return new TupleN<>(l);
	}

	public static <T> TupleN<T> of(final Tuple2<T, T> tuple) {
		@SuppressWarnings("unchecked") TupleN<T> ret = new TupleN<>((List<T>) tuple.toList());
		return ret;
	}

	public static <T> TupleN<T> of(final Tuple3<T, T, T> tuple) {
		@SuppressWarnings("unchecked") TupleN<T> ret = new TupleN<>((List<T>) tuple.toList());
		return ret;
	}

	public static <T> TupleN<T> of(final Tuple4<T, T, T, T> tuple) {
		@SuppressWarnings("unchecked") TupleN<T> ret = new TupleN<>((List<T>) tuple.toList());
		return ret;
	}

	public static <T> TupleN<T> of(final Tuple5<T, T, T, T, T> tuple) {
		@SuppressWarnings("unchecked") TupleN<T> ret = new TupleN<>((List<T>) tuple.toList());
		return ret;
	}

	public static <T> TupleN<T> of(final Tuple6<T, T, T, T, T, T> tuple) {
		@SuppressWarnings("unchecked") TupleN<T> ret = new TupleN<>((List<T>) tuple.toList());
		return ret;
	}

	public static <T> TupleN<T> of(final Tuple7<T, T, T, T, T, T, T> tuple) {
		@SuppressWarnings("unchecked") TupleN<T> ret = new TupleN<>((List<T>) tuple.toList());
		return ret;
	}

	public T get(int i) {
		@SuppressWarnings("unchecked") final T result = (T) elem(i);
		return result;
	}

}
