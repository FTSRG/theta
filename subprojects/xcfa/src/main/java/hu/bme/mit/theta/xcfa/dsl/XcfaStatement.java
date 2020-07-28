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
package hu.bme.mit.theta.xcfa.dsl;

import hu.bme.mit.theta.common.dsl.Scope;
import hu.bme.mit.theta.common.dsl.Symbol;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicBeginStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicEndStmt;
import hu.bme.mit.theta.core.stmt.xcfa.LoadStmt;
import hu.bme.mit.theta.core.stmt.xcfa.NotifyAllStmt;
import hu.bme.mit.theta.core.stmt.xcfa.NotifyStmt;
import hu.bme.mit.theta.core.stmt.xcfa.StoreStmt;
import hu.bme.mit.theta.core.stmt.xcfa.MtxLock;
import hu.bme.mit.theta.core.stmt.xcfa.MtxUnlock;
import hu.bme.mit.theta.core.stmt.xcfa.WaitStmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.TypeUtils;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslBaseVisitor;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.AssignStmtContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.AssumeStmtContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.HavocStmtContext;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.StmtContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.stmt.Stmts.*;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;

final class XcfaStatement {

	private final Scope scope;
	private final StmtContext context;
	private Stmt stmt = null;

	XcfaStatement(final Scope scope, final StmtContext context) {
		this.scope = checkNotNull(scope);
		this.context = checkNotNull(context);
	}

	Stmt instantiate() {
		if (stmt != null) return stmt;
		final StmtCreatorVisitor visitor = new StmtCreatorVisitor(scope);
		final Stmt stmt = context.accept(visitor);
		if (stmt == null) {
			throw new AssertionError();
		} else {
			return this.stmt = stmt;
		}
	}

	private static final class StmtCreatorVisitor extends XcfaDslBaseVisitor<Stmt> {

		private final Scope scope;

		StmtCreatorVisitor(final Scope scope) {
			this.scope = checkNotNull(scope);
		}

		@Override
		public Stmt visitHavocStmt(final HavocStmtContext ctx) {
			final String lhsId = ctx.lhs.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> var = (VarDecl<?>) lhsSymbol.instantiate();
			return Havoc(var);
		}

		@Override
		public Stmt visitAssumeStmt(final AssumeStmtContext ctx) {
			final XcfaExpression expression = new XcfaExpression(scope, ctx.cond);
			final Expr<BoolType> expr = TypeUtils.cast(expression.instantiate(), Bool());
			return Assume(expr);
		}

		@Override
		public Stmt visitAssignStmt(final AssignStmtContext ctx) {
			final String lhsId = ctx.lhs.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent(), "Could not find lhs variable %s in assign stmt %s", ctx.lhs.getText(), ctx.getText());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> var = (VarDecl<?>) lhsSymbol.instantiate();

			final XcfaExpression expression = new XcfaExpression(scope, ctx.value);
			final Expr<?> expr = expression.instantiate();

			if (expr.getType().equals(var.getType())) {
				@SuppressWarnings("unchecked") final VarDecl<Type> tVar = (VarDecl<Type>) var;
				@SuppressWarnings("unchecked") final Expr<Type> tExpr = (Expr<Type>) expr;
				return Assign(tVar, tExpr);
			} else {
				throw new IllegalArgumentException("Type of " + var + " is incompatible with " + expr);
			}
		}

		@Override
		public Stmt visitProcCallStmt(final XcfaDslParser.ProcCallStmtContext ctx) {
			final String callee = ctx.funcName.getText();

			final VarDecl<?> var;
			if (ctx.lhs != null) {
				final String lhsId = ctx.lhs.getText();
				Optional<? extends Symbol> opt = scope.resolve(lhsId);
				checkState(opt.isPresent());
				InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
				var = (VarDecl<?>) lhsSymbol.instantiate();
			} else var = null;

			Optional<? extends Symbol> opt = scope.resolve(callee);
			checkState(opt.isPresent());
			final InstantiatableSymbol calleeSymbol = (InstantiatableSymbol) opt.get();
			checkState(calleeSymbol instanceof XcfaProcedureSymbol);
			XCFA.Process.Procedure procedure = (XCFA.Process.Procedure) calleeSymbol.instantiate();

			List<VarDecl<?>> params = new ArrayList<>();

			if (ctx.params != null) {
				ctx.params.forEach(token -> {
					Optional<? extends Symbol> optionalSymbol = scope.resolve(token.getText());
					checkState(optionalSymbol.isPresent());
					final InstantiatableSymbol varSymbol = (InstantiatableSymbol) optionalSymbol.get();
					params.add((VarDecl<?>) varSymbol.instantiate());

				});
			}
			final CallStmt callStmt = new CallStmt(var, procedure, params);
			if (procedure == null) ((XcfaProcedureSymbol) calleeSymbol).addIncompleteInstantiation(callStmt);
			return callStmt;
		}

