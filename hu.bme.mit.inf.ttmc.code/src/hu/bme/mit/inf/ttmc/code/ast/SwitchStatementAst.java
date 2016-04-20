package hu.bme.mit.inf.ttmc.code.ast;

import hu.bme.mit.inf.ttmc.code.ast.visitor.StatementVisitor;

public class SwitchStatementAst extends StatementAst {

	private ExpressionAst expr;
	private StatementAst body;
	
	public SwitchStatementAst(ExpressionAst expr, StatementAst body) {
		this.expr = expr;
		this.body = body;
	}
	
	public ExpressionAst getExpression() {
		return this.expr;
	}
	
	public StatementAst getBody() {
		return this.body;
	}
	
	@Override
	public <S> S accept(StatementVisitor<S> visitor) {
		return visitor.visit(this);
	}

	@Override
	public StatementAst copy() {
		return new SwitchStatementAst(this.expr.copy(), this.body.copy());
	}

	@Override
	public AstNode[] getChildren() {
		return new AstNode[] { this.expr, this.body };
	}

}
