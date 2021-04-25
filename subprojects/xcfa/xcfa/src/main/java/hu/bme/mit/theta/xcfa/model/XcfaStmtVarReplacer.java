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

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicBeginStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicEndStmt;
import hu.bme.mit.theta.core.stmt.xcfa.FenceStmt;
import hu.bme.mit.theta.core.stmt.xcfa.LoadStmt;
import hu.bme.mit.theta.core.stmt.xcfa.StoreStmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaCallStmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaStmtVisitor;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.stmt.Stmts.Assume;
import static hu.bme.mit.theta.core.stmt.Stmts.Havoc;
import static hu.bme.mit.theta.core.stmt.Stmts.NonDetStmt;
import static hu.bme.mit.theta.core.stmt.Stmts.SequenceStmt;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class XcfaStmtVarReplacer implements XcfaStmtVisitor<Map<VarDecl<?>, VarDecl<?>>, Stmt> {

    public static <T extends Type> Expr<T> replaceVars(Expr<T> expr, Map<VarDecl<?>, VarDecl<?>> varLut) {
        List<? extends Expr<?>> ops = expr.getOps();
        List<Expr<?>> newOps = new ArrayList<>();
        for (Expr<?> op : ops) {
            if (op instanceof LitExpr<?>) newOps.add(op);
            else if (op instanceof RefExpr<?>) {
                if (((RefExpr<?>) op).getDecl() instanceof VarDecl<?>) {
                    newOps.add(varLut.getOrDefault(((RefExpr<?>) op).getDecl(), (VarDecl<?>) ((RefExpr<?>) op).getDecl()).getRef());
                }
            } else newOps.add(replaceVars(op, varLut));
        }
        return expr.withOps(newOps);
    }


    @Override
    public Stmt visit(SkipStmt stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return stmt;
    }

    @Override
    public Stmt visit(AssumeStmt stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return Assume(cast(replaceVars(stmt.getCond(), param), BoolType.getInstance()));
    }

    @Override
    public <DeclType extends Type> Stmt visit(AssignStmt<DeclType> stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return Assign(cast(param.getOrDefault(stmt.getVarDecl(), stmt.getVarDecl()), stmt.getVarDecl().getType()), replaceVars(stmt.getExpr(), param));
    }

    @Override
    public <DeclType extends Type> Stmt visit(HavocStmt<DeclType> stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return Havoc(param.getOrDefault(stmt.getVarDecl(), stmt.getVarDecl()));
    }

    @Override
    public Stmt visit(XcfaStmt xcfaStmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return xcfaStmt.accept(this, param);
    }

    @Override
    public Stmt visit(SequenceStmt stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        List<Stmt> stmts = stmt.getStmts();
        List<Stmt> newStmts = new ArrayList<>();
        for (Stmt stmt1 : stmts) {
            newStmts.add(stmt1.accept(this, param));
        }
        return SequenceStmt(newStmts);
    }

    @Override
    public Stmt visit(NonDetStmt stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        List<Stmt> stmts = stmt.getStmts();
        List<Stmt> newStmts = new ArrayList<>();
        for (Stmt stmt1 : stmts) {
            newStmts.add(stmt1.accept(this, param));
        }
        return NonDetStmt(newStmts);
    }

    @Override
    public Stmt visit(OrtStmt stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        List<Stmt> stmts = stmt.getStmts();
        List<Stmt> newStmts = new ArrayList<>();
        for (Stmt stmt1 : stmts) {
            newStmts.add(stmt1.accept(this, param));
        }
        return OrtStmt.of(newStmts);
    }

    @Override
    public Stmt visit(XcfaCallStmt stmt, Map<VarDecl<?>, VarDecl<?>> param) {
        LinkedHashMap<Expr<?>, XcfaCallStmt.Direction> exprs = stmt.getParams();
        LinkedHashMap<Expr<?>, XcfaCallStmt.Direction> newExprs = new LinkedHashMap<>();
        exprs.forEach((expr, direction) ->
            newExprs.put(replaceVars(expr, param), direction)
        );
        return stmt.of(newExprs, stmt.getProcedure());
    }

    @Override
    public Stmt visit(StoreStmt storeStmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return new StoreStmt(
                param.getOrDefault(storeStmt.getLhs(), storeStmt.getLhs()),
                param.getOrDefault(storeStmt.getRhs(), storeStmt.getRhs()),
                storeStmt.isAtomic(),
                storeStmt.getOrdering()
        );
    }

    @Override
    public Stmt visit(LoadStmt loadStmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return new LoadStmt(
                param.getOrDefault(loadStmt.getLhs(), loadStmt.getLhs()),
                param.getOrDefault(loadStmt.getRhs(), loadStmt.getRhs()),
                loadStmt.isAtomic(),
                loadStmt.getOrdering()
        );
    }

    @Override
    public Stmt visit(FenceStmt fenceStmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return fenceStmt;
    }

    @Override
    public Stmt visit(AtomicBeginStmt atomicBeginStmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return atomicBeginStmt;
    }

    @Override
    public Stmt visit(AtomicEndStmt atomicEndStmt, Map<VarDecl<?>, VarDecl<?>> param) {
        return atomicEndStmt;
    }


}
