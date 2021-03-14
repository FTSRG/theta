package hu.bme.mit.theta.xcfa.ir;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.common.Tuple4;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.Stmts;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntEqExpr;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.type.rattype.RatLitExpr;
import hu.bme.mit.theta.xcfa.XCFA;

import java.math.BigInteger;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.stmt.Stmts.Assume;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.*;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;

public class Utils {

    private static final int FLOAT_DIGITS = 5;

    public static Type createType(String type) {
        switch (type) {
            case "i32": return Int();
            case "f64":   return Rat();
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    public static VarDecl<? extends Type> createVariable(String name, String type) {
        Type t = createType(type);
        return Var(name, t);
    }

    public static LitExpr<? extends Type> createConstant(String value) {
        String[] arguments = value.split(" ");
        checkState(arguments.length == 2, "Contant should be of form \"(type=[a-zA-Z0-9]*) (value=[\\.0-9fe+-]*)\"");
        switch (arguments[0]) {
            case "i32": return IntLitExpr.of(new BigInteger(arguments[1]));
            case "f64":
                float v = Float.parseFloat(arguments[1]);
                return RatLitExpr.of(BigInteger.valueOf((long)(v*(1 << FLOAT_DIGITS))), BigInteger.valueOf(1 << FLOAT_DIGITS));
            default:
                throw new IllegalStateException("Unexpected value: " + arguments[0]);
        }
    }

    public static void handleProcedure(
            Tuple3<String, Optional<String>, List<Tuple2<String, String>>> function,
            XCFA.Process.Procedure.Builder procedureBuilder,
            SSAProvider ssa,
            Map<String, VarDecl<?>> globalVarLut,
            Collection<String> processes) {

        Map<String, VarDecl<?>> localVarLut = new HashMap<>();

        // Adding params
        for (Tuple2<String, String> param : function.get3()) {
            VarDecl<?> var = createVariable(param.get2(), param.get1());
            procedureBuilder.createParam(var);
            localVarLut.put(param.get2(), var);
        }

        // Adding final location
        XCFA.Process.Procedure.Location finalLoc = new XCFA.Process.Procedure.Location(function.get1() + "_final", new HashMap<>());
        procedureBuilder.addLoc(finalLoc);
        procedureBuilder.setFinalLoc(finalLoc);

        // Adding return variable
        Optional<VarDecl<? extends Type>> retVar = Optional.empty();
        if(function.get2().isPresent()) {
            retVar = Optional.of(createVariable(function.get1() + "_ret", function.get2().get()));
            procedureBuilder.setRtype(createType(function.get2().get()));
            procedureBuilder.setResult(retVar.get());
        }

        // Adding blocks and first location
        List<String> blocks = ssa.getBlocks(function.get1());
        Map<String, XCFA.Process.Procedure.Location> locationLut = new HashMap<>();
        boolean first = true;
        for (String block : blocks) {
            XCFA.Process.Procedure.Location loc = new XCFA.Process.Procedure.Location(block, new HashMap<>());
            locationLut.put(block, loc);
            procedureBuilder.addLoc(loc);
            if(first) {
                procedureBuilder.setInitLoc(loc);
                first = false;
            }
        }

        InstructionHandler instructionHandler = new NaiveInstructionHandler(function, procedureBuilder, ssa, processes, localVarLut, finalLoc, retVar, locationLut);

        // Handling instructions
        for (String block : locationLut.keySet()) {
            instructionHandler.reinitClass(block);
            for (Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction : ssa.getInstructions(block)) {
                instructionHandler.handleInstruction(instruction);
            }
        }

    }
}