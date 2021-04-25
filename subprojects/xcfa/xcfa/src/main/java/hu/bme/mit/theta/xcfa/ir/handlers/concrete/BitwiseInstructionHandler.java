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

import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Or;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Xor;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class BitwiseInstructionHandler extends BaseInstructionHandler {
    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getOpName()) {
            case "shl":
                shl(instruction, globalState, functionState, blockState);
                break;
            case "lshr":
                lshr(instruction, globalState, functionState, blockState);
                break;
            case "ashr":
                ashr(instruction, globalState, functionState, blockState);
                break;
            case "and":
                and(instruction, globalState, functionState, blockState);
                break;
            case "or":
                or(instruction, globalState, functionState, blockState);
                break;
            case "xor":
                xor(instruction, globalState, functionState, blockState);
                break;
            default:
                super.handleInstruction(instruction, globalState, functionState, blockState);
                break;
        }

    }

    private void shl(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void lshr(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void ashr(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void and(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == BoolType.getInstance(), "And only supports boolean types!");
        checkState(op2.getType() == BoolType.getInstance(), "And only supports boolean types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), And(cast(op1.getExpr(functionState.getValues()), BoolType.getInstance()), cast(op2.getExpr(functionState.getValues()), BoolType.getInstance())));
    }

    private void or(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == BoolType.getInstance(), "Or only supports boolean types!");
        checkState(op2.getType() == BoolType.getInstance(), "Or only supports boolean types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Or(cast(op1.getExpr(functionState.getValues()), BoolType.getInstance()), cast(op2.getExpr(functionState.getValues()), BoolType.getInstance())));

    }

    private void xor(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == BoolType.getInstance(), "Xor only supports boolean types!");
        checkState(op2.getType() == BoolType.getInstance(), "Xor only supports boolean types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Xor(cast(op1.getExpr(functionState.getValues()), BoolType.getInstance()), cast(op2.getExpr(functionState.getValues()), BoolType.getInstance())));

    }

}
