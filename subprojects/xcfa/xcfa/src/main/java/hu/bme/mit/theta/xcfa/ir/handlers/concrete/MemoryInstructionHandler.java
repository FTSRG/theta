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
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.RegArgument;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class MemoryInstructionHandler extends BaseInstructionHandler {
    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getOpName()) {
            case "alloca":
                alloca(instruction, globalState, functionState, blockState);
                break;
            case "load":
                load(instruction, globalState, functionState, blockState);
                break;
            case "store":
                store(instruction, globalState, functionState, blockState);
                break;
            case "getelementptr":
                getelementptr(instruction, globalState, functionState, blockState);
                break;
            case "fence":
            case "cmpxchg":
            case "atomicrmw":
                break;
            default:
                super.handleInstruction(instruction, globalState, functionState, blockState);
                break;
        }

    }

    private void load(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        checkState(functionState.getLocalVars().containsKey(op.getName()), "Load must load a variable!");
        functionState.getValues().put(instruction.getRetVar().get().getName(), functionState.getValues().get(op.getName()));

        Tuple2<VarDecl<?>, Integer> oldVar = functionState.getLocalVars().get(op.getName());

        functionState.getLocalVars().put(instruction.getRetVar().get().getName(), Tuple2.of(oldVar.get1(), oldVar.get2() + 1));
    }

    private void store(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);
        checkState(functionState.getLocalVars().containsKey(op2.getName()), "Store must store into a variable!");

        Tuple2<VarDecl<?>, Integer> oldVar = functionState.getLocalVars().get(op2.getName());
        if (oldVar.get2() > 1) {
            functionState.getLocalVars().put(op2.getName(), Tuple2.of(oldVar.get1(), oldVar.get2() - 1));
        } else if (oldVar.get2() == 1) {
            if (functionState.getParams().contains(oldVar.get1())) {
                VarDecl<?> var = functionState.getLocalVars().get(op2.getName()).get1();
                functionState.getProcedureBuilder().getLocalVars().remove(var);
                var = functionState.getLocalVars().get(op1.getName()).get1();
                functionState.getLocalVars().put(op2.getName(), Tuple2.of(var, 0));
                functionState.getValues().put(op1.getName(), var.getRef());
                functionState.getValues().put(op2.getName(), var.getRef());
            } else {
                XcfaLocation loc = new XcfaLocation(blockState.getName() + "_" + blockState.getBlockCnt(), new HashMap<>());
                VarDecl<?> var = functionState.getLocalVars().get(op2.getName()).get1();
                Stmt stmt = Assign(cast(var, var.getType()), cast(op1.getExpr(functionState.getValues()), var.getType()));
                XcfaEdge edge = new XcfaEdge(blockState.getLastLocation(), loc, List.of(stmt));
                if(instruction.getLineNumber() >= 0) XcfaMetadata.create(edge, "lineNumber", instruction.getLineNumber());
                functionState.getProcedureBuilder().addLoc(loc);
                functionState.getProcedureBuilder().addEdge(edge);
                blockState.setLastLocation(loc);
            }
        }

    }

    private void getelementptr(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void alloca(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Optional<RegArgument> retVar = instruction.getRetVar();
        checkState(retVar.isPresent(), "Alloca must have a variable tied to it");
        VarDecl<?> var = Var(retVar.get().getName(), retVar.get().getType());
        functionState.getProcedureBuilder().getLocalVars().put(var, Optional.empty());
        functionState.getLocalVars().put(retVar.get().getName(), Tuple2.of(var, 0));
        functionState.getValues().put(retVar.get().getName(), var.getRef());
    }
}
