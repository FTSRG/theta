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

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.arraytype.ArrayExprs;
import hu.bme.mit.theta.core.type.arraytype.ArrayType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaMetadata;

import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class ArrayIntrinsicsHandler extends BaseInstructionHandler {
    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        if (instruction.getOpName().equals("call")) {
            String name = instruction.getArguments().get(instruction.getArguments().size() - 1).getName();
            if (name.startsWith("getArrayElement")) {
                getArrayElement(instruction, globalState, functionState, blockState);
                return;
            } else if (name.startsWith("setArrayElement")) {
                setArrayElement(instruction, globalState, functionState, blockState);
                return;
            }
        }
        super.handleInstruction(instruction, globalState, functionState, blockState);
    }

    private void getArrayElement(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument arr = instruction.getArguments().get(0);
        Argument idx = instruction.getArguments().get(1);

        checkState(arr.getType() instanceof ArrayType<?, ?>, "getArrayElement used on non-array type.");
        checkState(idx.getType() == IntType.getInstance(), "getArrayElement used with non-int index.");
        checkState(instruction.getRetVar().isPresent(), "getArrayElement used without return value.");

        Argument retVar = instruction.getRetVar().get();

        //noinspection unchecked
        functionState.getValues().put(retVar.getName(), ArrayExprs.Read((Expr<ArrayType<IntType, Type>>) arr.getExpr(functionState.getValues()), cast(idx.getExpr(functionState.getValues()), Int())));
    }

    private void setArrayElement(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument arr = instruction.getArguments().get(0);
        Argument idx = instruction.getArguments().get(1);
        Argument val = instruction.getArguments().get(2);

        checkState(arr.getType() instanceof ArrayType<?, ?>, "getArrayElement used on non-array type.");
        checkState(idx.getType() == IntType.getInstance(), "getArrayElement used with non-int index.");

        XcfaLocation loc = new XcfaLocation(blockState.getName() + "_" + blockState.getBlockCnt(), new HashMap<>());
        VarDecl<?> var = functionState.getLocalVars().get(arr.getName()).get1();

        Expr<?> expr = ArrayExprs.Write(cast(var.getRef(), ArrayType.of(Int(), val.getType())), cast(idx.getExpr(functionState.getValues()), Int()), cast(val.getExpr(functionState.getValues()), val.getType()));
        Stmt stmt = Assign(cast(var, var.getType()), cast(expr, var.getType()));
        XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), loc, List.of(stmt));
        if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
        functionState.getProcedureBuilder().addLoc(loc);
        functionState.getProcedureBuilder().addEdge(edge);
        blockState.setLastLocation(loc);
    }


}
