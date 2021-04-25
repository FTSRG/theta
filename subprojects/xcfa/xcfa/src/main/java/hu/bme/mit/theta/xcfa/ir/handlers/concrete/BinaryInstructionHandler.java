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

import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.type.rattype.RatExprs;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Add;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Div;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Mul;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Rem;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Sub;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class BinaryInstructionHandler extends BaseInstructionHandler {
    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getOpName()) {
            case "add":
                add(instruction, globalState, functionState, blockState);
                break;
            case "fadd":
                fadd(instruction, globalState, functionState, blockState);
                break;
            case "sub":
                sub(instruction, globalState, functionState, blockState);
                break;
            case "fsub":
                fsub(instruction, globalState, functionState, blockState);
                break;
            case "mul":
                mul(instruction, globalState, functionState, blockState);
                break;
            case "fmul":
                fmul(instruction, globalState, functionState, blockState);
                break;
            case "udiv":
            case "sdiv":
                div(instruction, globalState, functionState, blockState);
                break;
            case "fdiv":
                fdiv(instruction, globalState, functionState, blockState);
                break;
            case "urem":
            case "srem":
                rem(instruction, globalState, functionState, blockState);
                break;
            case "frem":
                frem(instruction, globalState, functionState, blockState);
                break;
            default:
                super.handleInstruction(instruction, globalState, functionState, blockState);
                break;
        }

    }

    private void frem(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == RatType.getInstance(), "Frem only supports rational types!");
        checkState(op2.getType() == RatType.getInstance(), "Frem only supports rational types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        // TODO: semantics of FREM?
        throw new RuntimeException("Frem semantics is not yet implemented!");
        //functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Rem(cast(op1.getExpr(functionState.getValues()), RatType.getInstance()), cast(op2.getExpr(functionState.getValues()), RatType.getInstance())));
    }

    private void rem(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == IntType.getInstance(), "Rem only supports integer types!");
        checkState(op2.getType() == IntType.getInstance(), "Rem only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Rem(cast(op1.getExpr(functionState.getValues()), IntType.getInstance()), cast(op2.getExpr(functionState.getValues()), IntType.getInstance())));
    }

    private void fdiv(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == RatType.getInstance(), "Fdiv only supports rational types!");
        checkState(op2.getType() == RatType.getInstance(), "Fdiv only supports rational types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Div(cast(op1.getExpr(functionState.getValues()), RatType.getInstance()), cast(op2.getExpr(functionState.getValues()), RatType.getInstance())));
    }

    private void div(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == IntType.getInstance(), "Div only supports integer types!");
        checkState(op2.getType() == IntType.getInstance(), "Div only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Div(cast(op1.getExpr(functionState.getValues()), IntType.getInstance()), cast(op2.getExpr(functionState.getValues()), IntType.getInstance())));
    }

    private void fmul(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == RatType.getInstance(), "Fmul only supports rational types!");
        checkState(op2.getType() == RatType.getInstance(), "Fmul only supports rational types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Mul(cast(op1.getExpr(functionState.getValues()), RatType.getInstance()), cast(op2.getExpr(functionState.getValues()), RatType.getInstance())));
    }

    private void mul(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == IntType.getInstance(), "Mul only supports integer types!");
        checkState(op2.getType() == IntType.getInstance(), "Mul only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Mul(cast(op1.getExpr(functionState.getValues()), IntType.getInstance()), cast(op2.getExpr(functionState.getValues()), IntType.getInstance())));
    }

    private void fsub(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == RatType.getInstance(), "Fsub only supports rational types!");
        checkState(op2.getType() == RatType.getInstance(), "Fsub only supports rational types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Sub(cast(op1.getExpr(functionState.getValues()), RatType.getInstance()), cast(op2.getExpr(functionState.getValues()), RatType.getInstance())));
    }

    private void sub(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == IntType.getInstance(), "Sub only supports integer types!");
        checkState(op2.getType() == IntType.getInstance(), "Sub only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Sub(cast(op1.getExpr(functionState.getValues()), IntType.getInstance()), cast(op2.getExpr(functionState.getValues()), IntType.getInstance())));
    }

    private void fadd(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == RatType.getInstance(), "Fadd only supports rational types!");
        checkState(op2.getType() == RatType.getInstance(), "Fadd only supports rational types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), RatExprs.Add(cast(op1.getExpr(functionState.getValues()), RatType.getInstance()), cast(op2.getExpr(functionState.getValues()), RatType.getInstance())));
    }

    private void add(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op1 = instruction.getArguments().get(0);
        Argument op2 = instruction.getArguments().get(1);

        checkState(op1.getType() == IntType.getInstance(), "Rem only supports integer types!");
        checkState(op2.getType() == IntType.getInstance(), "Rem only supports integer types!");
        checkState(instruction.getRetVar().isPresent(), "Instruction must have return variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), Add(cast(op1.getExpr(functionState.getValues()), IntType.getInstance()), cast(op2.getExpr(functionState.getValues()), IntType.getInstance())));
    }

}
