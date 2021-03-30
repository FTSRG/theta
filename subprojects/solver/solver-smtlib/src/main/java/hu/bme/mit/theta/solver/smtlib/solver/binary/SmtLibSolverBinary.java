package hu.bme.mit.theta.solver.smtlib.solver.binary;

public interface SmtLibSolverBinary {
    void issueCommand(String command);
    String readResponse();
}
