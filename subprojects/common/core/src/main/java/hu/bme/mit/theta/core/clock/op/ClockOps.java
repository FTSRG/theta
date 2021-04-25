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
package hu.bme.mit.theta.core.clock.op;

import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstrs;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.abstracttype.AddExpr;
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.core.type.rattype.RatLitExpr;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.core.utils.TypeUtils;

import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;

public final class ClockOps {

	private static final StmtToClockOpVisitor VISITOR;

	static {
		VISITOR = new StmtToClockOpVisitor();
	}

	private ClockOps() {
	}

	////

	public static ClockOp fromStmt(final Stmt stmt) {
		return stmt.accept(VISITOR, null);
	}

	////

	public static CopyOp Copy(final VarDecl<RatType> varDecl, final VarDecl<RatType> value) {
		return new CopyOp(varDecl, value);
	}

	public static FreeOp Free(final VarDecl<RatType> varDecl) {
		return new FreeOp(varDecl);
	}

	public static GuardOp Guard(final ClockConstr constr) {
		return new GuardOp(constr);
	}

	public static ResetOp Reset(final VarDecl<RatType> varDecl, final int value) {
		return new ResetOp(varDecl, value);
	}

	public static ShiftOp Shift(final VarDecl<RatType> varDecl, final int offset) {
		return new ShiftOp(varDecl, offset);
	}

	////

	private static final class StmtToClockOpVisitor implements StmtVisitor<Void, ClockOp> {

		private StmtToClockOpVisitor() {
		}

		@Override
		public ClockOp visit(final SkipStmt stmt, final Void param) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <DeclType extends Type> ClockOp visit(final HavocStmt<DeclType> stmt, final Void param) {
			final VarDecl<RatType> varDecl = TypeUtils.cast(stmt.getVarDecl(), Rat());
			return Free(varDecl);
		}

		@Override
		public ClockOp visit(XcfaStmt xcfaStmt, Void param) {
			throw new UnsupportedOperationException("Not yet implemented");
		}

		@Override
		public ClockOp visit(SequenceStmt stmt, Void param) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ClockOp visit(NonDetStmt stmt, Void param) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ClockOp visit(OrtStmt stmt, Void param) { throw new UnsupportedOperationException(); }

		@Override
		public <DeclType extends Type> ClockOp visit(final AssignStmt<DeclType> stmt, final Void param) {

			final VarDecl<RatType> varDecl = TypeUtils.cast(stmt.getVarDecl(), Rat());
			final Expr<?> expr = stmt.getExpr();

			if (expr instanceof IntLitExpr) {
				final IntLitExpr intLit = (IntLitExpr) expr;
				final int value = Math.toIntExact(intLit.getValue().longValue());
				return Reset(varDecl, value);

			} else if (expr instanceof RefExpr) {
				final RefExpr<?> rightRef = (RefExpr<?>) expr;
				final Decl<?> rightDecl = rightRef.getDecl();
				if (rightDecl instanceof VarDecl) {
					final VarDecl<?> rightVar = (VarDecl<?>) rightDecl;
					final VarDecl<RatType> rightRatVar = TypeUtils.cast(rightVar, Rat());
					return Copy(varDecl, rightRatVar);
				}

			} else if (expr instanceof AddExpr) {
				final RefExpr<RatType> varRef = varDecl.getRef();
				final AddExpr<?> addExpr = (AddExpr<?>) expr;
				final Expr<?>[] ops = addExpr.getOps().toArray(new Expr<?>[0]);

				if (ops.length == 2) {
					if (ops[0].equals(varRef)) {
						if (ops[1] instanceof RatLitExpr) {
							final RatLitExpr ratLit = (RatLitExpr) ops[1];
							final int num = ratLit.getNum().intValue();
							final int denom = ratLit.getDenom().intValue();
							if (denom == 1) {
								return Shift(varDecl, num);
							}
						}
					} else if (ops[1].equals(varRef)) {
						if (ops[0] instanceof IntLitExpr) {
							final IntLitExpr intLit = (IntLitExpr) ops[0];
							final int offset = Math.toIntExact(intLit.getValue().longValue());
							return Shift(varDecl, offset);
						}
					}
				}
			}

			throw new IllegalArgumentException();
		}

		@Override
		public ClockOp visit(final AssumeStmt stmt, final Void param) {
			try {
				final Expr<BoolType> cond = stmt.getCond();
				final ClockConstr constr = ClockConstrs.formExpr(cond);
				return Guard(constr);

			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException();
			}
		}

	}

}
