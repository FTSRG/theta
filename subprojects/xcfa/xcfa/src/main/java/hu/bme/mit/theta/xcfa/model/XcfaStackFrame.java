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

import hu.bme.mit.theta.core.stmt.Stmt;

public class XcfaStackFrame {
	private final XcfaState owner;
	private final XcfaEdge edge;
	private Stmt stmt;
	private boolean lastStmt;
	private boolean newProcedure;

	XcfaStackFrame(XcfaState owner, XcfaEdge edge, Stmt stmt) {
		this.owner = owner;
		this.edge = edge;
		this.stmt = stmt;
		this.lastStmt = false;
		this.newProcedure = false;
	}

	public XcfaEdge getEdge() {
		return edge;
	}

	public Stmt getStmt() {
		return stmt;
	}

	void setStmt(Stmt stmt) {
		this.stmt = stmt;
	}

	public boolean isLastStmt() {
		return lastStmt;
	}

	void setLastStmt() {
		this.lastStmt = true;
	}

	XcfaStackFrame duplicate(XcfaState newOwner) {
		return new XcfaStackFrame(newOwner, edge, stmt);
	}

	public XcfaProcess getProcess() {
		return edge.getParent().getParent();
	}

	public XcfaState getOwner() {
		return owner;
	}

	public void accept() {
		owner.acceptOffer(this);
	}

	public boolean isNewProcedure() {
		return newProcedure;
	}

	public void setNewProcedure() {
		this.newProcedure = true;
	}
}
