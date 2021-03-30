/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.core.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.ParamDecl;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.core.type.booltype.AndExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.NotExpr;
import hu.bme.mit.theta.core.utils.IndexedVars.Builder;

/**
 * Utility functions related to expressions.
 */
public final class ExprUtils {

	private ExprUtils() {
	}

	/**
	 * Collect atoms from a Boolean expression into a given collection.
	 *
	 * @param expr      Expression
	 * @param collectTo Collection where the atoms should be put
	 */
	public static void collectAtoms(final Expr<BoolType> expr, final Collection<Expr<BoolType>> collectTo) {
		ExprAtomCollector.collectAtoms(expr, collectTo);
	}

	/**
	 * Collect atoms from a Boolean expression.
	 *
	 * @param expr Expression
	 * @return Set of atoms
	 */
	public static Set<Expr<BoolType>> getAtoms(final Expr<BoolType> expr) {
		final Set<Expr<BoolType>> atoms = new HashSet<>();
		collectAtoms(expr, atoms);
		return atoms;
	}

	/**
	 * Check if an expression is in CNF form.
	 *
	 * @param expr Expression
	 * @return True, if the expression is in CNF
	 */
	public static boolean isExprCnf(final Expr<BoolType> expr) {
		return ExprCnfChecker.isExprCnf(expr);
	}

	/**
	 * Transform an expression into an equisatisfiable CNF form.
	 *
	 * @param expr Original expression
	 * @return Transformed expression
	 */
	public static Expr<BoolType> transformEquiSatCnf(final Expr<BoolType> expr) {
		return new ExprCnfTransformer().transformEquiSat(expr);
	}

	/**
	 * Get conjuncts of an expression.
	 *
	 * @param expr Expression
	 * @return Collection of conjuncts
	 */
	public static Collection<Expr<BoolType>> getConjuncts(final Expr<BoolType> expr) {
		checkNotNull(expr);

		if (expr instanceof AndExpr) {
			final AndExpr andExpr = (AndExpr) expr;
			return andExpr.getOps().stream().map(ExprUtils::getConjuncts).flatMap(Collection::stream)
					.collect(Collectors.toSet());
		} else {
			return Collections.singleton(expr);
		}
	}

	/**
	 * Collect variables of an expression into a given collection.
	 *
	 * @param expr      Expression
	 * @param collectTo Collection where the variables should be put
	 */
	public static void collectVars(final Expr<?> expr, final Collection<VarDecl<?>> collectTo) {
		if (expr instanceof RefExpr) {
			final RefExpr<?> refExpr = (RefExpr<?>) expr;
			final Decl<?> decl = refExpr.getDecl();
			if (decl instanceof VarDecl) {
				final VarDecl<?> varDecl = (VarDecl<?>) decl;
				collectTo.add(varDecl);
				return;
			}
		}
		expr.getOps().forEach(op -> collectVars(op, collectTo));
	}

	/**
	 * Collect variables from expressions into a given collection.
	 *
	 * @param exprs     Expressions
	 * @param collectTo Collection where the variables should be put
	 */
	public static void collectVars(final Iterable<? extends Expr<?>> exprs, final Collection<VarDecl<?>> collectTo) {
		exprs.forEach(e -> collectVars(e, collectTo));
	}

	/**
	 * Get variables of an expression.
	 *
	 * @param expr Expression
	 * @return Set of variables appearing in the expression
	 */
	public static Set<VarDecl<?>> getVars(final Expr<?> expr) {
		final Set<VarDecl<?>> vars = new HashSet<>();
		collectVars(expr, vars);
		return vars;
	}

	/**
	 * Get variables of expressions.
	 *
	 * @param exprs Expressions
	 * @return Set of variables appearing in the expressions
	 */
	public static Set<VarDecl<?>> getVars(final Iterable<? extends Expr<?>> exprs) {
		final Set<VarDecl<?>> vars = new HashSet<>();
		collectVars(exprs, vars);
		return vars;
	}

	/**
	 * Collect constants of an expression into a given collection.
	 *
	 * @param expr      Expression
	 * @param collectTo Collection where the constants should be put
	 */
	public static void collectConstants(final Expr<?> expr, final Collection<ConstDecl<?>> collectTo) {
		if (expr instanceof RefExpr) {
			final RefExpr<?> refExpr = (RefExpr<?>) expr;
			final Decl<?> decl = refExpr.getDecl();
			if (decl instanceof ConstDecl) {
				final ConstDecl<?> constDecl = (ConstDecl<?>) decl;
				collectTo.add(constDecl);
				return;
			}
		}
		expr.getOps().forEach(op -> collectConstants(op, collectTo));
	}

