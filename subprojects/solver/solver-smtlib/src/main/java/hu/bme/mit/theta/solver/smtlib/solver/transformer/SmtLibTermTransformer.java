package hu.bme.mit.theta.solver.smtlib.solver.transformer;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.arraytype.ArrayType;
import hu.bme.mit.theta.core.type.bvtype.BvType;
import hu.bme.mit.theta.core.type.functype.FuncType;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibModel;

public interface SmtLibTermTransformer {
    <P extends Type, R extends Type> LitExpr<FuncType<P, R>> toFuncLitExpr(String funcLitImpl, FuncType<P, R> type, SmtLibModel model);

    <T extends Type> Expr<T> toExpr(String term, T type, SmtLibModel model);

    <T extends Type> LitExpr<T> toLitExpr(String litImpl, T type, SmtLibModel model);

    <I extends Type, E extends Type>  LitExpr<ArrayType<I, E>> toArrayLitExpr(String arrayLitImpl, ArrayType<I, E> type, SmtLibModel model);

    LitExpr<BvType> toBvLitExpr(String bvLitImpl, BvType type, SmtLibModel model);
}
