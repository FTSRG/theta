package hu.bme.mit.inf.ttmc.formalism.ta;

import java.util.Collection;

import hu.bme.mit.inf.ttmc.constraint.expr.Expr;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.formalism.common.Loc;

public interface TALoc extends Loc {

	public Collection<Expr<? extends BoolType>> getInvars();

	@Override
	public Collection<? extends TAEdge> getInEdges();

	@Override
	public Collection<? extends TAEdge> getOutEdges();

}
