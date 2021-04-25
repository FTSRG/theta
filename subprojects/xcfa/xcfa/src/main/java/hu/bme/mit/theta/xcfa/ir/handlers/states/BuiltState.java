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

import hu.bme.mit.theta.xcfa.model.XCFA;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;
import hu.bme.mit.theta.xcfa.model.XcfaProcess;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public class BuiltState {
	private XCFA xcfa;
	private Map<String, XcfaProcess> processes;
	private Map<String, XcfaProcedure> procedures;

	public BuiltState() {
		processes = new HashMap<>();
		procedures = new HashMap<>();
	}

	public XCFA getXcfa() {
		checkState(xcfa != null, "XCFA has not been built yet");
		return xcfa;
	}

	public void setXcfa(XCFA xcfa) {
		this.xcfa = xcfa;
	}

	public Map<String, XcfaProcess> getProcesses() {
		return processes;
	}

	public void setProcesses(Map<String, XcfaProcess> processes) {
		this.processes = processes;
	}

	public Map<String, XcfaProcedure> getProcedures() {
		return procedures;
	}

	public void setProcedures(Map<String, XcfaProcedure> procedures) {
		this.procedures = procedures;
	}
}
