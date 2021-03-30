package hu.bme.mit.theta.solver.smtlib.solver.transformer;

import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;

public interface SmtLibTransformationManager {
    String toSort(Type type);

    String toSymbol(Decl<?> decl);

    String toTerm(Expr<?> expr);
}
