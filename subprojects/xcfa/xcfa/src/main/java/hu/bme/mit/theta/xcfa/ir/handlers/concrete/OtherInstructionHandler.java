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

package hu.bme.mit.theta.xcfa.ir.handlers.concrete;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.common.Tuple4;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaCallStmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.type.rattype.RatExprs;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.RegArgument;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.StringArgument;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;
import hu.bme.mit.theta.xcfa.ir.handlers.utils.PlaceholderAssignmentStmt;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.stmt.Stmts.Havoc;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Ite;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Eq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Geq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Gt;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Leq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Lt;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Neq;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class OtherInstructionHandler extends BaseInstructionHandler {
    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getOpName()) {
            case "icmp":
                icmp(instruction, globalState, functionState, blockState);
                break;
            case "fcmp":
                fcmp(instruction, globalState, functionState, blockState);
                break;
            case "phi":
                phi(instruction, globalState, functionState, blockState);
                break;
            case "select":
                select(instruction, globalState, functionState, blockState);
                break;
            case "call":
                call(instruction, globalState, functionState, blockState);
                break;
            case "freeze":
            case "va_arg":
            case "landingpad":
            case "catchpad":
            case "cleanuppad":
                break;
            default:
                super.handleInstruction(instruction, globalState, functionState, blockState);
                break;
        }

    }

    private void call(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument functionName = instruction.getArguments().get(instruction.getArguments().size() - 1);
        XcfaLocation newLoc = new XcfaLocation(blockState.getName() + "_" + blockState.getBlockCnt(), new HashMap<>());
        if (functionName.getName().equals("__assert_fail")) {
            XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), newLoc, List.of());
            if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
            functionState.getProcedureBuilder().addLoc(newLoc);
            functionState.getProcedureBuilder().addEdge(edge);
            blockState.setLastLocation(newLoc);
            functionState.getProcedureBuilder().setErrorLoc(newLoc);
        } else if (globalState.getProcedures().stream().anyMatch(objects -> objects.get1().equals(functionName.getName()))) {

            VarDecl<?> callVar = null;
            if (instruction.getRetVar().isPresent()) {
                callVar = Var(instruction.getRetVar().get().getName(), instruction.getRetVar().get().getType());
                functionState.getProcedureBuilder().getLocalVars().put(callVar, Optional.empty());
                functionState.getLocalVars().put(instruction.getRetVar().get().getName(), Tuple2.of(callVar, 1));
                functionState.getValues().put(instruction.getRetVar().get().getName(), callVar.getRef());
            }
            LinkedHashMap<Expr<?>, XcfaCallStmt.Direction> exprs = new LinkedHashMap<>();
            if(callVar != null) exprs.put(callVar.getRef(), XcfaCallStmt.Direction.OUT);
            for (int i = 0; i < instruction.getArguments().size() - 1; ++i) {
                Expr<? extends Type> expr = instruction.getArguments().get(i).getExpr(functionState.getValues());
                Tuple2<VarDecl<?>, Integer> objects = functionState.getLocalVars().get(instruction.getArguments().get(i).getName());
                if (expr != null) exprs.put(expr, (objects!=null && objects.get2() > 0) ? XcfaCallStmt.Direction.INOUT : XcfaCallStmt.Direction.IN);
            }
            XcfaCallStmt stmt = new XcfaCallStmt(exprs, functionName.getName());
            XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), newLoc, List.of(stmt));
            if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
            functionState.getProcedureBuilder().addLoc(newLoc);
            functionState.getProcedureBuilder().addEdge(edge);
            blockState.setLastLocation(newLoc);
        } else {
            List<Stmt> stmts = new ArrayList<>();
            if (instruction.getRetVar().isPresent()) {
                stmts.add(havocVar(instruction.getRetVar().get(), functionState, blockState));
            }
            for (Argument argument : instruction.getArguments()) {
                Tuple2<VarDecl<?>, Integer> objects = functionState.getLocalVars().get(argument.getName());
                if (objects != null && objects.get2() > 0)
                    stmts.add(havocVar(argument, functionState, blockState));
            }
            XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), newLoc, stmts);
            if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
            functionState.getProcedureBuilder().addLoc(newLoc);
            functionState.getProcedureBuilder().addEdge(edge);
            blockState.setLastLocation(newLoc);
        }
    }

    private Stmt havocVar(Argument reg, FunctionState functionState, BlockState blockState) {
        VarDecl<?> callVar = Var(reg.getName(), reg.getType());
        functionState.getProcedureBuilder().getLocalVars().put(callVar, Optional.empty());
        functionState.getLocalVars().put(reg.getName(), Tuple2.of(callVar, 1));
        functionState.getValues().put(reg.getName(), callVar.getRef());
        return Havoc(callVar);
    }

    private void select(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument cond = instruction.getArguments().get(0);
        Argument op1 = instruction.getArguments().get(1);
        Argument op2 = instruction.getArguments().get(2);

        checkState(cond.getType() == BoolType.getInstance(), "Select only supports boolean condition!");
        checkState(op1.getType() == op2.getType(), "Select only supports common types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        Expr<?> expr1 = op1.getExpr(functionState.getValues());
        Expr<?> expr2 = op2.getExpr(functionState.getValues());
        functionState.getValues().put(instruction.getRetVar().get().getName(), Ite(
                cast(cond.getExpr(functionState.getValues()), BoolType.getInstance()),
                cast(expr1, expr1.getType()),
                cast(expr2, expr1.getType())));
    }

    // Phi nodes are the only possible place where an argument might not be known yet.
    private void phi(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Optional<RegArgument> retVar = instruction.getRetVar();
        checkState(retVar.isPresent(), "Return var must be present!");
        VarDecl<?> phiVar = Var(retVar.get().getName(), retVar.get().getType());
        functionState.getProcedureBuilder().getLocalVars().put(phiVar, Optional.empty());
        functionState.getLocalVars().put(retVar.get().getName(), Tuple2.of(phiVar, 1));
        functionState.getValues().put(retVar.get().getName(), phiVar.getRef());
        for (int i = 0; i < (instruction.getArguments().size()) / 2; ++i) {
            Argument block = instruction.getArguments().get(2 * i + 1);
            Argument value = instruction.getArguments().get(2 * i);
            Tuple2<String, String> key = Tuple2.of(block.getName(), blockState.getName());
            Tuple4<XcfaLocation, XcfaLocation, List<Stmt>, Integer> val = functionState.getInterBlockEdges().getOrDefault(key, Tuple4.of(new XcfaLocation(key.get1(), null), new XcfaLocation(key.get2(), null), new ArrayList<>(), -1));
            checkState(phiVar.getType() == value.getType(), "phiVar and value has to be of the same type!");
            Stmt stmt;
            Expr<?> expr;
            if ((expr = value.getExpr(functionState.getValues())) != null) {
                stmt = Assign(cast(phiVar, phiVar.getType()), cast(expr, phiVar.getType()));
            } else {
                stmt = PlaceholderAssignmentStmt.of(phiVar, value);
            }
            val.get3().add(stmt);
            functionState.getInterBlockEdges().put(key, val);
        }
    }

    private void fcmp(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);
        Argument op3 = instruction.getArguments().get(2);

        checkState(op1 instanceof StringArgument, "Icmp has to have string argument as first operand!");
        checkState(op2.getType() == RatType.getInstance(), "Icmp only supports integer types!");
        checkState(op3.getType() == RatType.getInstance(), "Icmp only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        switch (op1.getName()) {
            case "eq":
                functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Eq(cast(op2.getExpr(functionState.getValues()), RatType.getInstance()), cast(op3.getExpr(functionState.getValues()), RatType.getInstance())));
                break;
            case "ne":
                functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Neq(cast(op2.getExpr(functionState.getValues()), RatType.getInstance()), cast(op3.getExpr(functionState.getValues()), RatType.getInstance())));
                break;
            case "ugt":
            case "sgt":
                functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Gt(cast(op2.getExpr(functionState.getValues()), RatType.getInstance()), cast(op3.getExpr(functionState.getValues()), RatType.getInstance())));
                break;
            case "uge":
            case "sge":
                functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Geq(cast(op2.getExpr(functionState.getValues()), RatType.getInstance()), cast(op3.getExpr(functionState.getValues()), RatType.getInstance())));
                break;
            case "ult":
            case "slt":
                functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Lt(cast(op2.getExpr(functionState.getValues()), RatType.getInstance()), cast(op3.getExpr(functionState.getValues()), RatType.getInstance())));
                break;
            case "ule":
            case "sle":
                functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Leq(cast(op2.getExpr(functionState.getValues()), RatType.getInstance()), cast(op3.getExpr(functionState.getValues()), RatType.getInstance())));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + op1.getName());
        }
    }

    private void icmp(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);
        Argument op3 = instruction.getArguments().get(2);

        checkState(op1 instanceof StringArgument, "Icmp has to have string argument as first operand!");
        checkState(op2.getType() == IntType.getInstance(), "Icmp only supports integer types!");
        checkState(op3.getType() == IntType.getInstance(), "Icmp only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        switch (op1.getName()) {
            case "eq":
                functionState.getValues().put(instruction.getRetVar().get().getName(), Eq(cast(op2.getExpr(functionState.getValues()), IntType.getInstance()), cast(op3.getExpr(functionState.getValues()), IntType.getInstance())));
                break;
            case "ne":
                functionState.getValues().put(instruction.getRetVar().get().getName(), Neq(cast(op2.getExpr(functionState.getValues()), IntType.getInstance()), cast(op3.getExpr(functionState.getValues()), IntType.getInstance())));
                break;
            case "ugt":
            case "sgt":
                functionState.getValues().put(instruction.getRetVar().get().getName(), Gt(cast(op2.getExpr(functionState.getValues()), IntType.getInstance()), cast(op3.getExpr(functionState.getValues()), IntType.getInstance())));
                break;
            case "uge":
            case "sge":
                functionState.getValues().put(instruction.getRetVar().get().getName(), Geq(cast(op2.getExpr(functionState.getValues()), IntType.getInstance()), cast(op3.getExpr(functionState.getValues()), IntType.getInstance())));
                break;
            case "ult":
            case "slt":
                functionState.getValues().put(instruction.getRetVar().get().getName(), Lt(cast(op2.getExpr(functionState.getValues()), IntType.getInstance()), cast(op3.getExpr(functionState.getValues()), IntType.getInstance())));
                break;
            case "ule":
            case "sle":
                functionState.getValues().put(instruction.getRetVar().get().getName(), Leq(cast(op2.getExpr(functionState.getValues()), IntType.getInstance()), cast(op3.getExpr(functionState.getValues()), IntType.getInstance())));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + op1.getName());
        }
    }
}
