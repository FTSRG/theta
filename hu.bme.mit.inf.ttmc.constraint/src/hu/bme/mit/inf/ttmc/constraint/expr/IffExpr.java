package hu.bme.mit.inf.ttmc.constraint.expr;

import hu.bme.mit.inf.ttmc.constraint.type.BoolType;

public interface IffExpr extends BinaryExpr<BoolType, BoolType, BoolType> {
	
	@Override
	public IffExpr withOps(final Expr<? extends BoolType> leftOp, final Expr<? extends BoolType> rightOp);
	
	@Override
	public IffExpr withLeftOp(final Expr<? extends BoolType> leftOp);

	@Override
	public IffExpr withRightOp(final Expr<? extends BoolType> rightOp);
}
