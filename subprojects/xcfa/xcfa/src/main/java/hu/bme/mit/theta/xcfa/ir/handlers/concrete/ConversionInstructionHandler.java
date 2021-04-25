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

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xcfa.ir.handlers.BaseInstructionHandler;
import hu.bme.mit.theta.xcfa.ir.handlers.Instruction;
import hu.bme.mit.theta.xcfa.ir.handlers.arguments.Argument;
import hu.bme.mit.theta.xcfa.ir.handlers.states.BlockState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.FunctionState;
import hu.bme.mit.theta.xcfa.ir.handlers.states.GlobalState;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Ite;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class ConversionInstructionHandler extends BaseInstructionHandler {
    @Override
    public void handleInstruction(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        switch (instruction.getOpName()) {
            case "trunc":
                trunc(instruction, globalState, functionState, blockState);
                break;
            case "zext":
                zext(instruction, globalState, functionState, blockState);
                break;
            case "sext":
                sext(instruction, globalState, functionState, blockState);
                break;
            case "fptrunc":
                fptrunc(instruction, globalState, functionState, blockState);
                break;
            case "fpext":
                fpext(instruction, globalState, functionState, blockState);
                break;
            case "fptoui":
                fptoui(instruction, globalState, functionState, blockState);
                break;
            case "fptosi":
                fptosi(instruction, globalState, functionState, blockState);
                break;
            case "uitofp":
                uitofp(instruction, globalState, functionState, blockState);
                break;
            case "sitofp":
                sitofp(instruction, globalState, functionState, blockState);
                break;
            case "bitcast":
                bitcast(instruction, globalState, functionState, blockState);
                break;
            case "ptrtoint":
            case "inttoptr":
            case "addrspacecast":
                break;
            default:
                super.handleInstruction(instruction, globalState, functionState, blockState);
                break;
        }

    }

    private void trunc(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(op.getType() == IntType.getInstance(), "Only integer values are allowed!");
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), functionState.getValues().get(op.getName()));

        if (functionState.getLocalVars().containsKey(op.getName())) {
            functionState.getLocalVars().put(instruction.getRetVar().get().getName(), functionState.getLocalVars().get(op.getName()));
        }
    }

    private void zext(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(op.getType() == IntType.getInstance() || op.getType() == BoolType.getInstance(), "Only integer/boolean values are allowed!");
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), op.getType() == BoolType.getInstance() ? Ite(cast(functionState.getValues().get(op.getName()), BoolType.getInstance()), IntLitExpr.of(BigInteger.ONE), IntLitExpr.of(BigInteger.ZERO)) : functionState.getValues().get(op.getName()));

        if (functionState.getLocalVars().containsKey(op.getName())) {
            functionState.getLocalVars().put(instruction.getRetVar().get().getName(), functionState.getLocalVars().get(op.getName()));
        }
    }

    private void sext(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(op.getType() == IntType.getInstance() || op.getType() == BoolType.getInstance(), "Only integer/boolean values are allowed!");
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), op.getType() == BoolType.getInstance() ? Ite(cast(functionState.getValues().get(op.getName()), BoolType.getInstance()), IntLitExpr.of(BigInteger.ONE), IntLitExpr.of(BigInteger.ZERO)) : functionState.getValues().get(op.getName()));

        if (functionState.getLocalVars().containsKey(op.getName())) {
            functionState.getLocalVars().put(instruction.getRetVar().get().getName(), functionState.getLocalVars().get(op.getName()));
        }
    }

    private void fptrunc(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(op.getType() == RatType.getInstance(), "Only rational values are allowed!");
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), functionState.getValues().get(op.getName()));

        if (functionState.getLocalVars().containsKey(op.getName())) {
            functionState.getLocalVars().put(instruction.getRetVar().get().getName(), functionState.getLocalVars().get(op.getName()));
        }
    }

    private void fpext(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(op.getType() == RatType.getInstance(), "Only rational values are allowed!");
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), functionState.getValues().get(op.getName()));

        if (functionState.getLocalVars().containsKey(op.getName())) {
            functionState.getLocalVars().put(instruction.getRetVar().get().getName(), functionState.getLocalVars().get(op.getName()));
        }
    }

    private void fptoui(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(op.getType() == RatType.getInstance(), "Only rational values are allowed!");
        checkState(instruction.getRetVar().isPresent(), "Fptoui must load into a variable");

        Expr<RatType> expr = cast(functionState.getValues().get(op.getName()), Rat());
//        functionState.getValues().put(instruction.getRetVar().get().getName(), );

        throw new RuntimeException("Not yet implemented!");
    }

    private void fptosi(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void uitofp(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void sitofp(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        throw new RuntimeException("Not yet implemented!");
    }

    private void bitcast(Instruction instruction, GlobalState globalState, FunctionState functionState, BlockState blockState) {
        Argument op = instruction.getArguments().get(0);
        checkState(instruction.getRetVar().isPresent(), "Load must load into a variable");
        functionState.getValues().put(instruction.getRetVar().get().getName(), functionState.getValues().get(op.getName()));

        if (functionState.getLocalVars().containsKey(op.getName())) {
            functionState.getLocalVars().put(instruction.getRetVar().get().getName(), functionState.getLocalVars().get(op.getName()));
        }
    }
}
