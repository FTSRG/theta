package hu.bme.mit.theta.solver.smtlib.impl.generic;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.common.QuadFunction;
import hu.bme.mit.theta.common.TernaryOperator;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.ParamDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.abstracttype.AddExpr;
import hu.bme.mit.theta.core.type.abstracttype.DivExpr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.abstracttype.GeqExpr;
import hu.bme.mit.theta.core.type.abstracttype.GtExpr;
import hu.bme.mit.theta.core.type.abstracttype.LeqExpr;
import hu.bme.mit.theta.core.type.abstracttype.LtExpr;
import hu.bme.mit.theta.core.type.abstracttype.ModExpr;
import hu.bme.mit.theta.core.type.abstracttype.MulExpr;
import hu.bme.mit.theta.core.type.abstracttype.NegExpr;
import hu.bme.mit.theta.core.type.abstracttype.RemExpr;
import hu.bme.mit.theta.core.type.abstracttype.SubExpr;
import hu.bme.mit.theta.core.type.anytype.IteExpr;
import hu.bme.mit.theta.core.type.arraytype.ArrayReadExpr;
import hu.bme.mit.theta.core.type.arraytype.ArrayType;
import hu.bme.mit.theta.core.type.arraytype.ArrayWriteExpr;
import hu.bme.mit.theta.core.type.booltype.AndExpr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.IffExpr;
import hu.bme.mit.theta.core.type.booltype.ImplyExpr;
import hu.bme.mit.theta.core.type.booltype.NotExpr;
import hu.bme.mit.theta.core.type.booltype.OrExpr;
import hu.bme.mit.theta.core.type.booltype.XorExpr;
import hu.bme.mit.theta.core.type.bvtype.BvAddExpr;
import hu.bme.mit.theta.core.type.bvtype.BvAndExpr;
import hu.bme.mit.theta.core.type.bvtype.BvArithShiftRightExpr;
import hu.bme.mit.theta.core.type.bvtype.BvConcatExpr;
import hu.bme.mit.theta.core.type.bvtype.BvExprs;
import hu.bme.mit.theta.core.type.bvtype.BvExtractExpr;
import hu.bme.mit.theta.core.type.bvtype.BvLitExpr;
import hu.bme.mit.theta.core.type.bvtype.BvLogicShiftRightExpr;
import hu.bme.mit.theta.core.type.bvtype.BvNegExpr;
import hu.bme.mit.theta.core.type.bvtype.BvNotExpr;
import hu.bme.mit.theta.core.type.bvtype.BvOrExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSDivExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSExtExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSGeqExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSGtExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSLeqExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSLtExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSModExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSRemExpr;
import hu.bme.mit.theta.core.type.bvtype.BvShiftLeftExpr;
import hu.bme.mit.theta.core.type.bvtype.BvSubExpr;
import hu.bme.mit.theta.core.type.bvtype.BvType;
import hu.bme.mit.theta.core.type.bvtype.BvUDivExpr;
import hu.bme.mit.theta.core.type.bvtype.BvUGeqExpr;
import hu.bme.mit.theta.core.type.bvtype.BvUGtExpr;
import hu.bme.mit.theta.core.type.bvtype.BvULeqExpr;
import hu.bme.mit.theta.core.type.bvtype.BvULtExpr;
import hu.bme.mit.theta.core.type.bvtype.BvURemExpr;
import hu.bme.mit.theta.core.type.bvtype.BvXorExpr;
import hu.bme.mit.theta.core.type.bvtype.BvZExtExpr;
import hu.bme.mit.theta.core.type.functype.FuncExprs;
import hu.bme.mit.theta.core.type.functype.FuncLitExpr;
import hu.bme.mit.theta.core.type.functype.FuncType;
import hu.bme.mit.theta.core.type.inttype.IntToRatExpr;
import hu.bme.mit.theta.core.utils.BvUtils;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibModel;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolverException;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.solver.parser.ThrowExceptionErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.decl.Decls.Param;
import static hu.bme.mit.theta.core.type.arraytype.ArrayExprs.Array;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Exists;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Forall;
import static hu.bme.mit.theta.core.type.functype.FuncExprs.Func;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;
import static hu.bme.mit.theta.core.utils.TypeUtils.castBv;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.BinaryContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.DecimalContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Exists_termContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Forall_termContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Generic_termContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.HexadecimalContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.IdentifierContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.IndexContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.NumeralContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Qual_identifierContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.SortContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Spec_constantContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.SymbolContext;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.TermContext;
import static java.util.stream.Collectors.toList;

