package hu.bme.mit.theta.solver.smtlib.solver.parser;

import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2BaseVisitor;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.General_response_errorContext;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.General_response_successContext;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.General_response_unsupportedContext;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.ResponseContext;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser.Specific_success_responseContext;

public class GeneralResponse {
    private final boolean successful;
    private final String reason;
    private final SpecificResponse specificResponse;

    private GeneralResponse(boolean successful, String reason, SpecificResponse specificResponse) {
        this.successful = successful;
        this.reason = reason;
        this.specificResponse = specificResponse;
    }

    public static GeneralResponse fromContext(final ResponseContext ctx) {
        return ctx.accept(new SMTLIBv2BaseVisitor<>() {
            @Override
            public GeneralResponse visitGeneral_response_success(General_response_successContext ctx) {
                return new GeneralResponse(true, null, null);
            }

            @Override
            public GeneralResponse visitGeneral_response_unsupported(General_response_unsupportedContext ctx) {
                return new GeneralResponse(false, "Unsupported", null);
            }

            @Override
            public GeneralResponse visitSpecific_success_response(Specific_success_responseContext ctx) {
                return new GeneralResponse(true, null, SpecificResponse.fromContext(ctx));
            }

            @Override
            public GeneralResponse visitGeneral_response_error(General_response_errorContext ctx) {
                return new GeneralResponse(false, ctx.reason.getText(), null);
            }
        });
    }

    public boolean isSuccessful() {
        return successful && specificResponse == null;
    }

    public boolean isError() {
        return !successful;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSpecific() {
        return successful && specificResponse != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecificResponse> T asSpecific() {
        return (T) specificResponse;
    }
}