	/**
	 * Collect constants from expressions into a given collection.
	 *
	 * @param exprs     Expressions
	 * @param collectTo Collection where the constants should be put
	 */
	public static void collectConstants(final Iterable<? extends Expr<?>> exprs, final Collection<ConstDecl<?>> collectTo) {
		exprs.forEach(e -> collectConstants(e, collectTo));
	}

	/**
	 * Get constants of an expression.
	 *
	 * @param expr Expression
	 * @return Set of constants appearing in the expression
	 */
	public static Set<ConstDecl<?>> getConstants(final Expr<?> expr) {
		final Set<ConstDecl<?>> consts = new HashSet<>();
		collectConstants(expr, consts);
		return consts;
	}

	/**
	 * Get constants of expressions.
	 *
	 * @param exprs Expressions
	 * @return Set of constants appearing in the expressions
	 */
	public static Set<ConstDecl<?>> getConstants(final Iterable<? extends Expr<?>> exprs) {
		final Set<ConstDecl<?>> consts = new HashSet<>();
		collectConstants(exprs, consts);
		return consts;
	}

	/**
	 * Get indexed variables of an expression.
	 *
	 * @param expr Expression
	 * @return Indexed variables appearing in the expression
	 */
	public static IndexedVars getVarsIndexed(final Expr<?> expr) {
		final Builder builder = IndexedVars.builder();
		ExprIndexedVarCollector.collectIndexedVars(expr, builder);
		return builder.build();
	}

	/**
	 * Get indexed variables of expressions
	 *
	 * @param exprs Expressions
	 * @return Indexed variables appearing in the expressions
	 */
	public static IndexedVars getVarsIndexed(final Iterable<? extends Expr<?>> exprs) {
		final Builder builder = IndexedVars.builder();
		exprs.forEach(e -> ExprIndexedVarCollector.collectIndexedVars(e, builder));
		return builder.build();
	}

	/**
	 * Transform expression into an equivalent new expression without
	 * if-then-else constructs.
	 *
	 * @param expr Original expression
	 * @return Transformed expression
	 */
	public static Expr<BoolType> eliminateIte(final Expr<BoolType> expr) {
		return ExprIteEliminator.eliminateIte(expr);
	}

	/**
	 * Simplify expression and substitute the valuation.
	 *
	 * @param expr Original expression
	 * @param val  Valuation
	 * @return Simplified expression
	 */
	public static <ExprType extends Type> Expr<ExprType> simplify(final Expr<ExprType> expr, final Valuation val) {
		return ExprSimplifier.simplify(expr, val);
	}

	/**
	 * Simplify expression.
	 *
	 * @param expr Original expression
	 * @return Simplified expression
	 */
	public static <ExprType extends Type> Expr<ExprType> simplify(final Expr<ExprType> expr) {
		return simplify(expr, ImmutableValuation.empty());
	}

	/**
	 * Simplify a list of expressions.
	 *
	 * @param exprs Original expressions
	 * @return Simplified expressions
	 */
	public static List<Expr<?>> simplifyAll(final List<? extends Expr<?>> exprs) {
		final List<Expr<?>> simplifiedArgs = new ArrayList<>();
		for (final Expr<?> expr : exprs) {
			final Expr<?> simplifiedArg = simplify(expr);
			simplifiedArgs.add(simplifiedArg);
		}
		return simplifiedArgs;
	}

	/**
	 * Transform an expression into a ponated one.
	 *
	 * @param expr Original expression
	 * @return Transformed expression
	 */
	public static Expr<BoolType> ponate(final Expr<BoolType> expr) {
		if (expr instanceof NotExpr) {
			final NotExpr notExpr = (NotExpr) expr;
			return ponate(notExpr.getOp());
		} else {
			return expr;
		}
	}

	/**
	 * Transform an expression by universally quantifying certain variables.
	 *
	 * @param expr    Original expression
	 * @param mapping Quantifying
	 * @return Transformed expression
	 */
	public static <T extends Type> Expr<T> close(final Expr<T> expr, final Map<VarDecl<?>, ParamDecl<?>> mapping) {
		return ExprCloser.close(expr, mapping);
	}

	/**
	 * Transform an expression by applying primes to an expression based on an
	 * indexing.
	 *
	 * @param expr     Original expression
	 * @param indexing Indexing
	 * @return Transformed expression
	 */
	public static <T extends Type> Expr<T> applyPrimes(final Expr<T> expr, final VarIndexing indexing) {
		return ExprPrimeApplier.applyPrimes(expr, indexing);
	}

	/**
	 * Get the size of an expression by counting the nodes in its tree
	 * representation.
	 *
	 * @param expr Expression
	 * @return Node count
	 */
	public static int nodeCountSize(final Expr<?> expr) {
		return 1 + expr.getOps().stream().map(ExprUtils::nodeCountSize).reduce(0, (x, y) -> x + y);
	}

}
