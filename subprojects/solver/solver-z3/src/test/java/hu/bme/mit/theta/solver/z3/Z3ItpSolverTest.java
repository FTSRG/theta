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
package hu.bme.mit.theta.solver.z3;

import static hu.bme.mit.theta.core.decl.Decls.Const;
import static hu.bme.mit.theta.core.decl.Decls.Param;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Forall;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Imply;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;
import static hu.bme.mit.theta.core.type.functype.FuncExprs.App;
import static hu.bme.mit.theta.core.type.functype.FuncExprs.Func;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Add;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Eq;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Mul;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Neq;
import static hu.bme.mit.theta.solver.ItpMarkerTree.Leaf;
import static hu.bme.mit.theta.solver.ItpMarkerTree.Subtree;
import static hu.bme.mit.theta.solver.ItpMarkerTree.Tree;

import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.solver.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.decl.ParamDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.functype.FuncType;
import hu.bme.mit.theta.core.type.inttype.IntType;

public final class Z3ItpSolverTest {

	ItpSolver solver;

	Expr<IntType> a;
	Expr<IntType> b;
	Expr<IntType> c;
	Expr<IntType> d;
	Expr<IntType> e;
	Expr<FuncType<IntType, IntType>> f;
	Expr<FuncType<IntType, IntType>> g;

	@Before
	public void initialize() {
		solver = Z3SolverFactory.getInstance().createItpSolver();

		final ConstDecl<IntType> ad = Const("a", Int());
		final ConstDecl<IntType> bd = Const("b", Int());
		final ConstDecl<IntType> cd = Const("c", Int());
		final ConstDecl<IntType> dd = Const("d", Int());
		final ConstDecl<IntType> ed = Const("e", Int());
		final ConstDecl<FuncType<IntType, IntType>> fd = Const("f", Func(Int(), Int()));
		final ConstDecl<FuncType<IntType, IntType>> gd = Const("g", Func(Int(), Int()));

		a = ad.getRef();
		b = bd.getRef();
		c = cd.getRef();
		d = dd.getRef();
		e = ed.getRef();
		f = fd.getRef();
		g = gd.getRef();
	}

	@Test
	public void testBinaryInterpolation() {
		final ItpMarker A = solver.createMarker();
		final ItpMarker B = solver.createMarker();
		final ItpPattern pattern = solver.createBinPattern(A, B);

		solver.add(A, Eq(a, b));
		solver.add(A, Eq(a, c));
		solver.add(B, Eq(b, d));
		solver.add(B, Neq(c, d));

		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(A));
		System.out.println("----------");
		Assert.assertTrue(ExprUtils.getVars(itp.eval(A)).size() <= 3);
	}

	@Test
	public void testSequenceInterpolation() {
		final ItpMarker I1 = solver.createMarker();
		final ItpMarker I2 = solver.createMarker();
		final ItpMarker I3 = solver.createMarker();
		final ItpMarker I4 = solver.createMarker();
		final ItpMarker I5 = solver.createMarker();
		final ItpPattern pattern = solver.createSeqPattern(ImmutableList.of(I1, I2, I3, I4, I5));

		solver.add(I1, Eq(a, Int(0)));
		solver.add(I2, Eq(a, b));
		solver.add(I3, Eq(c, d));
		solver.add(I4, Eq(d, Int(1)));
		solver.add(I5, Eq(b, c));

		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(I1));
		System.out.println(itp.eval(I2));
		System.out.println(itp.eval(I3));
		System.out.println(itp.eval(I4));
		System.out.println(itp.eval(I5));
		System.out.println("----------");
	}

	@Test
	public void testTreeInterpolation() {
		final ItpMarker I1 = solver.createMarker();
		final ItpMarker I2 = solver.createMarker();
		final ItpMarker I3 = solver.createMarker();
		final ItpMarker I4 = solver.createMarker();
		final ItpMarker I5 = solver.createMarker();
		final ItpPattern pattern = solver.createTreePattern(Tree(I3, Subtree(I1, Leaf(I4), Leaf(I5)), Leaf(I2)));

		solver.add(I1, Eq(a, Int(0)));
		solver.add(I2, Eq(a, b));
		solver.add(I3, Eq(c, d));
		solver.add(I4, Eq(d, Int(1)));
		solver.add(I5, Eq(b, c));

		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(I1));
		System.out.println(itp.eval(I2));
		System.out.println(itp.eval(I3));
		System.out.println(itp.eval(I4));
		System.out.println(itp.eval(I5));
		System.out.println("----------");
	}

	@Test
	public void testEUF() {
		final ItpMarker A = solver.createMarker();
		final ItpMarker B = solver.createMarker();
		final ItpPattern pattern = solver.createBinPattern(A, B);

		solver.add(A, Eq(App(f, a), c));
		solver.add(A, Eq(App(f, b), d));
		solver.add(B, Eq(a, b));
		solver.add(B, Neq(App(g, c), App(g, d)));

		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(A));
		System.out.println("----------");
	}

	@Test
	public void testLIA() {
		final ItpMarker A = solver.createMarker();
		final ItpMarker B = solver.createMarker();
		final ItpPattern pattern = solver.createBinPattern(A, B);

		solver.add(A, Eq(b, Mul(ImmutableList.of(Int(2), a))));
		solver.add(B, Eq(b, Add(ImmutableList.of(Mul(ImmutableList.of(Int(2), c)), Int(1)))));

		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(A));
		System.out.println("----------");
	}

	@Test
	public void testQuantifiers() {
		final ItpMarker A = solver.createMarker();
		final ItpMarker B = solver.createMarker();
		final ItpPattern pattern = solver.createBinPattern(A, B);

		final ConstDecl<IntType> id = Const("i", Int());
		final ConstDecl<FuncType<IntType, BoolType>> pd = Const("p", Func(Int(), Bool()));
		final ConstDecl<FuncType<IntType, BoolType>> qd = Const("q", Func(Int(), Bool()));
		final ParamDecl<IntType> x1d = Param("x", Int());
		final ParamDecl<IntType> x2d = Param("x", Int());

		final Expr<IntType> i = id.getRef();
		final Expr<FuncType<IntType, BoolType>> p = pd.getRef();
		final Expr<FuncType<IntType, BoolType>> q = qd.getRef();
		final Expr<IntType> x1 = x1d.getRef();
		final Expr<IntType> x2 = x2d.getRef();

		solver.add(A, Forall(ImmutableList.of(x1d), Imply(App(q, x1), App(p, x1))));
		solver.add(A, Forall(ImmutableList.of(x2d), Not(App(p, x2))));
		solver.add(B, App(q, i));

		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(A));
		System.out.println("----------");
	}

	@Test
	public void testPushPop() {
		final ItpMarker A = solver.createMarker();
		final ItpMarker B = solver.createMarker();
		final ItpPattern pattern = solver.createBinPattern(A, B);

		solver.add(A, Eq(a, b));
		solver.add(B, Eq(b, c));

		solver.push();

		solver.add(A, Neq(a, c));
		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());

		solver.pop();

		solver.add(B, Neq(a, c));
		solver.check();
		Assert.assertEquals(SolverStatus.UNSAT, solver.getStatus());
		final Interpolant itp = solver.getInterpolant(pattern);

		System.out.println(itp.eval(A));
		System.out.println("----------");
	}

}
