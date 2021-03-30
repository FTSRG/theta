package hu.bme.mit.theta.solver.smtlib.impl.generic;

import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibDeclTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibExprTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTypeTransformer;

public class GenericSmtLibTransformationManager implements SmtLibTransformationManager {
    private final SmtLibTypeTransformer typeTransformer;
    private final SmtLibDeclTransformer declTransformer;
    private final SmtLibExprTransformer exprTransformer;

    public GenericSmtLibTransformationManager(final SmtLibSymbolTable symbolTable) {
        this.typeTransformer = instantiateTypeTransformer(this);
        this.declTransformer = instantiateDeclTransformer(this, symbolTable);
        this.exprTransformer = instantiateExprTransformer(this);
    }

    @Override
    public final String toSort(final Type type) {
        return typeTransformer.toSort(type);
    }

    @Override
    public final String toSymbol(final Decl<?> decl) {
        return declTransformer.toSymbol(decl);
    }

    @Override
    public final String toTerm(final Expr<?> expr) {
        return exprTransformer.toTerm(expr);
    }

    protected SmtLibTypeTransformer instantiateTypeTransformer(final SmtLibTransformationManager transformer) {
        return new GenericSmtLibTypeTransformer(transformer);
    }

    protected SmtLibDeclTransformer instantiateDeclTransformer(
        final SmtLibTransformationManager transformer, final SmtLibSymbolTable symbolTable
    ) {
        return new GenericSmtLibDeclTransformer(transformer, symbolTable);
    }

    protected SmtLibExprTransformer instantiateExprTransformer(final SmtLibTransformationManager transformer) {
        return new GenericSmtLibExprTransformer(transformer);
    }
}
