package hu.bme.mit.theta.common.datalog;

/*
 * Datalog engine using naive (enumerative) evaluation.
 * Limitations:
 *  - Only relations are supported right now, simple atoms are not
 *  - There is a performance penalty for specific rulesets, see
 *    http://pages.cs.wisc.edu/~paris/cs838-s16/lecture-notes/lecture8.pdf
 */

import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.TupleN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class Datalog {
	private final List<Relation> relations;
	private boolean debug = false;

	protected Datalog() {
		relations = new ArrayList<>();
	}

	public static Datalog createProgram() {
		return new Datalog();
	}

	/*
	 * Datalog program (facts, deduction rules and queries) in a basic String representation.
	 * Only supports queries of entire relations right now.
	 */
	public static String runProgram(String relations) {
		Datalog datalog = new Datalog();
		StringBuilder ret = new StringBuilder();
		Map<String, Relation> relationMap = new HashMap<>();
		for (String expression : relations.split("\\.")) {
			String nospace = expression.replaceAll("[ \t\n]", "");
			if (nospace.matches("([a-z_][a-zA-Z0-9_]*)\\(([a-z_0-9][a-zA-Z0-9_]*)(,[a-z_0-9][a-zA-Z_0-9]*)*\\)")) { //fact assertion
				String[] splitString = nospace.split("\\(");
				String[] arguments = splitString[1].replaceAll("\\)", "").split(",");
				relationMap.putIfAbsent(splitString[0], datalog.createRelation(arguments.length));
				TupleN<DatalogArgument> argumentTuple = TupleN.of(Arrays.stream(arguments).map(StringDatalogArgument::new).collect(Collectors.toList()));
				relationMap.get(splitString[0]).addFact(argumentTuple);
			} else if (nospace.matches("([a-z_][a-zA-Z_0-9]*)\\(([a-zA-Z_][a-zA-Z0-9_]*)(,[a-zA-Z_][a-zA-Z0-9_]*)*\\):-([a-z_][a-zA-Z_]*)\\(([a-zA-Z_][a-zA-Z0-9_]*)(,[a-zA-Z_][a-zA-Z0-9_]*)*\\)(,([a-z_][a-zA-Z_]*)\\(([a-zA-Z_][a-zA-Z0-9_]*)(,[a-zA-Z_][a-zA-Z0-9_]*)*\\))*")) { //deduction rule
				String[] splitExpression = nospace.split(":-");

				Map<String, Variable> variableMap = new HashMap<>();
				String[] splitString = splitExpression[0].split("\\(");
				String[] arguments = splitString[1].replaceAll("\\)", "").split(",");
				relationMap.putIfAbsent(splitString[0], datalog.createRelation(arguments.length));
				TupleN<Variable> argumentTuple = TupleN.of(Arrays.stream(arguments).map(s -> {
					variableMap.putIfAbsent(s, datalog.getVariable());
					return variableMap.get(s);
				}).collect(Collectors.toList()));

				Set<Tuple2<Relation, TupleN<Variable>>> dependencies = new LinkedHashSet<>();
				for (String dependency : splitExpression[1].split("\\),")) {
					String[] dsplitString = dependency.split("\\(");
					String[] darguments = dsplitString[1].replaceAll("\\)", "").split(",");
					relationMap.putIfAbsent(dsplitString[0], datalog.createRelation(darguments.length));
					TupleN<Variable> dargumentTuple = TupleN.of(Arrays.stream(darguments).map(s -> {
						variableMap.putIfAbsent(s, datalog.getVariable());
						return variableMap.get(s);
					}).collect(Collectors.toList()));
					dependencies.add(Tuple2.of(relationMap.get(dsplitString[0]), dargumentTuple));
				}
				relationMap.get(splitString[0]).addRule(argumentTuple, dependencies);

			} else if (nospace.matches("([a-z_][a-zA-Z_]*)\\?")) { //query
				String noquestion = nospace.replaceAll("\\?", "");
				if (!relationMap.containsKey(noquestion))
					throw new RuntimeException("Query " + nospace + " does not query an existing relation!");
				for (TupleN<DatalogArgument> element : relationMap.get(noquestion).getElements()) {
					StringBuilder stringBuilder = new StringBuilder(noquestion + "(");
					for (Object o : element) {
						stringBuilder.append(o.toString()).append(",");
					}
					ret.append(stringBuilder.substring(0, stringBuilder.length() - 1)).append(").").append("\r\n");
				}
			} else {
				throw new RuntimeException("Input " + nospace + " does not match any type of expression!");
			}
		}
		return ret.toString();
	}

	private void refresh() {
		int cnt;
		do {
			cnt = 0;
			for (Relation relation : relations) {
				int i = relation.calc();
				if (debug)
					System.out.println(i + " new facts");
				cnt += i;
			}
			if (debug)
				System.out.println("====");
		} while (cnt > 0);
		for (Relation relation : relations) {
			relation.flush();
		}
	}

	public Relation createRelation(int n) {
		checkState(n > 0, "Relation must have positive arity");
		Relation ret = new Relation(n);
		relations.add(ret);
		return ret;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public Variable getVariable() {
		return new Variable();
	}

	public static class Variable {
	}

	public class Relation {
		private final Set<TupleN<DatalogArgument>> elements;
		private final Set<TupleN<DatalogArgument>> newElements;
		private final Set<TupleN<DatalogArgument>> toAdd;
		private final Set<Tuple2<TupleN<Variable>, Set<Tuple2<Relation, TupleN<Variable>>>>> rules;
		private final int arity;

		private Relation(int n) {
			this.arity = n;
			elements = new LinkedHashSet<>();
			newElements = new LinkedHashSet<>();
			rules = new LinkedHashSet<>();
			toAdd = new LinkedHashSet<>();
		}

		public void addFact(TupleN<DatalogArgument> fact) {
			checkState(fact.arity() == arity);
			newElements.add(fact);
			if (debug) {
				System.out.println();
				System.out.println("New fact: " + fact);
			}
			refresh();
		}

		public void addRule(TupleN<Variable> args, Set<Tuple2<Relation, TupleN<Variable>>> dependencies) {
			checkState(args.arity() == arity);
			for (Tuple2<Relation, TupleN<Variable>> dependency : dependencies) {
				checkState(dependency.get1().arity == dependency.get2().arity());
			}
			rules.add(Tuple2.of(args, new LinkedHashSet<>(dependencies)));
			refresh();
		}

		public Collection<TupleN<DatalogArgument>> getElements() {
			ArrayList<TupleN<DatalogArgument>> ret = new ArrayList<>(elements);
			ret.addAll(newElements);
			return ImmutableList.copyOf(ret);
		}

		public int getArity() {
			return arity;
		}

		private boolean validate(List<Tuple2<Relation, TupleN<Variable>>> clauses, Map<Variable, DatalogArgument> currentAssignments, List<Map<Variable, DatalogArgument>> assignments) {
			if (clauses.size() > 0) {
				Tuple2<Relation, TupleN<Variable>> clause = clauses.get(0);
				clauses.remove(0);
				for (TupleN<DatalogArgument> element : clause.get1().elements) {
					Map<Variable, DatalogArgument> nextAssignments = new HashMap<>(currentAssignments);
					if (!putAssignments(nextAssignments, clause, element)) {
						validate(clauses, nextAssignments, assignments);
					}
				}
				for (TupleN<DatalogArgument> element : clause.get1().newElements) {
					Map<Variable, DatalogArgument> nextAssignments = new HashMap<>(currentAssignments);
					if (!putAssignments(nextAssignments, clause, element)) {
						validate(clauses, nextAssignments, assignments);
					}
				}
				return assignments.size() > 0;
			} else {
				assignments.add(currentAssignments);
				return true;
			}
		}

		private int calc() {
			int cnt = 0;
			for (Tuple2<TupleN<Variable>, Set<Tuple2<Relation, TupleN<Variable>>>> rule : rules) {
				for (Tuple2<Relation, TupleN<Variable>> clause : rule.get2()) {
					for (TupleN<DatalogArgument> newElement : clause.get1().newElements) {
						cnt = addElements(cnt, rule, clause, newElement);
					}
					for (TupleN<DatalogArgument> newElement : clause.get1().toAdd) {
						cnt = addElements(cnt, rule, clause, newElement);
					}
				}
			}
			return cnt;
		}

		private int addElements(int cnt, Tuple2<TupleN<Variable>, Set<Tuple2<Relation, TupleN<Variable>>>> rule, Tuple2<Relation, TupleN<Variable>> clause, TupleN<DatalogArgument> newElement) {
			Map<Variable, DatalogArgument> assignments = new HashMap<>();
			if (putAssignments(assignments, clause, newElement))
				return cnt;
			if (debug) {
				if (this == relations.get(1)) System.out.println("\tChecking " + newElement);
			}
			ArrayList<Tuple2<Relation, TupleN<Variable>>> validators = new ArrayList<>(rule.get2());
			validators.remove(clause);
			List<Map<Variable, DatalogArgument>> satisfyingAssignments = new ArrayList<>();
			if (validate(validators, assignments, satisfyingAssignments)) {
				for (Map<Variable, DatalogArgument> satisfyingAssignment : satisfyingAssignments) {
					List<DatalogArgument> arguments = new ArrayList<>();
					for (Object o : rule.get1()) {
						Variable v = (Variable) o;
						checkState(satisfyingAssignment.containsKey(v), "Not all variables are bound!");
						arguments.add(satisfyingAssignment.get(v));
					}
					TupleN<DatalogArgument> item = TupleN.of(arguments);
					if (!elements.contains(item) && !newElements.contains(item) && !toAdd.contains(item)) {
						toAdd.add(item);
						if (debug) {
							if (this == relations.get(1)) System.out.println("Adding " + item);
						}
						++cnt;
					}
				}

			}
			return cnt;
		}

		private boolean putAssignments(Map<Variable, DatalogArgument> assignments, Tuple2<Relation, TupleN<Variable>> validatorClause, TupleN<DatalogArgument> element) {
			for (int i = 0; i < element.arity(); i++) {
				if (assignments.containsKey(validatorClause.get2().get(i))) {
					if (!assignments.get(validatorClause.get2().get(i)).equals(element.get(i))) return true;
				} else {
					assignments.put(validatorClause.get2().get(i), element.get(i));
				}
			}
			return false;
		}

		private void flush() {
			if (debug) {
				StringBuilder stringBuilder = new StringBuilder("[");
				newElements.forEach(objects -> stringBuilder.append(objects.toString()).append(", "));
				if (this == relations.get(1))
					System.out.println("Promoting newElements to elements: " + stringBuilder.append("]").toString());
			}
			elements.addAll(newElements);
			newElements.clear();

			if (debug) {
				StringBuilder stringBuilder1 = new StringBuilder("[");
				toAdd.forEach(objects -> stringBuilder1.append(objects.toString()).append(", "));
				if (this == relations.get(1))
					System.out.println("Promoting toAdd to newElements: " + stringBuilder1.append("]").toString());
			}
			newElements.addAll(toAdd);
			toAdd.clear();
		}


	}
}
