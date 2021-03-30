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
package hu.bme.mit.theta.solver;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import hu.bme.mit.theta.solver.impl.StackImpl;

public class StackTest {
	@Test
	public void testPushPop() {
		final Stack<String> stack = new StackImpl<>();
		assertEquals(0, stack.toCollection().size());
		stack.push();
		assertEquals(0, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{}, stack.toCollection().toArray());
		stack.add("A");
		assertEquals(1, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A",}, stack.toCollection().toArray());
		stack.add("B");
		assertEquals(2, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B"}, stack.toCollection().toArray());
		stack.push();
		assertEquals(2, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B"}, stack.toCollection().toArray());
		stack.add("C");
		assertEquals(3, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B", "C"}, stack.toCollection().toArray());
		stack.add("D");
		assertEquals(4, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B", "C", "D"}, stack.toCollection().toArray());
		stack.pop();
		assertEquals(2, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B"}, stack.toCollection().toArray());
		stack.pop();
		assertEquals(0, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{}, stack.toCollection().toArray());
	}

	@Test
	public void testPop2() {
		final Stack<String> stack = new StackImpl<>();
		assertEquals(0, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{}, stack.toCollection().toArray());
		stack.push();
		assertEquals(0, stack.toCollection().size());
		stack.add("A");
		assertEquals(1, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A"}, stack.toCollection().toArray());
		stack.push();
		assertEquals(1, stack.toCollection().size());
		stack.add("B");
		assertEquals(2, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B"}, stack.toCollection().toArray());
		stack.push();
		assertEquals(2, stack.toCollection().size());
		stack.add("C");
		assertEquals(3, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A", "B", "C"}, stack.toCollection().toArray());
		stack.pop(2);
		assertEquals(1, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{"A"}, stack.toCollection().toArray());
		stack.pop();
		assertEquals(0, stack.toCollection().size());
		Assert.assertArrayEquals(new String[]{}, stack.toCollection().toArray());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyPop() {
		final Stack<String> stack = new StackImpl<>();

		stack.push();
		stack.push();
		stack.pop();
		stack.pop();
		stack.pop();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testClear() {
		final Stack<String> stack = new StackImpl<>();

		stack.push();
		stack.push();
		stack.clear();
		stack.push();
		stack.pop();
		stack.pop();
	}
}
