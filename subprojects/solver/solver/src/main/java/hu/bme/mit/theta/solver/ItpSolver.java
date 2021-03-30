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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;

/**
 * An extension of the {@link Solver} interface, which also supports interpolation.
 * It can create {@link ItpMarker}s, and expressions can be added to these markers.
 * The markers can form different {@link ItpPattern}s, e.g., binary or sequence.
 * First, the expressions have to be checked with {@link #check()}. Then if the
 * expressions are unsatisfiable, an interpolant can be calculated with
 * {@link #getInterpolant(ItpPattern)}.
 */
public interface ItpSolver extends SolverBase {
	/**
	 * Create a binary pattern, which is a sequence of two markers: A and B.
	 *
	 * @param markerA Marker A
	 * @param markerB Marker B
	 * @return Binary interpolant pattern
	 */
	default ItpPattern createBinPattern(final ItpMarker markerA, final ItpMarker markerB) {
		checkNotNull(markerA);
		checkNotNull(markerB);

		return createSeqPattern(Arrays.asList(markerA, markerB));
	}

	/**
	 * Create a sequence pattern, which is a linear sequence of N markers.
	 *
	 * @param markers Markers
	 * @return Sequence interpolant pattern
	 */
	default ItpPattern createSeqPattern(final List<? extends ItpMarker> markers) {
		checkNotNull(markers);
		checkArgument(!markers.isEmpty());

		ItpMarkerTree<ItpMarker> current = null;

		for (final var marker : markers) {
			if (current == null) {
				current = ItpMarkerTree.Leaf(marker);
			} else {
				current = ItpMarkerTree.Tree(marker, current);
			}
		}
		return createTreePattern(current);
	}

	/**
	 * Create a tree pattern, in which each node can have multiple children
	 *
	 * @param root Root of the marker tree
	 * @return Tree interpolant pattern
	 */
	ItpPattern createTreePattern(final ItpMarkerTree<? extends ItpMarker> root);

	/**
	 * Create a new marker.
	 *
	 * @return Marker
	 */
	ItpMarker createMarker();

	/**
	 * Add an expression to the solver and the given marker.
	 *
	 * @param marker    Marker
	 * @param assertion Expression
	 */
	void add(final ItpMarker marker, final Expr<BoolType> assertion);

	/**
	 * Add a collection of expressions to the solver and the given marker.
	 *
	 * @param marker     Marker
	 * @param assertions Expression
	 */
	default void add(final ItpMarker marker, final Iterable<? extends Expr<BoolType>> assertions) {
		checkNotNull(marker);
		checkNotNull(assertions);
		for (final Expr<BoolType> assertion : assertions) {
			add(marker, assertion);
		}
	}

	/**
	 * Get the interpolant for the currently added expressions. Should only be called if {@link #check()}
	 * was already called and the result is UNSAT.
	 *
	 * @param pattern Pattern
	 * @return Interpolant
	 */
	Interpolant getInterpolant(final ItpPattern pattern);

	Collection<? extends ItpMarker> getMarkers();

}
