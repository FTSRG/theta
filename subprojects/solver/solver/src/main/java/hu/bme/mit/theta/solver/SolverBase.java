package hu.bme.mit.theta.solver;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;

import java.util.Collection;

public interface SolverBase {


    /**
     * Check if the currently added expressions are satisfiable.
     *
     * @return Status
     */
    SolverStatus check();

    /**
     * Push the current solver state. When calling {@link #pop()}, all expressions added after
     * the last push will be removed.
     */
    void push();

    /**
     * Remove expressions added after the previous n {@link #push()} calls.
     *
     * @param n
     */
    void pop(final int n);

    /**
     * Remove expressions added after the previous {@link #push()} call.
     */
    default void pop() {
        pop(1);
    }

    /**
     * Reset the solver state.
     */
    void reset();

    /**
     * Get the current status of the solver.
     *
     * @return Status
     */
    SolverStatus getStatus();

    /**
     * Get the satisfying assignment for the currently added expressions.
     * Should only be called if {@link #check()} was already called and
     * the result is SAT.
     *
     * @return Satisfying assignment
     */
    Valuation getModel();

    /**
     * Get the currently added expressions.
     *
     * @return Expressions
     */
    Collection<Expr<BoolType>> getAssertions();
}
