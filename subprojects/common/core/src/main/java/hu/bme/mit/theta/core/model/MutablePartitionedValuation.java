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
package hu.bme.mit.theta.core.model;

import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.SmartBoolExprs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;

/**
 * Mutable implementation of a partitioned valuation.
 */
public final class MutablePartitionedValuation extends Valuation {

	private final List<Map<Decl<?>, LitExpr<?>>> partitions;

	public MutablePartitionedValuation() {
		this.partitions = new ArrayList<>();
	}

	public static MutablePartitionedValuation copyOf(final MutablePartitionedValuation val) {
		final MutablePartitionedValuation result = new MutablePartitionedValuation();
		for (int i = 0; i < val.getPartitions().size(); ++i) {
			int id = result.createPartition();
			val.getPartitions().get(i).forEach((decl, litExpr) -> result.put(id, decl, litExpr));
		}
		return result;
	}

	public int createPartition() {
		partitions.add(new LinkedHashMap<>());
		return partitions.size() - 1;
	}


	public List<Map<Decl<?>, LitExpr<?>>> getPartitions() {
		return Collections.unmodifiableList(partitions);
	}

	public MutablePartitionedValuation put(final int id, final Decl<?> decl, final LitExpr<?> value) {
		checkArgument(value.getType().equals(decl.getType()), "Type mismatch.");
		checkArgument(partitions.size() > id, "Index out of bounds");
		partitions.get(id).put(decl, value);
		return this;
	}

	public MutablePartitionedValuation remove(final int id, final Decl<?> decl) {
		checkArgument(partitions.size() > id, "Index out of bounds");
		partitions.get(id).remove(decl);
		return this;
	}

	public MutablePartitionedValuation remove(final Decl<?> decl) {
		for (Map<Decl<?>, LitExpr<?>> decls : partitions) {
			decls.remove(decl);
		}
		return this;
	}

	public MutablePartitionedValuation clear() {
		partitions.clear();
		return this;
	}

	public MutablePartitionedValuation clear(final int id) {
		checkArgument(partitions.size() > id, "Index out of bounds");
		partitions.get(id).clear();
		return this;
	}

	public MutablePartitionedValuation putAll(final int id, final Valuation val) {
		checkArgument(partitions.size() > id, "Index out of bounds");
		for (final Decl<?> decl : val.getDecls()) {
			partitions.get(id).put(decl, val.eval(decl).get());
		}
		return this;
	}

	@Override
	public Collection<Decl<?>> getDecls() {
		Set<Decl<?>> returnSet = new HashSet<>();
		for (Map<Decl<?>, LitExpr<?>> decls : partitions) {
			returnSet.addAll(decls.keySet());
		}
		return Collections.unmodifiableSet(returnSet);
	}

	public Collection<Decl<?>> getDecls(final int id) {
		checkArgument(partitions.size() > id, "Index out of bounds");
		return Collections.unmodifiableSet(partitions.get(id).keySet());
	}

	public Valuation getValuation(final int id) {
		checkArgument(partitions.size() > id, "Index out of bounds");
		MutableValuation mutableValuation = new MutableValuation();
		partitions.forEach(declLitExprMap -> declLitExprMap.forEach(mutableValuation::put));
		return mutableValuation;
	}

	@Override
	public <DeclType extends Type> Optional<LitExpr<DeclType>> eval(final Decl<DeclType> decl) {
		checkNotNull(decl);
		for (Map<Decl<?>, LitExpr<?>> decls : partitions) {
			@SuppressWarnings("unchecked") final LitExpr<DeclType> val = (LitExpr<DeclType>) decls.get(decl);
			if (val != null) return Optional.of(val);
		}
		return Optional.empty();
	}

	@Override
	public Expr<BoolType> toExpr() {
		return SmartBoolExprs.And(getDecls().stream().map(e -> Eq(e.getRef(), eval(e).get()))
				.collect(toImmutableList()));
	}

	@Override
	public Map<Decl<?>, LitExpr<?>> toMap() {
		Map<Decl<?>, LitExpr<?>> ret = new HashMap<>();
		for (Map<Decl<?>, LitExpr<?>> decl : partitions) {
			ret.putAll(decl);
		}
		return Collections.unmodifiableMap(ret);
	}

}
