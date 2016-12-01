package hu.bme.mit.theta.formalism.cfa;

import java.util.List;

import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.formalism.common.Edge;

public interface CfaEdge extends Edge<CfaLoc, CfaEdge> {

	List<Stmt> getStmts();

}