public class GenericSmtLibTermTransformer implements SmtLibTermTransformer {
    protected final SmtLibSymbolTable symbolTable;
    protected final Map<String, OperatorCreatorFunction> funAppTransformer;

    public GenericSmtLibTermTransformer(final SmtLibSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.funAppTransformer = new HashMap<>() {{
            // Generic
            put("ite", exprIteOperator());

            // Abstract
            put("=", exprRelationalOperator(EqExpr::create2));
            put("<=", exprRelationalOperator(LeqExpr::create2));
            put("<", exprRelationalOperator(LtExpr::create2));
            put(">=", exprRelationalOperator(GeqExpr::create2));
            put(">", exprRelationalOperator(GtExpr::create2));
            put("+", exprMultiaryOperator(AddExpr::create2));
            put("-", exprMinusOperator());
            put("*", exprMultiaryOperator(MulExpr::create2));
            put("div", exprBinaryOperator(DivExpr::create2));
            put("/", exprBinaryOperator(DivExpr::create2));
            put("mod", exprBinaryOperator(ModExpr::create2));
            put("rem", exprBinaryOperator(RemExpr::create2));

            // Booleal
            put("not", exprUnaryOperator(NotExpr::create));
            put("or", exprMultiaryOperator(OrExpr::create));
            put("and", exprMultiaryOperator(AndExpr::create));
            put("xor", exprBinaryOperator(XorExpr::create));
            put("iff", exprBinaryOperator(IffExpr::create));
            put("=>", exprBinaryOperator(ImplyExpr::create));

            // Integer
            put("to_real", exprUnaryOperator(IntToRatExpr::create));

            // Rational

            // Bitvector
            put("concat", exprMultiaryOperator(BvConcatExpr::create));
            put("extract", exprBvExtractOperator());
            put("zero_extend", exprBvExtendOperator(BvZExtExpr::create));
            put("sign_extend", exprBvExtendOperator(BvSExtExpr::create));
            put("bvadd", exprMultiaryOperator(BvAddExpr::create));
            put("bvsub", exprBinaryOperator(BvSubExpr::create));
            put("bvneg", exprUnaryOperator(BvNegExpr::create));
            put("bvmul", exprMultiaryOperator(BvAddExpr::create));
            put("bvudiv", exprBinaryOperator(BvUDivExpr::create));
            put("bvsdiv", exprBinaryOperator(BvSDivExpr::create));
            put("bvsmod", exprBinaryOperator(BvSModExpr::create));
            put("bvsrem", exprBinaryOperator(BvURemExpr::create));
            put("bvurem", exprBinaryOperator(BvSRemExpr::create));
            put("bvand", exprMultiaryOperator(BvAndExpr::create));
            put("bvor", exprMultiaryOperator(BvOrExpr::create));
            put("bvxor", exprMultiaryOperator(BvXorExpr::create));
            put("bvnot", exprUnaryOperator(BvNotExpr::create));
            put("bvshl", exprBinaryOperator(BvShiftLeftExpr::create));
            put("bvashr", exprBinaryOperator(BvArithShiftRightExpr::create));
            put("bvlshr", exprBinaryOperator(BvLogicShiftRightExpr::create));
            put("bvult", exprBinaryOperator(BvULtExpr::create));
            put("bvslt", exprBinaryOperator(BvSLtExpr::create));
            put("bvule", exprBinaryOperator(BvULeqExpr::create));
            put("bvsle", exprBinaryOperator(BvSLeqExpr::create));
            put("bvugt", exprBinaryOperator(BvUGtExpr::create));
            put("bvsgt", exprBinaryOperator(BvSGtExpr::create));
            put("bvuge", exprBinaryOperator(BvUGeqExpr::create));
            put("bvsge", exprBinaryOperator(BvSGeqExpr::create));

            // Array
            put("select", exprArrayReadOperator());
            put("store", exprArrayWriteOperator());
        }};
    }

    /* Public interface */

