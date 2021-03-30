package hu.bme.mit.theta.solver.smtlib.impl.generic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hu.bme.mit.theta.common.DispatchTable;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.arraytype.ArrayType;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.bvtype.BvType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTypeTransformer;

import java.util.concurrent.ExecutionException;

public class GenericSmtLibTypeTransformer implements SmtLibTypeTransformer {
    private static final int CACHE_SIZE = 1000;

    @SuppressWarnings("unused")
    private final SmtLibTransformationManager transformer;

    private final Cache<Type, String> typeToSmtLib;
    private final DispatchTable<String> table;

    public GenericSmtLibTypeTransformer(final SmtLibTransformationManager transformer) {
        this.transformer = transformer;

        typeToSmtLib = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();

        table = DispatchTable.<String>builder()
                .addCase(BoolType.class, this::boolType)
                .addCase(IntType.class, this::intType)
                .addCase(RatType.class, this::ratType)
                .addCase(BvType.class, this::bvType)
                .addCase(ArrayType.class, this::arrayType)
                .build();
    }

    @Override
    public final String toSort(final Type type) {
        try {
            return typeToSmtLib.get(type, () -> table.dispatch(type));
        } catch (final ExecutionException e) {
            throw new AssertionError();
        }
    }

    protected String boolType(final BoolType type) {
        return "Bool";
    }

    protected String intType(final IntType type) {
        return "Int";
    }

    protected String ratType(final RatType type) {
        return "Real";
    }

    protected String bvType(final BvType type) {
        return String.format("(_ BitVec %d)", type.getSize());
    }

    protected String arrayType(final ArrayType<?, ?> type) {
        return String.format("(Array %s %s)", toSort(type.getIndexType()), toSort(type.getElemType()));
    }
}
