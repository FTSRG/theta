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
package hu.bme.mit.theta.core.utils;

import java.util.Collection;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicBeginStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicEndStmt;
import hu.bme.mit.theta.core.stmt.xcfa.EnterWaitStmt;
import hu.bme.mit.theta.core.stmt.xcfa.ExitWaitStmt;
import hu.bme.mit.theta.core.stmt.xcfa.LoadStmt;
import hu.bme.mit.theta.core.stmt.xcfa.MtxLockStmt;
import hu.bme.mit.theta.core.stmt.xcfa.MtxUnlockStmt;
import hu.bme.mit.theta.core.stmt.xcfa.NotifyAllStmt;
import hu.bme.mit.theta.core.stmt.xcfa.NotifyStmt;
import hu.bme.mit.theta.core.stmt.xcfa.StoreStmt;
import hu.bme.mit.theta.core.stmt.xcfa.WaitStmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaCallStmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaStmtVisitor;
import hu.bme.mit.theta.core.type.Type;

final class VarCollectorStmtVisitor implements StmtVisitor<Collection<VarDecl<?>>, Void>, XcfaStmtVisitor<Collection<VarDecl<?>>, Void> {

	@Override
	public Void visit(XcfaCallStmt stmt, Collection<VarDecl<?>> param) {
		// TODO this only lists the passed parameters, not \
		// the variables where the procedure stores them.
		// This is not a problem for xcfa-analysis, because only globals are returned.
		param.addAll(stmt.getParams());
		return null;
	}

	@Override
	public Void visit(StoreStmt storeStmt, Collection<VarDecl<?>> param) {
		param.add(storeStmt.getLhs());
		param.add(storeStmt.getRhs());
		return null;
	}

	@Override
	public Void visit(LoadStmt loadStmt, Collection<VarDecl<?>> param) {
		param.add(loadStmt.getLhs());
		param.add(loadStmt.getRhs());
		return null;
	}

	@Override
	public Void visit(AtomicBeginStmt atomicBeginStmt, Collection<VarDecl<?>> param) {
		// TODO return list of all variables touched by any stmt that can be accessed from here?
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Void visit(AtomicEndStmt atomicEndStmt, Collection<VarDecl<?>> param) {
		// TODO return list of all variables touched by any stmt that can be accessed from here?
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Void visit(NotifyAllStmt notifyAllStmt, Collection<VarDecl<?>> param) {
		param.add(notifyAllStmt.getSyncVar());
		return null;
	}

	@Override
	public Void visit(NotifyStmt notifyStmt, Collection<VarDecl<?>> param) {
		param.add(notifyStmt.getSyncVar());
		return null;
	}

	@Override
	public Void visit(WaitStmt waitStmt, Collection<VarDecl<?>> param) {
		param.add(waitStmt.getCndSyncVar());
		if (waitStmt.getMtxSyncVar().isPresent())
			param.add(waitStmt.getMtxSyncVar().get());
		return null;
	}

	@Override
	public Void visit(MtxLockStmt lockStmt, Collection<VarDecl<?>> param) {
		param.add(lockStmt.getSyncVar());
		return null;
	}

	@Override
	public Void visit(MtxUnlockStmt unlockStmt, Collection<VarDecl<?>> param) {
		param.add(unlockStmt.getSyncVar());
		return null;
	}

	@Override
	public Void visit(ExitWaitStmt exitWaitStmt, Collection<VarDecl<?>> param) {
		param.add(exitWaitStmt.getSyncVar());
		return null;
	}

	@Override
	public Void visit(EnterWaitStmt enterWaitStmt, Collection<VarDecl<?>> param) {
		param.add(enterWaitStmt.getSyncVar());
		return null;
	}

	private static final class LazyHolder {
		private final static VarCollectorStmtVisitor INSTANCE = new VarCollectorStmtVisitor();
	}

	private VarCollectorStmtVisitor() {
	}

	static VarCollectorStmtVisitor getInstance() {
		return LazyHolder.INSTANCE;
	}

	@Override
	public Void visit(final SkipStmt stmt, final Collection<VarDecl<?>> vars) {
		return null;
	}

	@Override
	public Void visit(final AssumeStmt stmt, final Collection<VarDecl<?>> vars) {
		ExprUtils.collectVars(stmt.getCond(), vars);
		return null;
	}

	@Override
	public <DeclType extends Type> Void visit(final AssignStmt<DeclType> stmt, final Collection<VarDecl<?>> vars) {
		vars.add(stmt.getVarDecl());
		ExprUtils.collectVars(stmt.getExpr(), vars);
		return null;
	}

	@Override
	public <DeclType extends Type> Void visit(final HavocStmt<DeclType> stmt, final Collection<VarDecl<?>> vars) {
		vars.add(stmt.getVarDecl());
		return null;
	}

	@Override
	public Void visit(XcfaStmt xcfaStmt, Collection<VarDecl<?>> param) {
		xcfaStmt.accept(this, param);
		return null;
	}

}
