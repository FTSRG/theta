package hu.bme.mit.theta.formalism.sts.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.expr.AndExpr;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.LitExpr;
import hu.bme.mit.theta.core.model.Model;
import hu.bme.mit.theta.core.model.impl.Valuation;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.impl.ExprUtils;
import hu.bme.mit.theta.core.utils.impl.PathUtils;
import hu.bme.mit.theta.formalism.sts.STS;

/**
 * An immutable STS implementation.
 */
public final class StsImpl implements STS {

	private final Collection<VarDecl<? extends Type>> vars;
	private final Collection<Expr<? extends BoolType>> init;
	private final Collection<Expr<? extends BoolType>> invar;
	private final Collection<Expr<? extends BoolType>> trans;
	private final Expr<? extends BoolType> prop;

	// Protected constructor --> use the builder
	protected StsImpl(final Collection<VarDecl<? extends Type>> vars, final Collection<Expr<? extends BoolType>> init,
			final Collection<Expr<? extends BoolType>> invar, final Collection<Expr<? extends BoolType>> trans,
			final Expr<? extends BoolType> prop) {
		this.vars = Collections.unmodifiableCollection(new ArrayList<>(checkNotNull(vars)));
		this.init = Collections.unmodifiableCollection(new ArrayList<>(checkNotNull(init)));
		this.invar = Collections.unmodifiableCollection(new ArrayList<>(checkNotNull(invar)));
		this.trans = Collections.unmodifiableCollection(new ArrayList<>(checkNotNull(trans)));
		this.prop = checkNotNull(prop);
	}

	@Override
	public Collection<VarDecl<? extends Type>> getVars() {
		return vars;
	}

	@Override
	public Collection<Expr<? extends BoolType>> getInit() {
		return init;
	}

	@Override
	public Collection<Expr<? extends BoolType>> getInvar() {
		return invar;
	}

	@Override
	public Collection<Expr<? extends BoolType>> getTrans() {
		return trans;
	}

	@Override
	public Expr<? extends BoolType> getProp() {
		return prop;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("STS [" + System.lineSeparator());
		appendCollection(sb, "\tVars:  ", vars, System.lineSeparator());
		appendCollection(sb, "\tInit:  ", init, System.lineSeparator());
		appendCollection(sb, "\tInvar: ", invar, System.lineSeparator());
		appendCollection(sb, "\tTrans: ", trans, System.lineSeparator());
		sb.append("\tProp: ").append(prop).append(System.lineSeparator()).append("]");
		return sb.toString();
	}

	private void appendCollection(final StringBuilder sb, final String prefix, final Collection<?> collection,
			final String postfix) {
		sb.append(prefix);
		sb.append(String.join(", ", collection.stream().map(i -> i.toString()).collect(Collectors.toList())));
		sb.append(postfix);
	}

	/**
	 * Helper class for building the immutable STS. It splits AND expressions
	 * into conjuncts, eliminates duplicate expressions and collects the
	 * variables from the expressions automatically.
	 */
	public static class Builder {
		private final Collection<VarDecl<? extends Type>> vars;
		private final Collection<Expr<? extends BoolType>> init;
		private final Collection<Expr<? extends BoolType>> invar;
		private final Collection<Expr<? extends BoolType>> trans;
		private Expr<? extends BoolType> prop;

		public Builder() {
			vars = new HashSet<>();
			init = new HashSet<>();
			invar = new HashSet<>();
			trans = new HashSet<>();
			prop = null;
		}

		/**
		 * Add an initial constraint. If the constraint is an AND expression it
		 * will be split into its conjuncts. Duplicate constraints are included
		 * only once.
		 */
		public Builder addInit(final Expr<? extends BoolType> expr) {
			checkNotNull(expr);
			if (expr instanceof AndExpr)
				addInit(((AndExpr) expr).getOps());
			else
				init.add(checkNotNull(expr));
			return this;
		}

		/**
		 * Add initial constraints. If any constraint is an AND expression it
		 * will be split into its conjuncts. Duplicate constraints are included
		 * only once.
		 */
		public Builder addInit(final Iterable<? extends Expr<? extends BoolType>> exprs) {
			checkNotNull(exprs);
			for (final Expr<? extends BoolType> expr : exprs)
				addInit(expr);
			return this;
		}

