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

package hu.bme.mit.theta.xcfa.ir.handlers.states;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.common.Tuple4;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.xcfa.ir.handlers.utils.PlaceholderAssignmentStmt;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaMetadata;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static hu.bme.mit.theta.xcfa.ir.Utils.createType;
import static hu.bme.mit.theta.xcfa.ir.Utils.createVariable;

public class FunctionState {
    private final GlobalState globalState;
    private final Tuple3<String, Optional<String>, List<Tuple2<String, String>>> function;
    private final XcfaProcedure.Builder procedureBuilder;
    private final Map<String, Tuple2<VarDecl<?>, Integer>> localVars;
    private final Set<VarDecl<?>> params;
    private final Map<String, Expr<?>> values;
    private final Map<String, XcfaLocation> locations;
    private final Map<Tuple2<String, String>, Tuple4<XcfaLocation, XcfaLocation, List<Stmt>, Integer>> interBlockEdges;

    public FunctionState(GlobalState globalState, Tuple3<String, Optional<String>, List<Tuple2<String, String>>> function) {
        this.globalState = globalState;
        this.function = function;
        procedureBuilder = XcfaProcedure.builder();
        procedureBuilder.setName(function.get1());
        localVars = new HashMap<>();
        params = new HashSet<>();
        values = new HashMap<>();
        interBlockEdges = new HashMap<>();

        globalState.getGlobalVars().forEach((s, varDecl) -> localVars.put(s, Tuple2.of(varDecl, 1)));

        // Adding params
        for (Tuple2<String, String> param : function.get3()) {
            VarDecl<?> var = createVariable(param.get2(), param.get1());
            procedureBuilder.createParam(var);
            localVars.put(param.get2(), Tuple2.of(var, 1));
            params.add(var);
        }

        // Adding final location
        XcfaLocation finalLoc = new XcfaLocation(function.get1() + "_final", new HashMap<>());
        procedureBuilder.addLoc(finalLoc);
        procedureBuilder.setFinalLoc(finalLoc);

        // Adding return variable
        if (function.get2().isPresent()) {
            VarDecl<?> retVar = createVariable(function.get1() + "_ret", function.get2().get());
            procedureBuilder.setRtype(createType(function.get2().get()));
            procedureBuilder.setResult(retVar);
        }

        // Adding blocks and first location
        List<String> blocks = globalState.getProvider().getBlocks(function.get1());
        locations = new LinkedHashMap<>();
        boolean first = true;
        for (String block : blocks) {
            XcfaLocation loc = new XcfaLocation(block, new HashMap<>());
            locations.put(block, loc);
            procedureBuilder.addLoc(loc);
            if (first) {
                procedureBuilder.setInitLoc(loc);
                first = false;
            }
        }

        localVars.forEach((s, var) -> values.put(s, var.get1().getRef()));

    }

    public void finalizeFunctionState(BuiltState builtState) {
        interBlockEdges.forEach((_obj, edgeTup) -> {
            List<Stmt> stmts = edgeTup.get3().stream().map(stmt -> {
                if (stmt instanceof PlaceholderAssignmentStmt) {
                    return ((PlaceholderAssignmentStmt<?>) stmt).toAssignStmt(getValues());
                }
                return stmt;
            }).collect(Collectors.toUnmodifiableList());
            XcfaEdge edge = new XcfaEdge(edgeTup.get1(), edgeTup.get2(), stmts);
            if(edgeTup.get4() >= 0) XcfaMetadata.create(edge, "lineNumber", edgeTup.get4());
            procedureBuilder.addEdge(edge);
        });
    }

    public GlobalState getGlobalState() {
        return globalState;
    }

    public Tuple3<String, Optional<String>, List<Tuple2<String, String>>> getFunction() {
        return function;
    }

    public XcfaProcedure.Builder getProcedureBuilder() {
        return procedureBuilder;
    }

    public Map<String, Expr<?>> getValues() {
        return values;
    }

    public Map<String, Tuple2<VarDecl<?>, Integer>> getLocalVars() {
        return localVars;
    }

    public Map<String, XcfaLocation> getLocations() {
        return locations;
    }

    public Map<Tuple2<String, String>, Tuple4<XcfaLocation, XcfaLocation, List<Stmt>, Integer>> getInterBlockEdges() {
        return interBlockEdges;
    }

    public Set<VarDecl<?>> getParams() {
        return params;
    }
}

