package hu.bme.mit.theta.xcfa.ir;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.abstracttype.AbstractExprs;
import hu.bme.mit.theta.core.type.arraytype.ArrayExprs;
import hu.bme.mit.theta.core.type.booltype.BoolLitExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.type.rattype.RatLitExpr;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.type.arraytype.ArrayExprs.Array;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.*;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;

public class Utils {
    private static final int doublePrecision = 1 << 8;

    public static Type createType(String type) {
        type = type.replaceAll("\\*", "");
        String arrayPattern = "\\[([1-9][0-9]*) x (.*)]";
        Pattern pattern = Pattern.compile(arrayPattern);
        Matcher matcher = pattern.matcher(type);
        if(matcher.find()) {
            Integer size = Integer.parseInt(matcher.group(1)); // TODO: how to check bounds?
            type = matcher.group(2);
            return Array(Int(), createType(type));
        }

        switch(type) {
            case "double":
            case "float":
                return Rat();
            case "i64":
            case "i32":
            case "i16":
            case "i8":
                return Int();
            case "i1":  return Bool();
            default: new RuntimeException("Type " + type + " not known! (Using int instead)").printStackTrace(); return Int();
        }
    }

    public static VarDecl<? extends Type> createVariable(String name, String type) {
        Type t;
        t = createType(type);
        return Var(name, t);
    }

    public static LitExpr<? extends Type> createConstant(String value) {
        String[] arguments = value.split(" ");
        if(arguments.length != 2) {
            System.err.println("Constant should be of form \"(type=[a-zA-Z0-9]*) (value=[\\.0-9fe+-]*)\", got: " + value);
            return IntLitExpr.of(BigInteger.ZERO);
        }

        switch(arguments[0]) {
            case "double":
            case "float":
                return RatLitExpr.of(BigInteger.valueOf((long) (Float.parseFloat(arguments[1]) * doublePrecision)), BigInteger.valueOf(doublePrecision));
            case "i64":
            case "i32":
            case "i16":
            case "i8":
                return IntLitExpr.of(new BigInteger(arguments[1]));
            case "i1":  return BoolLitExpr.of(arguments[1].equals("true"));
            default: new RuntimeException("Type " + arguments[0] + " not known! (Using int(0) instead)").printStackTrace(); return IntLitExpr.of(BigInteger.ZERO);
        }
    }

    public static XcfaProcedure createEmptyProc(String name) {
        XcfaProcedure.Builder builder = XcfaProcedure.builder();
        XcfaLocation loc1 = new XcfaLocation("loc", null);
        XcfaLocation loc2 = new XcfaLocation("loc", null);
        builder.addLoc(loc1);
        builder.addLoc(loc2);
        builder.setFinalLoc(loc2);
        builder.setInitLoc(loc1);
        builder.setName(name);
        return builder.build();
    }

}
