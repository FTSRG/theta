package hu.bme.mit.theta.xsts.dsl;

import hu.bme.mit.theta.common.dsl.Env;
import hu.bme.mit.theta.common.dsl.Scope;
import hu.bme.mit.theta.common.dsl.Symbol;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.TypeUtils;
import hu.bme.mit.theta.xsts.dsl.gen.XstsDslBaseVisitor;
import hu.bme.mit.theta.xsts.dsl.gen.XstsDslParser.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.stmt.Stmts.*;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;

public class XstsStatement {

	private final Scope scope;
	private final StmtContext context;

	public XstsStatement(final Scope scope, final StmtContext context) {
		this.scope = checkNotNull(scope);
		this.context = checkNotNull(context);
	}

	public Stmt instantiate(final Env env) {
		final StmtCreatorVisitor visitor = new StmtCreatorVisitor(scope, env);
		final Stmt stmt = context.accept(visitor);
		if (stmt == null) {
			throw new AssertionError();
		} else {
			return stmt;
		}
	}

	private static final class StmtCreatorVisitor extends XstsDslBaseVisitor<Stmt> {

		private final Scope scope;
		private final Env env;

		public StmtCreatorVisitor(final Scope scope, final Env env) {
			this.scope = checkNotNull(scope);
			this.env = checkNotNull(env);
		}

		@Override
		public Stmt visitHavocStmt(final HavocStmtContext ctx) {
			final String lhsId = ctx.lhs.getText();
			final Symbol lhsSymbol = scope.resolve(lhsId).get();
			final VarDecl<?> var = (VarDecl<?>) env.eval(lhsSymbol);
			return Havoc(var);
		}

		@Override
		public Stmt visitAssumeStmt(final AssumeStmtContext ctx) {
			final XstsExpression expression = new XstsExpression(scope, ctx.cond);
			final Expr<BoolType> expr = TypeUtils.cast(expression.instantiate(env), Bool());
			return Assume(expr);
		}

		@Override
		public Stmt visitAssignStmt(final AssignStmtContext ctx) {
			final String lhsId = ctx.lhs.getText();
			final Symbol lhsSymbol = scope.resolve(lhsId).get();
			final VarDecl<?> var = (VarDecl<?>) env.eval(lhsSymbol);

			final XstsExpression expression = new XstsExpression(scope, ctx.value);
			final Expr<?> expr = expression.instantiate(env);

			if (expr.getType().equals(var.getType())) {
				@SuppressWarnings("unchecked") final VarDecl<Type> tVar = (VarDecl<Type>) var;
				@SuppressWarnings("unchecked") final Expr<Type> tExpr = (Expr<Type>) expr;
				return Assign(tVar, tExpr);
			} else {
				throw new IllegalArgumentException("Type of " + var + " is incompatilbe with " + expr);
			}
		}

	}

}
