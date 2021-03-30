package hu.bme.mit.theta.solver.smtlib.solver.transformer;

import hu.bme.mit.theta.core.type.Type;

public interface SmtLibTypeTransformer {
    String toSort(Type type);
}
