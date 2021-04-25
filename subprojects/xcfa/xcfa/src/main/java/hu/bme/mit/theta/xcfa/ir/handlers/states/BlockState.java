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

package hu.bme.mit.theta.xcfa.ir.handlers.states;

import hu.bme.mit.theta.xcfa.model.XcfaLocation;

public class BlockState {
	private final FunctionState functionState;
	private final String block;
	private int blockCnt = 0;
	private XcfaLocation lastLocation;

	public BlockState(FunctionState functionState, String block) {
		this.functionState = functionState;
		this.block = block;
		lastLocation = functionState.getLocations().get(block);
	}

	public XcfaLocation getLastLocation() {
		return lastLocation;
	}

	public void setLastLocation(XcfaLocation lastLocation) {
		this.lastLocation = lastLocation;
	}

	public int getBlockCnt() {
		int cnt = blockCnt;
		++blockCnt;
		return cnt;
	}

	public String getName() {
		return block;
	}

	public FunctionState getFunctionState() {
		return functionState;
	}
}
