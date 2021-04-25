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
package hu.bme.mit.theta.core.stmt.xcfa;

import hu.bme.mit.theta.core.stmt.StmtVisitor;

public interface XcfaStmtVisitor<P, R> extends StmtVisitor<P, R> {

	R visit(XcfaCallStmt stmt, P param);

	R visit(StoreStmt storeStmt, P param);

	R visit(LoadStmt loadStmt, P param);

	R visit(FenceStmt fenceStmt, P param);

	R visit(AtomicBeginStmt atomicBeginStmt, P param);

	R visit(AtomicEndStmt atomicEndStmt, P param);

}
