package hu.bme.mit.theta.solver.smtlib.solver.interpolation;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.ItpMarker;
import hu.bme.mit.theta.solver.Stack;
import hu.bme.mit.theta.solver.impl.StackImpl;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class SmtLibItpMarker implements ItpMarker {
    private final Stack<Tuple2<Expr<BoolType>, String>> terms;

    public SmtLibItpMarker() {
        terms = new StackImpl<>();
    }

    public void add(final Expr<BoolType> assertion, final String term) {
        terms.add(Tuple2.of(checkNotNull(assertion), checkNotNull(term)));
    }

    public void push() {
        terms.push();
    }

    public void pop(final int n) {
        terms.pop(n);
    }

    public Collection<Tuple2<Expr<BoolType>, String>> getTerms() {
        return terms.toCollection();
    }
}
