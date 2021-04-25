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

package hu.bme.mit.theta.xcfa.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.LitExpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/*
 * Process subclass
 */
@SuppressWarnings("unused")
public final class XcfaProcess {
    private static final String LABEL = "process";
    private final String name;
    private final ImmutableList<VarDecl<?>> params;
    private final ImmutableMap<VarDecl<?>, Optional<LitExpr<?>>> threadLocalVars;
    private final ImmutableList<XcfaProcedure> procedures;
    private final XcfaProcedure mainProcedure;
    private XCFA parent;


    private XcfaProcess(final Builder builder) {
        params = ImmutableList.copyOf(builder.params);
        threadLocalVars = ImmutableMap.copyOf(builder.threadLocalVars);
        procedures = ImmutableList.copyOf(builder.procedures);
        procedures.forEach(procedure -> procedure.setParent(this));
        mainProcedure = builder.mainProcedure;
        name = builder.name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toDot() {
        StringBuilder ret = new StringBuilder();
        int cnt = 0;
        for (XcfaProcedure procedure : getProcedures()) {
            ret.append("subgraph cluster").append(cnt++).append("{\n");
            ret.append(procedure.toDot());
            ret.append("}\n");
        }
        return ret.toString();
    }

    public List<VarDecl<?>> getParams() {
        return params;
    }

    public List<VarDecl<?>> getThreadLocalVars() {
        return List.copyOf(threadLocalVars.keySet());
    }

    public Optional<LitExpr<?>> getInitValue(VarDecl<?> varDecl) {
        return threadLocalVars.get(varDecl);
    }

    public List<XcfaProcedure> getProcedures() {
        return procedures;
    }

    public XcfaProcedure getMainProcedure() {
        return mainProcedure;
    }

    public String getName() {
        return name;
    }

    public XCFA getParent() {
        return parent;
    }

    void setParent(XCFA xcfa) {
        this.parent = xcfa;
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder().add(LABEL).add(name).toString();
    }

    public static final class Builder {
        private final List<VarDecl<?>> params;
        private final Map<VarDecl<?>, Optional<LitExpr<?>>> threadLocalVars;
        private final List<XcfaProcedure> procedures;
        private XcfaProcedure mainProcedure;
        private String name;

        private boolean built;

        private Builder() {
            built = false;
            params = new ArrayList<>();
            threadLocalVars = new HashMap<>();
            procedures = new ArrayList<>();
        }

        private void checkNotBuilt() {
            checkState(!built, "A Process was already built.");
        }

        // params
        public List<VarDecl<?>> getParams() {
            return params;
        }

        public void createParam(final VarDecl<?> param) {
            checkNotBuilt();
            params.add(param);
        }

        // threadLocalVars
        public Map<VarDecl<?>, Optional<LitExpr<?>>> getThreadLocalVars() {
            return threadLocalVars;
        }

        public void createVar(final VarDecl<?> var, final LitExpr<?> initValue) {
            checkNotBuilt();
            threadLocalVars.put(var, Optional.ofNullable(initValue));
        }

        // procedures
        public List<XcfaProcedure> getProcedures() {
            return procedures;
        }

        public void addProcedure(final XcfaProcedure procedure) {
            checkNotBuilt();
            procedures.add(procedure);
        }

        // mainProcedure
        public XcfaProcedure getMainProcedure() {
            return mainProcedure;
        }

        public void setMainProcedure(final XcfaProcedure mainProcedure) {
            checkNotBuilt();
            checkArgument(procedures.contains(mainProcedure), "'procedures' does not contain main procedure");
            this.mainProcedure = mainProcedure;
        }

        // name

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            checkNotBuilt();
            this.name = name;
        }

        public XcfaProcess build() {
            checkNotBuilt();
            checkState(mainProcedure != null, "Main procedure must be set.");
            XcfaProcess process = new XcfaProcess(this);
            built = true;
            return process;
        }
    }
}
