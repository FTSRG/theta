package hu.bme.mit.theta.formalism.sts;

import java.util.Collection;
import java.util.List;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.model.Model;
import hu.bme.mit.theta.core.model.impl.Valuation;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.formalism.Formalism;
import hu.bme.mit.theta.formalism.utils.ExprUnroller;

/**
 * Symbolic Transition System (STS) interface.
 */
public interface STS extends Formalism, ExprUnroller {

	public Collection<VarDecl<? extends Type>> getVars();

	public Collection<Expr<? extends BoolType>> getInit();

	public Collection<Expr<? extends BoolType>> getInvar();

	public Collection<Expr<? extends BoolType>> getTrans();

	public Expr<? extends BoolType> getProp();

	// Unrolling methods

	public Collection<? extends Expr<? extends BoolType>> unrollInit(final int i);

	public Collection<? extends Expr<? extends BoolType>> unrollTrans(final int i);

	public Collection<? extends Expr<? extends BoolType>> unrollInv(final int i);

	public Expr<? extends BoolType> unrollProp(final int i);

	public Valuation getConcreteState(final Model model, final int i);

	public Valuation getConcreteState(final Model model, final int i,
			final Collection<VarDecl<? extends Type>> variables);

	public List<Valuation> extractTrace(final Model model, final int length);

	public List<Valuation> extractTrace(final Model model, final int length,
			final Collection<VarDecl<? extends Type>> variables);

	public Expr<? extends BoolType> foldin(final Expr<? extends BoolType> expr, final int i);
}
