package hu.bme.mit.theta.solver.smtlib.solver.transformer;

import hu.bme.mit.theta.core.decl.ConstDecl;

public interface SmtLibSymbolTable {
    boolean definesConst(ConstDecl<?> constDecl);

    boolean definesSymbol(String symbol);

    String getSymbol(ConstDecl<?> constDecl);

    String getDeclaration(ConstDecl<?> constDecl);

    ConstDecl<?> getConst(String symbol);

    void put(ConstDecl<?> constDecl, String symbol, String declaration);
}
