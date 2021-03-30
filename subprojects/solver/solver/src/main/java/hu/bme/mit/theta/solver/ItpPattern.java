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

import java.util.Collection;
import java.util.List;

/**
 * Interface for an element of an interpolation pattern.
 */
public interface ItpPattern {

	/**
	 * ItpPatter visitor function
	 * @param visitor The visitor
	 * @param <E> The return type of the visitor
	 * @return Returns the result of the visitor
	 */
	<E> E visit(final ItpPatternVisitor<E> visitor);

	/**
	 * Interface for a binary interpolation pattern
	 */
	interface Binary<T extends ItpMarker>  extends ItpPattern {
		T getA();
		T getB();

		@Override
		default <E> E visit(final ItpPatternVisitor<E> visitor) {
			return visitor.visitBinaryPattern(this);
		}
	}

	/**
	 * Interface for a sequence interpolation pattern
	 */
	interface Sequence<T extends ItpMarker>  extends ItpPattern {
		List<T> getSequence();

		@Override
		default <E> E visit(final ItpPatternVisitor<E> visitor) {
			return visitor.visitSequencePattern(this);
		}
	}

	/**
	 * Interface for a tree interpolation pattern
	 */
	interface Tree<T extends ItpMarker>  extends ItpPattern {
		ItpMarkerTree<T> getRoot();

		@Override
		default <E> E visit(final ItpPatternVisitor<E> visitor) {
			return visitor.visitTreePattern(this);
		}
	}

	interface ItpPatternVisitor<E> {
		E visitBinaryPattern(final Binary<?> binaryPattern);
		E visitSequencePattern(final Sequence<?> sequencePattern);
		E visitTreePattern(final Tree<?> treePattern);
	}

}
