package hu.bme.mit.theta.solver.smtlib.impl.generic;

import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.functype.FuncType;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibDeclTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class GenericSmtLibDeclTransformer implements SmtLibDeclTransformer {
    private final SmtLibTransformationManager transformer;
    private final SmtLibSymbolTable symbolTable;

    private int symbolCount;

    public GenericSmtLibDeclTransformer(final SmtLibTransformationManager transformer, final SmtLibSymbolTable symbolTable) {
        this.transformer = transformer;
        this.symbolTable = symbolTable;

        symbolCount = 0;
    }

    @Override
    public String toSymbol(final Decl<?> decl) {
        if (decl instanceof ConstDecl) {
            final ConstDecl<?> cdecl = (ConstDecl<?>) decl;
            if(!symbolTable.definesConst(cdecl)) {
                transformConst(cdecl);
            }
            return symbolTable.getSymbol(cdecl);
        } else {
            throw new UnsupportedOperationException("Cannot transform declaration: " + decl);
        }
    }

    @Override
    public String toDeclaration(final Decl<?> decl) {
        if (decl instanceof ConstDecl) {
            final ConstDecl<?> cdecl = (ConstDecl<?>) decl;
            if(!symbolTable.definesConst(cdecl)) {
                transformConst(cdecl);
            }
            return symbolTable.getDeclaration(cdecl);
        } else {
            throw new UnsupportedOperationException("Cannot transform declaration: " + decl);
        }
    }

    private void transformConst(final ConstDecl<?> decl) {
        final Type type = decl.getType();

        final Tuple2<List<Type>, Type> extractedTypes = extractTypes(type);
        final List<Type> paramTypes = extractedTypes.get1();
        final Type returnType = extractedTypes.get2();

        final String returnSort = transformer.toSort(returnType);
        final String[] paramSorts = paramTypes.stream().map(transformer::toSort)
                .toArray(String[]::new);

        final String symbolName = symbolNameFor(decl);
        final String symbolDeclaration = String.format(
            "(declare-fun %s (%s) %s)",
            symbolName, String.join(" ", paramSorts), returnSort
        );
        symbolTable.put(decl, symbolName, symbolDeclaration);
    }

    private Tuple2<List<Type>, Type> extractTypes(final Type type) {
        if (type instanceof FuncType<?, ?>) {
            final FuncType<?, ?> funcType = (FuncType<?, ?>) type;

            final Type paramType = funcType.getParamType();
            final Type resultType = funcType.getResultType();

            checkArgument(!(paramType instanceof FuncType));

            final Tuple2<List<Type>, Type> subResult = extractTypes(resultType);
            final List<Type> paramTypes = subResult.get1();
            final Type newResultType = subResult.get2();
            final List<Type> newParamTypes = ImmutableList.<Type>builder().add(paramType).addAll(paramTypes).build();
            final Tuple2<List<Type>, Type> result = Tuple2.of(newParamTypes, newResultType);

            return result;
        } else {
            return Tuple2.of(ImmutableList.of(), type);
        }
    }

    private String symbolNameFor(final Decl<?> decl) {
        return String.format("%s_%d", decl.getName(), symbolCount++);
    }

}
