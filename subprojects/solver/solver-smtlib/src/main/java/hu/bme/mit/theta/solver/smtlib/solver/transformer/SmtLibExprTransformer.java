package hu.bme.mit.theta.solver.smtlib.solver.transformer;

import hu.bme.mit.theta.core.type.Expr;

public interface SmtLibExprTransformer {
    String toTerm(Expr<?> expr);
}
