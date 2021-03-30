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
package hu.bme.mit.theta.solver.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import hu.bme.mit.theta.solver.Stack;

public class StackImpl<T> implements Stack<T> {

	public final List<T> items;
	private final List<Integer> sizes;

	public StackImpl() {
		items = new LinkedList<>();
		sizes = new LinkedList<>();
	}

	@Override
	public void add(final T elem) {
		items.add(elem);
	}

	@Override
	public void add(final Collection<? extends T> elems) {
		items.addAll(elems);
	}

	@Override
	public void push() {
		sizes.add(items.size());
	}

	@Override
	public void pop(final int n) {
		checkArgument(n > 0, "Number of pops must be positive");
		final int depth = sizes.size();
		checkArgument(depth >= n, "Stack not deep enough to pop " + n);

		final int size = sizes.get(depth - n);
		sizes.subList(depth - n, depth).clear();
		items.subList(size, items.size()).clear();
	}

	@Override
	public Collection<T> toCollection() {
		return Collections.unmodifiableCollection(items);
	}

	@Override
	public Iterator<T> iterator() {
		return items.iterator();
	}

	@Override
	public void clear() {
		items.clear();
		sizes.clear();
	}
}
