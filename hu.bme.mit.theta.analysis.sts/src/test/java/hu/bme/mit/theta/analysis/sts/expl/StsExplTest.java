package hu.bme.mit.theta.analysis.sts.expl;

import static hu.bme.mit.theta.core.decl.impl.Decls.Var;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Add;
import static hu.bme.mit.theta.core.expr.impl.Exprs.And;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Eq;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Geq;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Imply;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Int;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Lt;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Not;
import static hu.bme.mit.theta.core.expr.impl.Exprs.Prime;
import static hu.bme.mit.theta.core.type.impl.Types.Int;

import java.util.Collections;
import java.util.function.Predicate;

import org.junit.Test;

import hu.bme.mit.theta.analysis.ExprState;
import hu.bme.mit.theta.analysis.algorithm.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.ArgPrinter;
import hu.bme.mit.theta.analysis.algorithm.impl.AbstractorImpl;
import hu.bme.mit.theta.analysis.algorithm.impl.CEGARLoopImpl;
import hu.bme.mit.theta.analysis.algorithm.impl.RefutationBasedRefiner;
import hu.bme.mit.theta.analysis.algorithm.impl.refinerops.GlobalExplItpRefinerOp;
import hu.bme.mit.theta.analysis.expl.ExplPrecision;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.impl.ExprStatePredicate;
import hu.bme.mit.theta.analysis.refutation.ItpRefutation;
import hu.bme.mit.theta.analysis.sts.StsAction;
import hu.bme.mit.theta.analysis.sts.StsExprSeqConcretizer;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.IntType;
import hu.bme.mit.theta.formalism.sts.STS;
import hu.bme.mit.theta.formalism.sts.impl.StsImpl;
import hu.bme.mit.theta.formalism.sts.impl.StsImpl.Builder;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.SolverManager;
import hu.bme.mit.theta.solver.z3.Z3SolverManager;

public class StsExplTest {

	@Test
	public void test() {

		final VarDecl<IntType> vx = Var("x", Int());
		final Expr<IntType> x = vx.getRef();
		final VarDecl<IntType> vy = Var("y", Int());
		final Expr<IntType> y = vy.getRef();

		final int mod = 10;

		final Builder builder = new StsImpl.Builder();

		builder.addInit(Eq(x, Int(0)));
		builder.addInit(Eq(y, Int(0)));
		builder.addTrans(And(Imply(Lt(x, Int(mod)), Eq(Prime(x), Add(x, Int(1)))),
				Imply(Geq(x, Int(mod)), Eq(Prime(x), Int(0)))));
		builder.addTrans(Eq(Prime(y), Int(0)));
		builder.setProp(Not(Eq(x, Int(mod))));

		final STS sts = builder.build();

		final SolverManager manager = new Z3SolverManager();
		final ItpSolver solver = manager.createItpSolver();

		final StsExplAnalysis analysis = new StsExplAnalysis(sts, solver);
		final Predicate<ExprState> target = new ExprStatePredicate(Not(sts.getProp()), solver);

		final ExplPrecision precision = ExplPrecision.create(Collections.singleton(vy));

		final Abstractor<ExplState, StsAction, ExplPrecision> abstractor = new AbstractorImpl<>(analysis, target);

		final StsExprSeqConcretizer concretizerOp = new StsExprSeqConcretizer(sts, solver);
		final GlobalExplItpRefinerOp<StsAction> refinerOp = new GlobalExplItpRefinerOp<>();

		final RefutationBasedRefiner<ExplState, ExplState, ItpRefutation, ExplPrecision, StsAction> refiner = new RefutationBasedRefiner<>(
				concretizerOp, refinerOp);

		final CEGARLoopImpl<ExplState, StsAction, ExplPrecision, ExplState> cegarLoop = new CEGARLoopImpl<>(abstractor,
				refiner);

		cegarLoop.check(precision);
		System.out.println(ArgPrinter.toGraphvizString(abstractor.getARG()));
		System.out.println(cegarLoop.getStatus());
	}

}
