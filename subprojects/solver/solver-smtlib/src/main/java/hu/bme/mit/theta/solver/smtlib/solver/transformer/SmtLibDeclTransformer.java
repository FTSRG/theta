package hu.bme.mit.theta.solver.smtlib.solver.transformer;

import hu.bme.mit.theta.core.decl.Decl;

public interface SmtLibDeclTransformer {
    String toSymbol(Decl<?> decl);

    String toDeclaration(Decl<?> decl);
}
