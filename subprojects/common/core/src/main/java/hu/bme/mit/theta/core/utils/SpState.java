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

package hu.bme.mit.theta.core.utils;

import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.BasicSubstitution;
import hu.bme.mit.theta.core.model.Substitution;
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
import hu.bme.mit.theta.core.type.booltype.BoolType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.decl.Decls.Const;
import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;

public class SpState {
    private static final int HASH_SEED = 2029;
    private volatile int hashCode = 0;

    private final Expr<BoolType> expr;
    private final int constCount;

    private SpState(final Expr<BoolType> expr, final int constCount) {
        checkNotNull(expr);
        checkArgument(constCount >= 0);
        this.expr = expr;
        this.constCount = constCount;
    }

    public static SpState of(final Expr<BoolType> expr) {
        return new SpState(expr, 0);
    }

    public static SpState of(final Expr<BoolType> expr, final int constCount) {
        return new SpState(expr, constCount);
    }

    public Expr<BoolType> getExpr() {
        return expr;
    }

    public int getConstCount() {
        return constCount;
    }

    /**
     * Compute the strongest postcondition w.r.t. a statement
     *
     * @param stmt Statement
     * @return
     */
    public SpState sp(final Stmt stmt) {
        return stmt.accept(SpState.SpVisitor.getInstance(), this);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = HASH_SEED;
            result = 31 * result + expr.hashCode();
            result = 31 * result + constCount;
            hashCode = result;
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof SpState) {
            final SpState that = (SpState) obj;
            return this.constCount == that.constCount && this.expr.equals(that.expr);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName()).add(expr).add(Integer.valueOf(constCount))
                .toString();
    }

    private static final class SpVisitor implements StmtVisitor<SpState, SpState> {

        private SpVisitor() {
        }

        private static class LazyHolder {
            private static final SpState.SpVisitor INSTANCE = new SpState.SpVisitor();
        }

        public static SpState.SpVisitor getInstance() {
            return SpState.SpVisitor.LazyHolder.INSTANCE;
        }

        @Override
        public SpState visit(final SkipStmt stmt, final SpState state) {
            return state;
        }

        @Override
        public <DeclType extends Type> SpState visit(final AssignStmt<DeclType> stmt, final SpState state) {
            final VarDecl<DeclType> varDecl = stmt.getVarDecl();
            final int constCount = state.constCount + 1;
            final String valName = String.format("_sp_%d", constCount);
            final Expr<DeclType> val = Const(valName, varDecl.getType()).getRef();
            final Substitution sub = BasicSubstitution.builder().put(varDecl, val).build();

            final Expr<BoolType> stateExpr = sub.apply(state.getExpr());
            final Expr<DeclType> eqExpr = sub.apply(stmt.getExpr());
            final Expr<BoolType> expr = And(stateExpr, Eq(varDecl.getRef(), eqExpr));
            return new SpState(expr, constCount);
        }

        @Override
        public <DeclType extends Type> SpState visit(final HavocStmt<DeclType> stmt, final SpState state) {
            final VarDecl<DeclType> varDecl = stmt.getVarDecl();
            final int constCount = state.constCount + 1;
            final String valName = String.format("_sp_%d", constCount);
            final Expr<DeclType> val = Const(valName, varDecl.getType()).getRef();
            final Substitution sub = BasicSubstitution.builder().put(varDecl, val).build();
            final Expr<BoolType> expr = sub.apply(state.getExpr());
            return new SpState(expr, constCount);
        }

        @Override
        public SpState visit(final AssumeStmt stmt, final SpState state) {
            final Expr<BoolType> expr = And(state.getExpr(), stmt.getCond());
            final int constCount = state.constCount;
            return new SpState(expr, constCount);
        }

        @Override
        public SpState visit(SequenceStmt stmt, SpState param) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpState visit(NonDetStmt stmt, SpState param) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpState visit(OrtStmt stmt, SpState param) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpState visit(XcfaStmt stmt, SpState param) {
            throw new UnsupportedOperationException();
        }
    }
}
