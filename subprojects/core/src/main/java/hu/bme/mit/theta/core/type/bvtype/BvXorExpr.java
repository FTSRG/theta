package hu.bme.mit.theta.core.type.bvtype;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.BinaryExpr;
import hu.bme.mit.theta.core.type.Expr;

import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class BvXorExpr extends BinaryExpr<BvType, BvType> {
    private static final int HASH_SEED = 9457;
    private static final String OPERATOR_LABEL = "^";

    private BvXorExpr(final Expr<BvType> leftOp, final Expr<BvType> rightOp) {
        super(leftOp, rightOp);
    }

    public static BvXorExpr of(final Expr<BvType> leftOp, final Expr<BvType> rightOp) {
        return new BvXorExpr(leftOp, rightOp);
    }

    public static BvXorExpr create(final Expr<?> leftOp, final Expr<?> rightOp) {
        final Expr<BvType> newLeftOp = cast(leftOp, (BvType) leftOp.getType());
        final Expr<BvType> newRightOp = cast(rightOp, (BvType) leftOp.getType());
        return BvXorExpr.of(newLeftOp, newRightOp);
    }

    @Override
    public BvType getType() {
        return getOps().get(0).getType();
    }

    @Override
    public BvLitExpr eval(final Valuation val) {
        final BvLitExpr leftOpVal = (BvLitExpr) getLeftOp().eval(val);
        final BvLitExpr rightOpVal = (BvLitExpr) getRightOp().eval(val);
        return leftOpVal.xor(rightOpVal);
    }

    @Override
    public BvXorExpr with(final Expr<BvType> leftOp, final Expr<BvType> rightOp) {
        if (leftOp == getLeftOp() && rightOp == getRightOp()) {
            return this;
        } else {
            return BvXorExpr.of(leftOp, rightOp);
        }
    }

    @Override
    public BvXorExpr withLeftOp(final Expr<BvType> leftOp) {
        return with(leftOp, getRightOp());
    }

    @Override
    public BvXorExpr withRightOp(final Expr<BvType> rightOp) {
        return with(getLeftOp(), rightOp);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BvXorExpr) {
            final BvXorExpr that = (BvXorExpr) obj;
            return this.getLeftOp().equals(that.getLeftOp()) && this.getRightOp().equals(that.getRightOp());
        } else {
            return false;
        }
    }

    @Override
    protected int getHashSeed() {
        return HASH_SEED;
    }

    @Override
    public String getOperatorLabel() {
        return OPERATOR_LABEL;
    }
}