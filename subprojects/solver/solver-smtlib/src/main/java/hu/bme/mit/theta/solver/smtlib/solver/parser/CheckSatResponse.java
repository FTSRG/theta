package hu.bme.mit.theta.solver.smtlib.solver.parser;

import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolverException;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Check_sat_responseContext;

import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.PS_Sat;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.PS_Unknown;
import static hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.PS_Unsat;

public class CheckSatResponse implements SpecificResponse {
    private enum Status {
        SAT, UNSAT, UNKNOWN
    }

    private final Status status;

    private CheckSatResponse(Status status) {
        this.status = status;
    }

    public static CheckSatResponse fromContext(final Check_sat_responseContext ctx) {
        switch (ctx.value.getType()) {
            case PS_Sat:
                return new CheckSatResponse(Status.SAT);
            case PS_Unsat:
                return new CheckSatResponse(Status.UNSAT);
            case PS_Unknown:
                return new CheckSatResponse(Status.UNKNOWN);
            default:
                throw new SmtLibSolverException("Invalid interface");

        }
    }

    public boolean isSat() {
        return status == Status.SAT;
    }

    public boolean isUnsat() {
        return status == Status.UNSAT;
    }

    public boolean isUnknown() {
        return status == Status.UNKNOWN;
    }
}
