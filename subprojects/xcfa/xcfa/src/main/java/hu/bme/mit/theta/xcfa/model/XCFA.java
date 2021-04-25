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
package hu.bme.mit.theta.xcfa.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents an immutable Extended Control Flow Automata (XCFA). Use the builder class to
 * create a new instance.
 */
@SuppressWarnings("unused")
public final class XCFA {
	private final ImmutableMap<VarDecl<? extends Type>, Optional<LitExpr<?>>> globalVars;
	private final ImmutableList<XcfaProcess> processes;
	private final XcfaProcess mainProcess;

	private XCFA(Builder builder) {
		globalVars = ImmutableMap.copyOf(builder.globalVars);
		processes = ImmutableList.copyOf(builder.processes);
		processes.forEach(process -> process.setParent(this));
		mainProcess = builder.mainProcess;
	}

	public static Builder builder() {
		return new Builder();
	}

	/*
	 * Returns the equivalent CFA, if it exists.
	 */
	public CFA createCFA() {
		checkState(getProcesses().size() == 1, "XCFA cannot be converted to CFA because it has more than one process.");
		checkState(getMainProcess().getProcedures().size() == 1, "XCFA cannot be converted to CFA because it has more than one procedure.");

		CFA.Builder builder = CFA.builder();
		CFA.Loc initLoc = null;
		CFA.Loc errorLoc = null;
		CFA.Loc finalLoc = null;

		int tmpcnt = 0;

		Map<XcfaLocation, CFA.Loc> locationLUT = new HashMap<>();

		for (XcfaLocation loc : getMainProcess().getMainProcedure().getLocs()) {
			CFA.Loc cfaLoc = builder.createLoc(loc.getName());
			locationLUT.put(loc, cfaLoc);
		}

		for (XcfaEdge e : getMainProcess().getMainProcedure().getEdges()) {

			List<CFA.Loc> locations = new ArrayList<>();
			// Adding source
			locations.add(locationLUT.get(e.getSource()));
			// Adding intermediate locations (CFAs can only have one per edge)
			for (int i = 1; i < e.getStmts().size(); ++i) {
				locations.add(builder.createLoc("tmp" + tmpcnt++));
			}
			// Adding target
			locations.add(locationLUT.get(e.getTarget()));
			// Adding edges
			for (int i = 0; i < e.getStmts().size(); ++i) {
				checkState(!(e.getStmts().get(i) instanceof XcfaStmt), "XCFA statement " + e.getStmts().get(i) + " is not supported!");
				CFA.Edge edge = builder.createEdge(locations.get(i), locations.get(i + 1), e.getStmts().get(i));
				XcfaMetadata.create(e, "cfaEdge", edge);
			}
			if (e.getStmts().size() == 0) {
				CFA.Edge edge = builder.createEdge(locations.get(0), locations.get(1), SkipStmt.getInstance());
				XcfaMetadata.create(e, "cfaEdge", edge);
			}
			// Deciding if the source or target is any special location
			if (e.getSource() == getMainProcess().getMainProcedure().getInitLoc()) initLoc = locations.get(0);
			if (e.getTarget() == getMainProcess().getMainProcedure().getErrorLoc())
				errorLoc = locations.get(locations.size() - 1);
			else if (e.getTarget() == getMainProcess().getMainProcedure().getFinalLoc())
				finalLoc = locations.get(locations.size() - 1);
		}
		// Setting special locations (initial and final locations are mandatory, error location is not)
		builder.setInitLoc(initLoc);
		if (errorLoc != null) builder.setErrorLoc(errorLoc);
		builder.setFinalLoc(finalLoc);

		return builder.build();
	}

	/*
	 * Getters
	 */

	/*
	 * Returns the XCFA as its graphviz representation
	 */
	public String toDot() {
		StringBuilder ret = new StringBuilder("digraph G{\n");
		for (VarDecl<? extends Type> globalVar : getGlobalVars()) {
			ret.append("\"var ").append(globalVar).append(" = ").append(getInitValue(globalVar).get()).append("\";\n");
		}
		ret.append(getMainProcess().toDot());
		ret.append("}\n");
		return ret.toString();
	}

	public XcfaState getInitialState() {
		return new XcfaState(this);
	}

	public List<VarDecl<? extends Type>> getGlobalVars() {
		return List.copyOf(globalVars.keySet());
	}

	public Optional<LitExpr<?>> getInitValue(final VarDecl<?> varDecl) {
		return globalVars.get(varDecl);
	}

	public List<XcfaProcess> getProcesses() {
		return processes;
	}

	public XcfaProcess getMainProcess() {
		return mainProcess;
	}

	public static final class Builder {
		private final Map<VarDecl<?>, Optional<LitExpr<?>>> globalVars;
		private final List<XcfaProcess> processes;
		private XcfaProcess mainProcess;

		private boolean built;

		private Builder() {
			globalVars = new LinkedHashMap<>();
			processes = new ArrayList<>();
		}

		private void checkNotBuilt() {
			checkState(!built, "An XCFA was already built.");
		}

		// globalVars
		public Map<VarDecl<?>, Optional<LitExpr<?>>> getGlobalVars() {
			return globalVars;
		}

		public void addGlobalVar(final VarDecl<?> var, final LitExpr<?> initValue) {
			checkNotBuilt();
			globalVars.put(var, Optional.ofNullable(initValue));
		}

		// processes
		public List<XcfaProcess> getProcesses() {
			return processes;
		}

		public void addProcess(final XcfaProcess process) {
			checkNotBuilt();
			processes.add(process);
		}

		// mainProcess
		public XcfaProcess getMainProcess() {
			return mainProcess;
		}

		public void setMainProcess(final XcfaProcess mainProcess) {
			checkNotBuilt();
			checkArgument(processes.contains(mainProcess), "Invalid main process.");
			this.mainProcess = mainProcess;
		}

		public XCFA build() {
			checkNotBuilt();
			checkState(mainProcess != null, "Main process must be set.");
			XCFA xcfa = new XCFA(this);
			built = true;
			return xcfa;
		}
	}

}
