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
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.abstracttype.AbstractExprs;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.stmt.Stmts.Assume;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Or;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class TerminatorInstructionHandler extends BaseInstructionHandler {

    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getOpName()) {
            case "ret":
                ret(instruction, globalState, functionState, blockState);
                break;
            case "br":
                br(instruction, globalState, functionState, blockState);
                break;
            case "switch":
                sw(instruction, globalState, functionState, blockState);
                break;
            case "unreachable":
                unreachable(instruction, globalState, functionState, blockState);
            case "indirectbr":
            case "invoke":
            case "callbr":
            case "resume":
            case "catchswitch":
            case "catchret":
            case "cleanupret":
                break;
            default:
                super.handleInstruction(instruction, globalState, functionState, blockState);
                break;
        }

    }

    private void unreachable(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        XcfaLocation errLoc = new XcfaLocation(blockState.getName() + "_" + blockState.getBlockCnt(), new HashMap<>());
        functionState.getProcedureBuilder().addLoc(errLoc);
        functionState.getProcedureBuilder().setErrorLoc(errLoc);
        XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), errLoc, List.of());
        if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
        functionState.getProcedureBuilder().addEdge(edge);
        blockState.setLastLocation(errLoc);
    }

    private void ret(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        List<Stmt> stmts = new ArrayList<>();
        VarDecl<?> retVar = functionState.getProcedureBuilder().getResult();
        switch (instruction.getArguments().size()) {
            case 0:
                checkState(retVar == null, "Not returning a value from non-void function!");
                break;
            case 1:
                checkState(retVar != null, "Returning a value from void function!");
                Stmt assignStmt = Assign(cast(retVar, retVar.getType()), cast(instruction.getArguments().get(0).getExpr(functionState.getValues()), retVar.getType()));
                stmts.add(assignStmt);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + instruction.getArguments().size());
        }
        XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), functionState.getProcedureBuilder().getFinalLoc(), stmts);
        if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
        functionState.getProcedureBuilder().addEdge(edge);
        blockState.setLastLocation(functionState.getProcedureBuilder().getFinalLoc());
    }

    private void br(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getArguments().size()) {
            case 1:
                XcfaLocation loc = functionState.getLocations().get(instruction.getArguments().get(0).getName());
                Tuple2<String, String> key = Tuple2.of(blockState.getName(), loc.getName());
                List<Stmt> stmts = functionState.getInterBlockEdges().getOrDefault(key, Tuple4.of(blockState.getLastLocation(), loc, new ArrayList<>(), -1)).get3();
                functionState.getInterBlockEdges().put(key, Tuple4.of(blockState.getLastLocation(), loc, stmts, instruction.getLineNumber()));
                break;
            case 3:
                XcfaLocation loc1 = functionState.getLocations().get(instruction.getArguments().get(1).getName());
                XcfaLocation loc2 = functionState.getLocations().get(instruction.getArguments().get(2).getName());

                Expr<?> lhs = instruction.getArguments().get(0).getExpr(functionState.getValues());
                boolean lhsType = lhs.getType() == BoolType.getInstance();
                checkState(lhsType, "Condition should be bool type!");
                AssumeStmt assume1 = Assume(cast(lhs, BoolType.getInstance()));
                AssumeStmt assume2 = Assume(BoolExprs.Not(cast(lhs, BoolType.getInstance())));
                key = Tuple2.of(blockState.getName(), loc1.getName());
                stmts = functionState.getInterBlockEdges().getOrDefault(key, Tuple4.of(blockState.getLastLocation(), loc1, new ArrayList<>(), -1)).get3();
                stmts.add(assume1);
                functionState.getInterBlockEdges().put(key, Tuple4.of(blockState.getLastLocation(), loc1, stmts, instruction.getLineNumber()));
                key = Tuple2.of(blockState.getName(), loc2.getName());
                stmts = functionState.getInterBlockEdges().getOrDefault(key, Tuple4.of(blockState.getLastLocation(), loc1, new ArrayList<>(), -1)).get3();
                stmts.add(assume2);
                functionState.getInterBlockEdges().put(key, Tuple4.of(blockState.getLastLocation(), loc2, stmts, instruction.getLineNumber()));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + instruction.getArguments().size());
        }
        blockState.setLastLocation(functionState.getProcedureBuilder().getFinalLoc());
    }

    private void sw(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Expr<?> varExpr = instruction.getArguments().get(0).getExpr(functionState.getValues());
        Expr<BoolType> defaultBranch = null;
        for (int i = 0; i < (instruction.getArguments().size() / 2) - 1; ++i) {
            XcfaLocation loc = functionState.getLocations().get(instruction.getArguments().get(2 + 2 * i + 1).getName());
            Expr<?> constExpr = instruction.getArguments().get(2 + 2 * i).getExpr(functionState.getValues());
            checkState(varExpr.getType() == constExpr.getType(), "variable and constant should be of the same type!");
            EqExpr<?> eq = AbstractExprs.Eq(cast(varExpr, constExpr.getType()), cast(constExpr, constExpr.getType()));
            if (defaultBranch == null) defaultBranch = eq;
            else defaultBranch = Or(defaultBranch, eq);
            AssumeStmt assume = Assume(eq);
            Tuple2<String, String> key = Tuple2.of(blockState.getName(), loc.getName());
            List<Stmt> stmts = functionState.getInterBlockEdges().getOrDefault(key, Tuple4.of(blockState.getLastLocation(), loc, new ArrayList<>(), -1)).get3();
            stmts.add(assume);
            functionState.getInterBlockEdges().put(key, Tuple4.of(blockState.getLastLocation(), loc, stmts, instruction.getLineNumber()));
        }
        XcfaLocation loc = functionState.getLocations().get(instruction.getArguments().get(1).getName());
        XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), loc, List.of(Assume(BoolExprs.Not(defaultBranch))));
        if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
        functionState.getProcedureBuilder().addEdge(edge);
        blockState.setLastLocation(functionState.getProcedureBuilder().getFinalLoc());
    }


}
