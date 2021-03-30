package hu.bme.mit.theta.solver.smtlib.solver.parser;

import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2BaseVisitor;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Specific_success_responseContext;

public interface SpecificResponse {
    static SpecificResponse fromContext(final Specific_success_responseContext ctx) {
        return ctx.accept(new SMTLIBv2BaseVisitor<>() {
            @Override
            public SpecificResponse visitCheck_sat_response(SMTLIBv2Parser.Check_sat_responseContext ctx) {
                return CheckSatResponse.fromContext(ctx);
            }

            @Override
            public SpecificResponse visitGet_unsat_core_response(SMTLIBv2Parser.Get_unsat_core_responseContext ctx) {
                return GetUnsatCoreResponse.fromContext(ctx);
            }

            @Override
            public SpecificResponse visitGet_model_response(SMTLIBv2Parser.Get_model_responseContext ctx) {
                return GetModelResponse.fromContext(ctx);
            }
        });
    }
}