		/**
		 * Add an invariant constraint. If the constraint is an AND expression
		 * it will be split into its conjuncts. Duplicate constraints are
		 * included only once.
		 */
		public Builder addInvar(final Expr<? extends BoolType> expr) {
			checkNotNull(expr);
			if (expr instanceof AndExpr)
				addInvar(((AndExpr) expr).getOps());
			else
				invar.add(expr);
			return this;
		}

		/**
		 * Add invariant constraints. If any constraint is an AND expression it
		 * will be split into its conjuncts. Duplicate constraints are included
		 * only once.
		 */
		public Builder addInvar(final Iterable<? extends Expr<? extends BoolType>> exprs) {
			checkNotNull(exprs);
			for (final Expr<? extends BoolType> expr : exprs)
				addInvar(expr);
			return this;
		}

		/**
		 * Add a transition constraint. If the constraint is an AND expression
		 * it will be split into its conjuncts. Duplicate constraints are
		 * included only once.
		 */
		public Builder addTrans(final Expr<? extends BoolType> expr) {
			checkNotNull(expr);
			if (expr instanceof AndExpr)
				addTrans(((AndExpr) expr).getOps());
			else
				trans.add(expr);
			return this;
		}

		/**
		 * Add transition constraints. If any constraint is an AND expression it
		 * will be split into its conjuncts. Duplicate constraints are included
		 * only once.
		 */
		public Builder addTrans(final Iterable<? extends Expr<? extends BoolType>> exprs) {
			checkNotNull(exprs);
			for (final Expr<? extends BoolType> expr : exprs)
				addTrans(expr);
			return this;
		}

		public Builder setProp(final Expr<? extends BoolType> expr) {
			checkNotNull(expr);
			this.prop = expr;
			return this;
		}

		public STS build() {
			checkNotNull(prop);

			ExprUtils.collectVars(init, vars);
			ExprUtils.collectVars(invar, vars);
			ExprUtils.collectVars(trans, vars);
			ExprUtils.collectVars(prop, vars);

			return new StsImpl(vars, init, invar, trans, prop);
		}
	}

	@Override
	public Expr<? extends BoolType> unfold(final Expr<? extends BoolType> expr, final int i) {
		return PathUtils.unfold(expr, i);
	}

	@Override
	public Collection<? extends Expr<? extends BoolType>> unfold(
			final Collection<? extends Expr<? extends BoolType>> exprs, final int i) {
		return exprs.stream().map(e -> unfold(e, i)).collect(Collectors.toSet());
	}

	@Override
	public Collection<? extends Expr<? extends BoolType>> unfoldInit(final int i) {
		return unfold(getInit(), i);
	}

	@Override
	public Collection<? extends Expr<? extends BoolType>> unfoldTrans(final int i) {
		return unfold(getTrans(), i);
	}

	@Override
	public Collection<? extends Expr<? extends BoolType>> unfoldInv(final int i) {
		return unfold(getInvar(), i);
	}

	@Override
	public Expr<? extends BoolType> unfoldProp(final int i) {
		return unfold(getProp(), i);
	}

	@Override
	public Valuation getConcreteState(final Model model, final int i) {
		return getConcreteState(model, i, getVars());
	}

	@Override
	public Valuation getConcreteState(final Model model, final int i,
			final Collection<VarDecl<? extends Type>> variables) {
		final Valuation.Builder builder = Valuation.builder();

		for (final VarDecl<? extends Type> varDecl : variables) {
			LitExpr<? extends Type> value = null;
			try {
				value = model.eval(varDecl.getConstDecl(i)).get();
			} catch (final Exception ex) {
				value = varDecl.getType().getAny();
			}
			builder.put(varDecl, value);
		}

		return builder.build();
	}

	@Override
	public List<Valuation> extractTrace(final Model model, final int length) {
		return extractTrace(model, length, getVars());
	}

	@Override
	public List<Valuation> extractTrace(final Model model, final int length,
			final Collection<VarDecl<? extends Type>> variables) {
		final List<Valuation> trace = new ArrayList<>(length);
		for (int i = 0; i < length; ++i)
			trace.add(getConcreteState(model, i, variables));
		return trace;
	}

	@Override
	public Expr<? extends BoolType> foldin(final Expr<? extends BoolType> expr, final int i) {
		return PathUtils.foldin(expr, i);
	}

}