		@Override
		public Stmt visitStoreStmt(final XcfaDslParser.StoreStmtContext ctx) {
			final String lhsId = ctx.lhs.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> lhs = (VarDecl<?>) lhsSymbol.instantiate();
			opt = scope.resolve(ctx.rhs.getText());
			checkState(opt.isPresent());
			final InstantiatableSymbol rhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> rhs = (VarDecl<?>) rhsSymbol.instantiate();

			final boolean atomic = ctx.atomic != null;
			final String ordering = atomic ? ctx.ordering.getText() : null;

			return new StoreStmt(lhs, rhs, atomic, ordering);

		}

		@Override
		public Stmt visitLoadStmt(final XcfaDslParser.LoadStmtContext ctx) {
			final String lhsId = ctx.lhs.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> lhs = (VarDecl<?>) lhsSymbol.instantiate();
			opt = scope.resolve(ctx.rhs.getText());
			checkState(opt.isPresent());
			final InstantiatableSymbol rhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> rhs = (VarDecl<?>) rhsSymbol.instantiate();

			final boolean atomic = ctx.atomic != null;
			final String ordering = atomic ? ctx.ordering.getText() : null;

			return new LoadStmt(lhs, rhs, atomic, ordering);
		}

		@Override
		public Stmt visitAtomicBegin(final XcfaDslParser.AtomicBeginContext ctx) {
			return new AtomicBeginStmt();
		}

		@Override
		public Stmt visitAtomicEnd(final XcfaDslParser.AtomicEndContext ctx) {
			return new AtomicEndStmt();
		}

		@Override
		public Stmt visitWaitStmt(final XcfaDslParser.WaitStmtContext ctx) {
			final String id1 = ctx.syncVar.getText();
			final String id2 = ctx.mtxVar.getText();
			Optional<? extends Symbol> opt1 = scope.resolve(id1);
			Optional<? extends Symbol> opt2 = scope.resolve(id2);
			checkState(opt1.isPresent());
			checkState(opt2.isPresent());
			final InstantiatableSymbol symbol1 = (InstantiatableSymbol) opt1.get();
			final InstantiatableSymbol symbol2 = (InstantiatableSymbol) opt2.get();
			final VarDecl<?> var1 = (VarDecl<?>) symbol1.instantiate();
			final VarDecl<?> var2 = (VarDecl<?>) symbol1.instantiate();
			return new WaitStmt(var1, var2);
		}
		@Override
		public Stmt visitLegacyWaitStmt(final XcfaDslParser.LegacyWaitStmtContext ctx) {
			final String id1 = ctx.syncVar.getText();
			Optional<? extends Symbol> opt1 = scope.resolve(id1);
			checkState(opt1.isPresent());
			final InstantiatableSymbol symbol1 = (InstantiatableSymbol) opt1.get();
			final VarDecl<?> var1 = (VarDecl<?>) symbol1.instantiate();
			return new WaitStmt(var1);
		}

		@Override
		public Stmt visitNotifyStmt(final XcfaDslParser.NotifyStmtContext ctx) {
			final String lhsId = ctx.syncVar.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> lhs = (VarDecl<?>) lhsSymbol.instantiate();
			return new NotifyStmt(lhs);
		}

		@Override
		public Stmt visitNotifyAllStmt(final XcfaDslParser.NotifyAllStmtContext ctx) {
			final String lhsId = ctx.syncVar.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> lhs = (VarDecl<?>) lhsSymbol.instantiate();
			return new NotifyAllStmt(lhs);
		}

		@Override
		public Stmt visitMtxLock(final XcfaDslParser.MtxLockContext ctx) {
			final String lhsId = ctx.mtxVar.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> lhs = (VarDecl<?>) lhsSymbol.instantiate();
			return new MtxLock(lhs);
		}

		@Override
		public Stmt visitMtxUnlock(final XcfaDslParser.MtxUnlockContext ctx) {
			final String lhsId = ctx.mtxVar.getText();
			Optional<? extends Symbol> opt = scope.resolve(lhsId);
			checkState(opt.isPresent());
			final InstantiatableSymbol lhsSymbol = (InstantiatableSymbol) opt.get();
			final VarDecl<?> lhs = (VarDecl<?>) lhsSymbol.instantiate();
			return new MtxUnlock(lhs);
		}

	}

}
