package hu.bme.mit.theta.solver.smtlib.impl.generic;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class GenericSmtLibSymbolTable implements SmtLibSymbolTable {
    private static final String problematicCharactersRegex = ":";
    private static final String problematicCharactersReplacement = "\\$";

    private final BiMap<ConstDecl<?>, String> constToSymbol;
    private final BiMap<ConstDecl<?>, String> constToDeclaration;

    public GenericSmtLibSymbolTable() {
        constToSymbol = Maps.synchronizedBiMap(HashBiMap.create());
        constToDeclaration = Maps.synchronizedBiMap(HashBiMap.create());
    }

    @Override
    public boolean definesConst(final ConstDecl<?> constDecl) {
        return constToSymbol.containsKey(constDecl);
    }

    @Override
    public boolean definesSymbol(final String symbol) {
        return constToSymbol.inverse().containsKey(symbol.replaceAll(problematicCharactersRegex, problematicCharactersReplacement));
    }

    @Override
    public String getSymbol(final ConstDecl<?> constDecl) {
        checkArgument(definesConst(constDecl));
        return constToSymbol.get(constDecl);
    }

    @Override
    public String getDeclaration(final ConstDecl<?> constDecl) {
        checkArgument(definesConst(constDecl));
        return constToDeclaration.get(constDecl);
    }

    @Override
    public ConstDecl<?> getConst(final String symbol) {
        checkArgument(definesSymbol(symbol));
        return constToSymbol.inverse().get(symbol);
    }

    @Override
    public void put(final ConstDecl<?> constDecl, final String symbol, final String declaration) {
        checkNotNull(constDecl);
        checkNotNull(symbol);
        checkNotNull(declaration);
        checkState(!constToSymbol.containsKey(constDecl), "Constant not found.");
        constToSymbol.put(constDecl, symbol.replaceAll(problematicCharactersRegex, problematicCharactersReplacement));
        constToDeclaration.put(constDecl, declaration.replaceAll(problematicCharactersRegex, problematicCharactersReplacement));
    }
}
