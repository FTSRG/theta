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

package hu.bme.mit.theta.xcfa.ir.handlers.utils;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;

import java.util.Map;

import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class PlaceholderAssignmentStmt<T extends Type> implements Stmt {
    private final VarDecl<T> lhs;
    private final Argument rhsKey;

    private PlaceholderAssignmentStmt(VarDecl<T> lhs, Argument rhsKey) {
        this.lhs = lhs;
        this.rhsKey = rhsKey;
    }

    public static <T extends Type> PlaceholderAssignmentStmt<T> of(VarDecl<T> lhs, Argument rhsKey) {
        return new PlaceholderAssignmentStmt<T>(lhs, rhsKey);
    }

    @Override
    public <P, R> R accept(StmtVisitor<? super P, ? extends R> visitor, P param) {
        throw new RuntimeException("PlaceholderStmt should not be treated as a normal statement.");
    }

    public Argument getRhsKey() {
        return rhsKey;
    }

    public VarDecl<T> getLhs() {
        return lhs;
    }

    public AssignStmt<T> toAssignStmt(Map<String, Expr<?>> values) {
        return Assign(lhs, cast(rhsKey.getExpr(values), lhs.getType()));
    }
}
