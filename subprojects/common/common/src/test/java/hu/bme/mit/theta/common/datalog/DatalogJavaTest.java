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
package hu.bme.mit.theta.common.datalog;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.TupleN;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/*
 * Tests initial and incremental deduction with Datalog
 * The test:
 *   We model a simple directed graph using the relation edge(A, B) to denote A -> B.
 *   We then formulate two queries: which nodes are accessible from a given node, and is the graph irreflexive?
 */
public final class DatalogJavaTest {
	private final Datalog datalog;
	private final Datalog.Relation edge;
	private final Datalog.Relation successor;
	private final Datalog.Relation reflexive;
	private Node firstSubgraph1;
	private Node secondSubgraph1;

	public DatalogJavaTest() {
		datalog = Datalog.createProgram();
		//datalog.setDebug(true);
		edge = datalog.createRelation(2);
		successor = datalog.createRelation(2);
		reflexive = datalog.createRelation(1);

		Tuple2<Datalog.Variable, Datalog.Variable> accessibleVariables = Tuple2.of(datalog.getVariable(), datalog.getVariable());
		Datalog.Variable next = datalog.getVariable();
		successor.addRule(TupleN.of(accessibleVariables), Set.of(Tuple2.of(edge, TupleN.of(accessibleVariables.get1(), accessibleVariables.get2()))));
		successor.addRule(TupleN.of(accessibleVariables), Set.of(Tuple2.of(edge, TupleN.of(accessibleVariables.get1(), next)), Tuple2.of(successor, TupleN.of(next, accessibleVariables.get2()))));

		Datalog.Variable reflexivity = datalog.getVariable();
		reflexive.addRule(TupleN.of(List.of(reflexivity)), Set.of(Tuple2.of(edge, TupleN.of(reflexivity, reflexivity))));
	}

	@Test
	public void testInitial() {
		List<Node> firstSubgraph = new ArrayList<>();
		for (int i = 0; i < 10; ++i) {
			firstSubgraph.add(new Node('A', i));
			if (i > 0) {
				edge.addFact(TupleN.of(firstSubgraph.get(i - 1), firstSubgraph.get(i)));
			}
		}
		List<Node> secondSubgraph = new ArrayList<>();
		for (int i = 0; i < 15; ++i) {
			secondSubgraph.add(new Node('B', i));
			if (i > 0) {
				edge.addFact(TupleN.of(secondSubgraph.get(i - 1), secondSubgraph.get(i)));
			}
		}
		assertEquals(0, reflexive.getElements().size());
		assertEquals(45 + 105, successor.getElements().size());
		firstSubgraph1 = firstSubgraph.get(0);
		secondSubgraph1 = secondSubgraph.get(0);
	}

	@Test
	public void testIncremental() {
		testInitial();
		edge.addFact(TupleN.of(firstSubgraph1, secondSubgraph1));
		assertEquals(0, reflexive.getElements().size());
		assertEquals(45 + 105 + 15, successor.getElements().size());
		edge.addFact(TupleN.of(firstSubgraph1, firstSubgraph1));
		assertEquals(1, reflexive.getElements().size());
		assertEquals(45 + 105 + 15 + 1, successor.getElements().size());
	}

	private static class Node implements DatalogArgument {
		private final int i;
		private final char c;

		private Node(char a, int i) {
			this.c = a;
			this.i = i;
		}

		@Override
		public String toString() {
			return "Node" + c + "{" +
					"i=" + i +
					'}';
		}
	}

}