    @Override
    public <P extends Type, R extends Type> LitExpr<FuncType<P, R>> toFuncLitExpr(final String funcLitImpl, final FuncType<P, R> type, final SmtLibModel model) {
        final var litExpr = toFuncLitExpr(funcLitImpl, model);
        if(litExpr == null) {
            return null;
        }
        else if(litExpr instanceof LitExpr) {
            return (LitExpr<FuncType<P, R>>) cast(litExpr, type);
        }
        else {
            return (LitExpr<FuncType<P, R>>) cast(ExprUtils.simplify(litExpr), type);
        }
    }

    private Expr<?> toFuncLitExpr(final String funcLitImpl, final SmtLibModel model) {
        final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(funcLitImpl));
        final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowExceptionErrorListener());
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowExceptionErrorListener());

        return transformFuncDef(parser.function_def(), model, HashBiMap.create());
    }

    @Override
    public <T extends Type> Expr<T> toExpr(final String term, final T type, final SmtLibModel model) {
        final var expr  = toExpr(term, model);
        return cast(expr, type);
    }

    private Expr<?> toExpr(final String term, final SmtLibModel model) {
        final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(term));
        final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowExceptionErrorListener());
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowExceptionErrorListener());

        return transformTerm(parser.term(), model, HashBiMap.create());
    }

    @Override
    public <T extends Type> LitExpr<T> toLitExpr(final String litImpl, final T type, final SmtLibModel model) {
        final var litExpr = toLitExpr(litImpl, model);

        if(litExpr == null) {
            return null;
        }
        else if(litExpr instanceof LitExpr) {
            return (LitExpr<T>) cast(litExpr, type);
        }
        else {
            return (LitExpr<T>) cast(ExprUtils.simplify(litExpr), type);
        }
    }

    private Expr<?> toLitExpr(final String litImpl, final SmtLibModel model) {
        final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(litImpl));
        final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowExceptionErrorListener());
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowExceptionErrorListener());

        final var funcDef = parser.function_def();
        final var paramDecls = funcDef.sorted_var().stream()
            .map(sv -> Param(sv.symbol().getText(), transformSort(sv.sort())))
            .collect(toList());

        final var vars = HashBiMap.<ParamDecl<?>, String>create();
        pushParams(paramDecls, vars);
        final var expr = transformTerm(funcDef.term(), model, vars);
        popParams(paramDecls, vars);

        return expr;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends Type, E extends Type>  LitExpr<ArrayType<I, E>> toArrayLitExpr(final String arrayLitImpl, final ArrayType<I, E> type, final SmtLibModel model) {
        final var arrayLitExpr = toLitExpr(arrayLitImpl, model);

        if(arrayLitExpr == null) {
            return null;
        }
        else if(arrayLitExpr instanceof IteExpr) {
            final var entryExprsBuilder = new ImmutableList.Builder<Tuple2<Expr<I>, Expr<E>>>();
            var iteExpr = (IteExpr<E>) arrayLitExpr;
            while (true) {
                entryExprsBuilder.add(Tuple2.of((Expr<I>) iteExpr.getCond().getOps().get(1), iteExpr.getThen()));
                if (iteExpr.getElse() instanceof IteExpr) {
                    iteExpr = (IteExpr<E>) iteExpr.getElse();
                } else {
                    return Array(entryExprsBuilder.build(), iteExpr.getElse(), type);
                }
            }
        }
        else {
            return (LitExpr<ArrayType<I, E>>) cast(ExprUtils.simplify(arrayLitExpr), type);
        }
    }

    @Override
    public LitExpr<BvType> toBvLitExpr(final String bvLitImpl, final BvType type, final SmtLibModel model) {
        final var bvLitExpr = toLitExpr(bvLitImpl, model);

        if(bvLitExpr == null) {
            return null;
        }
        else if(bvLitExpr instanceof BvLitExpr) {
            return (BvLitExpr) bvLitExpr;
        }
        else {
            return (LitExpr<BvType>) cast(ExprUtils.simplify(bvLitExpr), type);
        }
    }

    /* End of public interface */

    /* Visitor implementation */

    protected Expr<?> transformFuncDef(final SMTLIBv2Parser.Function_defContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var paramDecls = ctx.sorted_var().stream()
            .map(sv -> Param(sv.symbol().getText(), transformSort(sv.sort())))
            .collect(toList());
        checkArgument(paramDecls.size() == 1, "Only unary functions are supported");

        pushParams(paramDecls, vars);
        final var op = transformTerm(ctx.term(), model, vars);
        popParams(paramDecls, vars);

        return Func(paramDecls.get(0), op);
    }

    protected Expr<?> transformTerm(final TermContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        if(ctx.spec_constant() != null) {
            return transformSpecConstant(ctx.spec_constant(), model, vars);
        }
        else if(ctx.qual_identifier() != null) {
            return transformQualIdentifier(ctx.qual_identifier(), model, vars);
        }
        else if(ctx.generic_term() != null) {
            return transformGenericTerm(ctx.generic_term(), model, vars);
        }
        else if(ctx.let_term() != null) {
            throw new UnsupportedOperationException();
        }
        else if(ctx.forall_term() != null) {
            return transformForallTerm(ctx.forall_term(), model, vars);
        }
        else if(ctx.exists_term() != null) {
            return transformExistsTerm(ctx.exists_term(), model, vars);
        }
        else if(ctx.match_term() != null) {
            throw new UnsupportedOperationException();
        }
        else if(ctx.annotate_term() != null) {
            return transformTerm(ctx.annotate_term().term(), model, vars);
        }
        else {
            throw new SmtLibSolverException("Invalid input");
        }
    }

    protected Expr<?> transformSpecConstant(final Spec_constantContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        if(ctx.numeral() != null) {
            return transformNumeral(ctx.numeral(), model, vars);
        }
        else if(ctx.decimal() != null) {
            return transformDecimal(ctx.decimal(), model, vars);
        }
        else if(ctx.hexadecimal() != null) {
            return transformHexadecimal(ctx.hexadecimal(), model, vars);
        }
        else if(ctx.binary() != null) {
            return transformBinary(ctx.binary(), model, vars);
        }
        else if(ctx.string() != null) {
            throw new UnsupportedOperationException();
        }
        else {
            throw new SmtLibSolverException("Invalid input");
        }
    }

    protected Expr<?> transformQualIdentifier(final Qual_identifierContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        return transformIdentifier(ctx.identifier(), model, vars);
    }

    protected Expr<?> transformGenericTerm(final Generic_termContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var funName = ctx.qual_identifier().identifier().symbol().getText();

        final var funParams = ctx.qual_identifier().identifier().index();
        final var funAppParams = ctx.term();

        if (funName.equals("const")) { // as const construct
            final var constType = transformSort(ctx.qual_identifier().sort());
            if (constType instanceof ArrayType) {
                checkArgument(funAppParams.size() == 1, "Invalid as const construct");

                final var arrayType = (ArrayType<?, ?>) constType;
                final var expr = transformTerm(funAppParams.get(0), model, vars);
                return createArrayLitExpr(expr, arrayType);
            }
            else {
                throw new UnsupportedOperationException();
            }
        } else if (funAppTransformer.containsKey(funName)) { // known function application
            return funAppTransformer.get(funName).apply(funParams, funAppParams, model, vars);
        } else { // custom function application
            checkArgument(funParams.size() == 0, "Custom unary function application cannot have parameter");
            checkArgument(funAppParams.size() == 1, "Only unary functions are supported");

            return createFuncAppExpr(funName, funAppParams.get(0), model, vars);
        }
    }

    @SuppressWarnings("unchecked")
    private <I extends Type, E extends Type> Expr<?> createArrayLitExpr(final Expr<?> elze, final ArrayType<I, E> type) {
        return Array(Collections.emptyList(), (Expr<E>) elze, type);
    }

    private <P extends Type, R extends Type> Expr<?> createFuncAppExpr(final String funName, final TermContext funAppParam, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        final Expr<?> funcExpr;
        if (symbolTable.definesSymbol(funName)) {
            funcExpr = checkNotNull(symbolTable.getConst(funName).getRef());
        } else {
            final var funDefImpl = model.getTerm(funName);
            funcExpr = toFuncLitExpr(funDefImpl, model);
        }

        assert funcExpr.getType() instanceof FuncType;
        @SuppressWarnings("unchecked") final var funType = (FuncType<P, R>) funcExpr.getType();
        final var paramExpr = transformTerm(funAppParam, model, vars);

        return FuncExprs.App(cast(funcExpr, funType), cast(paramExpr, funType.getParamType()));
    }

    protected Expr<?> transformForallTerm(final Forall_termContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var paramDecls = ctx.sorted_var().stream()
            .map(sv -> Param(sv.symbol().getText(), transformSort(sv.sort())))
            .collect(toList());

        pushParams(paramDecls, vars);
        final var op = transformTerm(ctx.term(), model, vars);
        popParams(paramDecls, vars);

        assert op != null;
        return Forall(paramDecls, cast(op, Bool()));
    }

    protected Expr<?> transformExistsTerm(final Exists_termContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var paramDecls = ctx.sorted_var().stream()
            .map(sv -> Param(sv.symbol().getText(), transformSort(sv.sort())))
            .collect(toList());

        pushParams(paramDecls, vars);
        final var op = transformTerm(ctx.term(), model, vars);
        popParams(paramDecls, vars);

        assert op != null;
        return Exists(paramDecls, cast(op, Bool()));
    }

    protected Expr<?> transformIdentifier(final IdentifierContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        if(ctx.symbol().getText().equals("as-array")) {
            final var name = ctx.index().get(0).getText();
            final var funcLit = (FuncLitExpr<?, ?>) toFuncLitExpr(model.getTerm(name), model);
            return funcLit.getResult();
        }
        else if(ctx.symbol().getText().startsWith("bv")) {
            final var value = ctx.symbol().getText().substring(2);
            final var bvSize = Integer.parseInt(ctx.index().get(0).getText());
            return BvUtils.bigIntegerToNeutralBvLitExpr(new BigInteger(value), bvSize);
        }
        else {
            return transformSymbol(ctx.symbol(), model, vars);
        }
    }

    protected Expr<?> transformSymbol(final SymbolContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var value = ctx.getText();
        switch (value) {
            case "true":
                return BoolExprs.True();
            case "false":
                return BoolExprs.False();
            default:
                if(vars.containsValue(value)) {
                    final var decl = vars.inverse().get(value);
                    return decl.getRef();
                }
                else if(symbolTable.definesSymbol(value)) {
                    return symbolTable.getConst(value).getRef();
                }
                else {
                    throw new UnsupportedOperationException();
                }
        }
    }

    protected Expr<?> transformNumeral(final NumeralContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        return Int(ctx.getText());
    }

    protected Expr<?> transformDecimal(final DecimalContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var decimal = new BigDecimal(ctx.getText());
        if(decimal.scale() <= 0) {
            return Rat(decimal.unscaledValue(), BigInteger.ONE);
        }
        else {
            return Rat(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()));
        }
    }

    protected Expr<?> transformHexadecimal(final HexadecimalContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var numStr = ctx.getText().substring(2);
        final var num = new BigInteger(numStr, 16);
        return BvUtils.bigIntegerToNeutralBvLitExpr(num, numStr.length() * 4);
    }

    protected Expr<?> transformBinary(final BinaryContext ctx, final SmtLibModel model, final BiMap<ParamDecl<?>, String> vars) {
        assert model != null;
        assert vars != null;

        final var numStr = ctx.getText().substring(2);
        final var num = new BigInteger(numStr, 2);
        return BvUtils.bigIntegerToNeutralBvLitExpr(num, numStr.length());
    }

    protected Type transformSort(final SortContext ctx) {
        final var name = ctx.identifier().symbol().getText();
        switch(name) {
            case "Bool":
                return Bool();
            case "Int":
                return Int();
            case "Real":
                return Rat();
            case "BitVec":
                assert ctx.identifier().index().size() == 1;
                return BvExprs.BvType(Integer.parseInt(ctx.identifier().index().get(0).getText()));
            case "Array":
                assert ctx.sort().size() == 2;
                return Array(transformSort(ctx.sort().get(0)), transformSort(ctx.sort().get(1)));
            default:
                throw new UnsupportedOperationException();
        }
    }

    /* End of visitor implementation */

    /* Variable scope handling */

    protected <T extends Type> void pushParams(final List<ParamDecl<T>> paramDecls, BiMap<ParamDecl<?>, String> vars) {
        vars.putAll(paramDecls.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), Decl::getName)));
    }

    protected <T extends Type> void popParams(final List<ParamDecl<T>> paramDecls, BiMap<ParamDecl<?>, String> vars) {
        for (final var paramDecl : paramDecls) {
            vars.remove(paramDecl, paramDecl.getName());
        }
    }

    /* Utilities */

    @SuppressWarnings("unused")
    private OperatorCreatorFunction exprNullaryOperator(final Supplier<Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 0, "Nullary operator expected");
            return function.get();
        };
    }

    private OperatorCreatorFunction exprUnaryOperator(final UnaryOperator<Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 1, "Unary operator expected");

            final var op = transformTerm(ops.get(0), model, vars);
            return function.apply(op);
        };
    }

    private OperatorCreatorFunction exprBvExtractOperator() {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 2, "Two parameters expected");
            checkArgument(ops.size() == 1, "Unary operator expected");

            final var until = Integer.parseInt(params.get(0).numeral().getText()) + 1;
            final var from = Integer.parseInt(params.get(1).numeral().getText());
            final var extractFrom = castBv(transformTerm(ops.get(0), model, vars));
            return BvExtractExpr.create(extractFrom, Int(from), Int(until));
        };
    }

    private OperatorCreatorFunction exprBvExtendOperator(final BiFunction<Expr<?>, BvType, Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 1, "One parameters expected");
            checkArgument(ops.size() == 1, "Unary operator expected");

            final var extendWith = Integer.parseInt(params.get(0).numeral().getText());
            final var toExtend = castBv(transformTerm(ops.get(0), model, vars));
            return function.apply(toExtend, BvType.of(toExtend.getType().getSize() + extendWith));
        };
    }

    private OperatorCreatorFunction exprBinaryOperator(final BinaryOperator<Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 2, "Binary operator expected");

            final var op1 = transformTerm(ops.get(0), model, vars);
            final var op2 = transformTerm(ops.get(1), model, vars);
            return function.apply(op1, op2);
        };
    }

    private OperatorCreatorFunction exprRelationalOperator(final BinaryOperator<Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 2, "Binary operator expected");

            final var op1 = transformTerm(ops.get(0), model, vars);
            final var op2 = transformTerm(ops.get(1), model, vars);
            return function.apply(op1, op2);
        };
    }

    private OperatorCreatorFunction exprArrayReadOperator() {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 2, "Binary operator expected");

            final var op1 = transformTerm(ops.get(0), model, vars);
            final var op2 = transformTerm(ops.get(1), model, vars);
            return ArrayReadExpr.create(op1, op2);
        };
    }

    private OperatorCreatorFunction exprMinusOperator() {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 1 || ops.size() == 2, "Unary or binary operator expected");
            if(ops.size() == 2) {
                return exprBinaryOperator(SubExpr::create2).apply(params, ops, model, vars);
            }
            else {
                return exprUnaryOperator(NegExpr::create2).apply(params, ops, model, vars);
            }
        };
    }

    @SuppressWarnings("unused")
    private OperatorCreatorFunction exprTernaryOperator(final TernaryOperator<Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 3, "Ternary operator expected");
            final Expr<?> op1 = transformTerm(ops.get(0), model, vars);
            final Expr<?> op2 = transformTerm(ops.get(1), model, vars);
            final Expr<?> op3 = transformTerm(ops.get(2), model, vars);
            return function.apply(op1, op2, op3);
        };
    }

    private OperatorCreatorFunction exprIteOperator() {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 3, "Ternary operator expected");

            final var op1 = transformTerm(ops.get(0), model, vars);
            final var op2 = transformTerm(ops.get(1), model, vars);
            final var op3 = transformTerm(ops.get(2), model, vars);
            return IteExpr.create(op1, op2, op3);
        };
    }

    private OperatorCreatorFunction exprArrayWriteOperator() {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");
            checkArgument(ops.size() == 3, "Ternary operator expected");

            final var op1 = transformTerm(ops.get(0), model, vars);
            final var op2 = transformTerm(ops.get(1), model, vars);
            final var op3 = transformTerm(ops.get(2), model, vars);
            return ArrayWriteExpr.create(op1, op2, op3);
        };
    }

    private OperatorCreatorFunction exprMultiaryOperator(final Function<List<Expr<?>>, Expr<?>> function) {
        return (params, ops, model, vars) -> {
            checkArgument(params.size() == 0, "No parameters expected");

            return function.apply(ops.stream().map(op -> transformTerm(op, model, vars)).collect(Collectors.toUnmodifiableList()));
        };
    }

    private interface OperatorCreatorFunction extends QuadFunction<
        List<IndexContext>,             // Parameters
        List<TermContext>,              // Operands
        SmtLibModel,                    // The model
        BiMap<ParamDecl<?>, String>,    // The variable (param) store
        Expr<?>                         // Return type
    > {}
}
