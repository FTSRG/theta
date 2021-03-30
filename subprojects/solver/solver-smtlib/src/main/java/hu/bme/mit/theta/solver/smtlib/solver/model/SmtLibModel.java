package hu.bme.mit.theta.solver.smtlib.solver.model;

import java.util.Collection;
import java.util.Map;

public class SmtLibModel {
    protected final Map<String, String> values;

    public SmtLibModel(final Map<String, String> values) {
        this.values = values;
    }

    public Collection<String> getDecls() {
        return values.keySet();
    }

    public String getTerm(final String symbol) {
        return values.get(symbol);
    }
}
