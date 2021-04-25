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

package hu.bme.mit.theta.core.utils;

import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.stmt.XcfaStmt;
import hu.bme.mit.theta.core.type.Type;

public class StmtCounterVisitor implements StmtVisitor<Void, Integer> {

	private static final class LazyHolder {
		private final static StmtCounterVisitor INSTANCE = new StmtCounterVisitor();
	}

	private StmtCounterVisitor() {
	}

	public static StmtCounterVisitor getInstance() {
		return StmtCounterVisitor.LazyHolder.INSTANCE;
	}

	@Override
	public Integer visit(SkipStmt stmt, Void param) {
		return 1;
	}

	@Override
	public Integer visit(AssumeStmt stmt, Void param) {
		return 1;
	}

	@Override
	public <DeclType extends Type> Integer visit(AssignStmt<DeclType> stmt, Void param) {
		return 1;
	}

	@Override
	public <DeclType extends Type> Integer visit(HavocStmt<DeclType> stmt, Void param) {
		return 1;
	}

	@Override
	public Integer visit(XcfaStmt stmt, Void param) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Integer visit(SequenceStmt stmt, Void param) {
		int count = 0;
		for (var subStmt : stmt.getStmts()) {
			count += subStmt.accept(this, null);
		}
		return count + 1;
	}

	@Override
	public Integer visit(NonDetStmt stmt, Void param) {
		int count = 0;
		for (var subStmt: stmt.getStmts()){
			count+=subStmt.accept(this,null);
		}
		return count+1;
	}

	@Override
	public Integer visit(OrtStmt stmt, Void param) {
		int count = 0;
		for (var subStmt: stmt.getStmts()){
			count+=subStmt.accept(this,null);
		}
		return count+1;
	}
}
