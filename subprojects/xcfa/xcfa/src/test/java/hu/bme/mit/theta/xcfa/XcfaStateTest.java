/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.bme.mit.theta.xcfa;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicBeginStmt;
import hu.bme.mit.theta.core.stmt.xcfa.AtomicEndStmt;
import hu.bme.mit.theta.core.stmt.xcfa.FenceStmt;
import hu.bme.mit.theta.core.stmt.xcfa.LoadStmt;
import hu.bme.mit.theta.core.stmt.xcfa.StoreStmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaCallStmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaStmtVisitor;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.xcfa.dsl.XcfaDslManager;
import hu.bme.mit.theta.xcfa.model.XCFA;
import hu.bme.mit.theta.xcfa.model.XcfaStackFrame;
import hu.bme.mit.theta.xcfa.model.XcfaState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public final class XcfaStateTest {

	@Parameter(0)
	public XCFA xcfa;

	@Parameter(1)
	public Map<VarDecl<?>, LitExpr<?>> expectedResult;

	@Parameter(2)
	public boolean required;

	@Parameters()
	public static Collection<Object[]> data() throws IOException {
		List<Object[]> testCases = new ArrayList<>();
		XCFA xcfa;
		Map<VarDecl<?>, LitExpr<?>> valuation;

		xcfa = getXcfa("/simple.xcfa");
		valuation = new LinkedHashMap<>();
		valuation.put(xcfa.getMainProcess().getMainProcedure().getLocalVars().get(0), IntLitExpr.of(new BigInteger("42")));
		testCases.add(new Object[]{xcfa, valuation, true});

		xcfa = getXcfa("/simpleprocedure.xcfa");
		valuation = new LinkedHashMap<>();
		valuation.put(xcfa.getMainProcess().getThreadLocalVars().get(0), IntLitExpr.of(new BigInteger("42")));
		testCases.add(new Object[]{xcfa, valuation, true});

		xcfa = getXcfa("/multithread.xcfa");
		valuation = new LinkedHashMap<>();
		valuation.put(xcfa.getProcesses().get(0).getMainProcedure().getLocalVars().get(0), IntLitExpr.of(new BigInteger("21")));
		valuation.put(xcfa.getProcesses().get(1).getMainProcedure().getLocalVars().get(0), IntLitExpr.of(new BigInteger("42")));
		testCases.add(new Object[]{xcfa, valuation, true});

		xcfa = getXcfa("/multithread_atomic.xcfa");
		valuation = new LinkedHashMap<>();
		valuation.put(xcfa.getProcesses().get(0).getMainProcedure().getLocalVars().get(0), IntLitExpr.of(new BigInteger("21")));
		valuation.put(xcfa.getProcesses().get(1).getMainProcedure().getLocalVars().get(0), IntLitExpr.of(new BigInteger("42")));
		testCases.add(new Object[]{xcfa, valuation, true});

		return testCases;
	}

	private static XCFA getXcfa(String filepath) throws IOException {
		final InputStream inputStream = XcfaStateTest.class.getResourceAsStream(filepath);
		XCFA xcfa = XcfaDslManager.createXcfa(inputStream);
		inputStream.close();
		return xcfa;
	}

	@Test
	public void test() {
		XcfaState state = xcfa.getInitialState();
		StmtVisitor visitor = new StmtVisitor();
		int lastProcId = 0;
		outerloop:
		while (true) {
			int i = lastProcId;
			do {
				i = (i + 1) % xcfa.getProcesses().size();
				Set<XcfaStackFrame> xcfaStackFrames = state.getOffers().get(xcfa.getProcesses().get(i));
				for (XcfaStackFrame xcfaStackFrame : xcfaStackFrames) {
					xcfaStackFrame.getStmt().accept(visitor, xcfaStackFrame);
					xcfaStackFrame.accept();
					lastProcId = i;
					continue outerloop;
				}
			} while (i != lastProcId);
			break;
		}
		assertEquals(state.test(expectedResult), required);
	}

	private static class StmtVisitor implements hu.bme.mit.theta.core.stmt.StmtVisitor<XcfaStackFrame, Void>, XcfaStmtVisitor<XcfaStackFrame, Void> {

		@Override
		public Void visit(XcfaCallStmt stmt, XcfaStackFrame param) {
//			System.out.println("Visiting XcfaCallStmt");
			return null;
		}

		@Override
		public Void visit(StoreStmt storeStmt, XcfaStackFrame param) {
//			System.out.println("Visiting StoreStmt");
			return null;
		}

		@Override
		public Void visit(LoadStmt loadStmt, XcfaStackFrame param) {
//			System.out.println("Visiting LoadStmt");
			return null;
		}

		@Override
		public Void visit(FenceStmt fenceStmt, XcfaStackFrame param) {
//			System.out.println("Visiting FenceStmt");
			return null;
		}

		@Override
		public Void visit(AtomicBeginStmt atomicBeginStmt, XcfaStackFrame param) {
//			System.out.println("Visiting AtomicBeginStmt");
			param.getOwner().setCurrentlyAtomic(param.getProcess());
			return null;
		}

		@Override
		public Void visit(AtomicEndStmt atomicEndStmt, XcfaStackFrame param) {
//			System.out.println("Visiting AtomicEndStmt");
			param.getOwner().setCurrentlyAtomic(null);
			return null;
		}



		@Override
		public Void visit(SkipStmt stmt, XcfaStackFrame param) {
//			System.out.println("Visiting SkipStmt");
			return null;
		}

		@Override
		public Void visit(AssumeStmt stmt, XcfaStackFrame param) {
//			System.out.println("Visiting AssumeStmt");
			return null;
		}

		@Override
		public <DeclType extends Type> Void visit(AssignStmt<DeclType> stmt, XcfaStackFrame param) {
//			System.out.println("Visiting AssignStmt");
			XcfaState owner = param.getOwner();
			int id = owner.getPartitions().get(param.getProcess());
			VarDecl<?> decl = stmt.getVarDecl();
			LitExpr<?> expr = stmt.getExpr().eval(owner.getValuation());
			owner.addValuation(id, decl, expr);
			return null;
		}

		@Override
		public <DeclType extends Type> Void visit(HavocStmt<DeclType> stmt, XcfaStackFrame param) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public Void visit(XcfaStmt xcfaStmt, XcfaStackFrame param) {
			return xcfaStmt.accept(this, param);
		}

		@Override
		public Void visit(SequenceStmt stmt, XcfaStackFrame param) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public Void visit(NonDetStmt stmt, XcfaStackFrame param) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public Void visit(OrtStmt stmt, XcfaStackFrame param) {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

}
