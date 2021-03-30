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
package hu.bme.mit.theta.solver.z3;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Interpolant;
import hu.bme.mit.theta.solver.ItpMarker;
import hu.bme.mit.theta.solver.ItpMarkerTree;
import hu.bme.mit.theta.solver.ItpPattern;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverStatus;
import hu.bme.mit.theta.solver.Stack;
import hu.bme.mit.theta.solver.impl.StackImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

final class Z3ItpSolver implements ItpSolver, Solver {

	private final Z3TransformationManager transformationManager;
	private final Z3TermTransformer termTransformer;

	private final com.microsoft.z3.InterpolationContext z3Context;
	private final com.microsoft.z3.Solver z3Solver;

	private final Z3Solver solver;

	private final Stack<Z3ItpMarker> markers;

	public Z3ItpSolver(final Z3SymbolTable symbolTable, final Z3TransformationManager transformationManager,
					   final Z3TermTransformer termTransformer, final com.microsoft.z3.InterpolationContext z3Context,
					   final com.microsoft.z3.Solver z3Solver) {
		this.transformationManager = transformationManager;
		this.termTransformer = termTransformer;
		this.z3Context = z3Context;
		this.z3Solver = z3Solver;

		solver = new Z3Solver(symbolTable, transformationManager, termTransformer, z3Context, z3Solver);

		markers = new StackImpl<>();
	}

	@Override
	public ItpPattern createTreePattern(final ItpMarkerTree<? extends ItpMarker> root) {
		checkNotNull(root);
		return Z3ItpPattern.of(root);
	}

	@Override
	 public Z3ItpMarker createMarker() {
		final Z3ItpMarker marker = new Z3ItpMarker();
		markers.add(marker);
		return marker;
	}

	@Override
	public void add(final ItpMarker marker, final Expr<BoolType> assertion) {
		checkNotNull(marker);
		checkNotNull(assertion);
		checkArgument(markers.toCollection().contains(marker), "Marker not found in solver");
		final Z3ItpMarker z3Marker = (Z3ItpMarker) marker;
		final com.microsoft.z3.BoolExpr term = (com.microsoft.z3.BoolExpr) transformationManager.toTerm(assertion);
		solver.add(assertion, term);
		z3Marker.add(term);
	}

	@Override
	public Interpolant getInterpolant(final ItpPattern pattern) {
		checkState(solver.getStatus() == SolverStatus.UNSAT, "Cannot get interpolant if status is not UNSAT.");
		checkArgument(pattern instanceof Z3ItpPattern);
		final Z3ItpPattern z3ItpPattern = (Z3ItpPattern) pattern;

		final com.microsoft.z3.Expr proof = z3Solver.getProof();
		final com.microsoft.z3.Expr term = patternToTerm(z3ItpPattern.getRoot());
		final com.microsoft.z3.Params params = z3Context.mkParams();

		final com.microsoft.z3.BoolExpr[] itpArray = z3Context.GetInterpolant(proof, term, params);
		final List<Expr<BoolType>> itpList = new LinkedList<>();

		for (final com.microsoft.z3.BoolExpr itpTerm : itpArray) {
			@SuppressWarnings("unchecked") final Expr<BoolType> itpExpr = (Expr<BoolType>) termTransformer.toExpr(itpTerm);
			itpList.add(itpExpr);
		}

		final Map<ItpMarker, Expr<BoolType>> itpMap = new HashMap<>();
		buildItpMapFormList(z3ItpPattern.getRoot(), itpList, itpMap);

		return new Z3Interpolant(itpMap);
	}

	private com.microsoft.z3.BoolExpr patternToTerm(final ItpMarkerTree<Z3ItpMarker> markerTree) {
		final Collection<com.microsoft.z3.BoolExpr> opTerms = new LinkedList<>();

		final Z3ItpMarker marker = (Z3ItpMarker) markerTree.getMarker();
		opTerms.addAll(marker.getTerms());

		for (final ItpMarkerTree<Z3ItpMarker> child : markerTree.getChildren()) {
			final com.microsoft.z3.BoolExpr childTerm = patternToTerm(child);
			opTerms.add(childTerm);
		}

		final com.microsoft.z3.BoolExpr andTerm = z3Context
				.mkAnd(opTerms.toArray(new com.microsoft.z3.BoolExpr[opTerms.size()]));
		final com.microsoft.z3.BoolExpr term = z3Context.MkInterpolant(andTerm);
		return term;
	}

	private void buildItpMapFormList(final ItpMarkerTree<Z3ItpMarker> pattern, final List<Expr<BoolType>> itpList,
									 final Map<ItpMarker, Expr<BoolType>> itpMap) {
		for (final ItpMarkerTree<Z3ItpMarker> child : pattern.getChildren()) {
			buildItpMapFormList(child, itpList, itpMap);
		}
		final ItpMarker marker = pattern.getMarker();
		final Expr<BoolType> itpExpr = itpList.get(0);
		itpMap.put(marker, itpExpr);
		itpList.remove(0);
	}

	@Override
	public Collection<? extends ItpMarker> getMarkers() {
		return markers.toCollection();
	}

	// delegate

	@Override
	public void add(final Expr<BoolType> assertion) {
		checkNotNull(assertion);
		solver.add(assertion);
	}

	@Override
	public SolverStatus check() {
		return solver.check();
	}

	@Override
	public void push() {
		markers.push();
		for (final Z3ItpMarker marker : markers) {
			marker.push();
		}
		solver.push();
	}

	@Override
	public void pop(final int n) {
		markers.pop(n);
		for (final Z3ItpMarker marker : markers) {
			marker.pop(n);
		}
		solver.pop(n);
	}

	@Override
	public void reset() {
		solver.reset();
	}

	@Override
	public SolverStatus getStatus() {
		return solver.getStatus();
	}

	@Override
	public Valuation getModel() {
		return solver.getModel();
	}

	@Override
	public Collection<Expr<BoolType>> getAssertions() {
		return solver.getAssertions();
	}

}
