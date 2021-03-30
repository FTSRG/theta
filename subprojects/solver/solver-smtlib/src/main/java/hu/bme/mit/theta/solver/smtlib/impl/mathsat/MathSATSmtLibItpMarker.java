package hu.bme.mit.theta.solver.smtlib.impl.mathsat;

import hu.bme.mit.theta.solver.smtlib.solver.interpolation.SmtLibItpMarker;

public class MathSATSmtLibItpMarker extends SmtLibItpMarker {
    private static final String markerPattern = "_mathsat_marker_%d";
    private static long markerCount = 0;

    static void resetMarkerCount() {
        markerCount = 0;
    }

    private final String markerName;

    public MathSATSmtLibItpMarker() {
        super();
        markerName = String.format(markerPattern, markerCount++);
    }

    public String getMarkerName() {
        return markerName;
    }
}
