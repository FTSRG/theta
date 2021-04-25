/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.bme.mit.theta.core.type.xcfa;

import hu.bme.mit.theta.core.type.Type;

public final class SyntheticType implements Type {

	private static final SyntheticType INSTANCE = new SyntheticType();

	private SyntheticType() {
	}

	public static SyntheticType getInstance() {
		return INSTANCE;
	}
}
