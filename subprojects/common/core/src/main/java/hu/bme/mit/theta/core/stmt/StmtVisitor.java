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
package hu.bme.mit.theta.core.stmt;

import hu.bme.mit.theta.core.type.Type;

public interface StmtVisitor<P, R> {

	R visit(SkipStmt stmt, P param);

	R visit(AssumeStmt stmt, P param);

	<DeclType extends Type> R visit(AssignStmt<DeclType> stmt, P param);

	<DeclType extends Type> R visit(HavocStmt<DeclType> stmt, P param);

	R visit(XcfaStmt xcfaStmt, P param);

	R visit(SequenceStmt stmt, P param);

	R visit(NonDetStmt stmt, P param);

	R visit(OrtStmt stmt, P param);
}
