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

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutablePartitionedValuation;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.xcfa.XcfaCallStmt;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolLitExpr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkState;

/*
 * This class is used to represent the state of an XCFA program. Currently the following primitives are supported:
 *      - processes
 *      - procedures
 *      - AtomicBegin, AtomicEnd
 *      - Assignments
 * The following are *not* supported:
 *      - Mutex
 *      - CndSync
 */
public class XcfaState {

    private final XCFA xcfa;

    private final List<XcfaProcess> enabledProcesses;
    private final MutablePartitionedValuation valuation;
    private final Map<XcfaProcess, Stack<XcfaStackFrame>> stackFrames;
    private final Map<XcfaProcess, Integer> partitions;
    private final Map<XcfaProcess, Set<XcfaStackFrame>> offers;
    private XcfaProcess currentlyAtomic;

    XcfaState(XCFA xcfa) {
        this.xcfa = xcfa;
        enabledProcesses = new ArrayList<>(xcfa.getProcesses());
        valuation = new MutablePartitionedValuation();
        int globalid = valuation.createPartition();
        stackFrames = new LinkedHashMap<>();
        partitions = new LinkedHashMap<>();
        offers = new LinkedHashMap<>();
        enabledProcesses.forEach(process -> {
            stackFrames.put(process, new Stack<>());
            partitions.put(process, valuation.createPartition());
            offers.put(process, new LinkedHashSet<>());
        });
        currentlyAtomic = null;
        for (VarDecl<? extends Type> globalVar : xcfa.getGlobalVars()) {
            if (xcfa.getInitValue(globalVar).isPresent()) {
                valuation.put(globalid, globalVar, xcfa.getInitValue(globalVar).get());
            }
        }
        recalcOffers();
    }

    public List<XcfaProcess> getEnabledProcesses() {
        return enabledProcesses;
    }

    public void setEnabledProcesses(Collection<XcfaProcess> processes) {
        enabledProcesses.clear();
        enabledProcesses.addAll(processes);
    }

    public Map<XcfaProcess, Integer> getPartitions() {
        return partitions;
    }

    public Map<XcfaProcess, Stack<XcfaStackFrame>> getStackFrames() {
        return stackFrames;
    }

    public MutablePartitionedValuation getValuation() {
        return valuation;
    }

    public XcfaProcess getCurrentlyAtomic() {
        return currentlyAtomic;
    }

    public void setCurrentlyAtomic(XcfaProcess currentlyAtomic) {
        checkState(currentlyAtomic == null || enabledProcesses.contains(currentlyAtomic), "The atomic process is not enabled!");
        this.currentlyAtomic = currentlyAtomic;
    }

    public XCFA getXcfa() {
        return xcfa;
    }

    public Map<XcfaProcess, Set<XcfaStackFrame>> getOffers() {
        return offers;
    }

    public void addValuation(int id, VarDecl<?> decl, LitExpr<?> value) {
        valuation.put(id, decl, value);
    }

    public Optional<? extends LitExpr<?>> eval(VarDecl<?> decl) {
        return valuation.eval(decl);
    }

    void acceptOffer(XcfaStackFrame frame) {
        XcfaProcess proc = frame.getProcess();
        checkState(offers.getOrDefault(proc, new HashSet<>()).contains(frame), "Stack frame is not currently offered!");
        XcfaStackFrame lastFrame = null;
        if (!stackFrames.get(proc).empty()) {
            lastFrame = stackFrames.get(proc).pop();
        }
        if (frame.isNewProcedure() && lastFrame != null) {
            if (lastFrame.isLastStmt()) {
                lastFrame.setStmt(SkipStmt.getInstance());
            } else {
                int i = lastFrame.getEdge().getStmts().indexOf(lastFrame.getStmt()) + 1;
                Stmt stmt = lastFrame.getEdge().getStmts().get(i);
                lastFrame.setStmt(stmt);
                if (lastFrame.getEdge().getStmts().size() == i + 1) lastFrame.setLastStmt();
            }
            stackFrames.get(proc).push(lastFrame);
        }

        if (!(frame.isLastStmt() && frame.getEdge().getParent().getFinalLoc() == frame.getEdge().getTarget())) {
            stackFrames.get(proc).push(frame);
        } else if (stackFrames.get(proc).empty()) {
            enabledProcesses.remove(proc);
        }

        recalcOffers();
    }

    public boolean test(Map<VarDecl<?>, LitExpr<?>> expected) {
        for (Map.Entry<VarDecl<?>, LitExpr<?>> entry : expected.entrySet()) {
            VarDecl<?> varDecl = entry.getKey();
            LitExpr<?> litExpr = entry.getValue();
            Optional<? extends LitExpr<?>> eval = valuation.eval(varDecl);
            if (eval.isEmpty() || !eval.get().equals(litExpr)) return false;
        }
        return true;
    }

    private void recalcOffers() {
        offers.forEach((process, xcfaStackFrames) -> xcfaStackFrames.clear());
        if (currentlyAtomic != null) {
            collectOffers(currentlyAtomic);
        } else {
            for (XcfaProcess enabledProcess : enabledProcesses) {
                collectOffers(enabledProcess);
            }
        }

    }

    private void collectOffers(XcfaProcess enabledProcess) {
        XcfaStackFrame last = stackFrames.get(enabledProcess).empty() ? null : stackFrames.get(enabledProcess).peek();
        if (last != null && last.getStmt() instanceof XcfaCallStmt) {
            XcfaProcedure procedure = enabledProcess.getProcedures().stream().filter(xcfaProcedure -> xcfaProcedure.getName().equals(((XcfaCallStmt) last.getStmt()).getProcedure())).findFirst().orElse(null);
            checkState(procedure != null, "Procedure should not be null! Unknown procedure name?");
            collectProcedureOffers(enabledProcess, procedure);
        } else if (last == null || last.isLastStmt()) {
            XcfaLocation sourceLoc = last == null ? enabledProcess.getMainProcedure().getInitLoc() : last.getEdge().getTarget();
            for (XcfaEdge outgoingEdge : sourceLoc.getOutgoingEdges()) {
                boolean canExecute = true;
                for (Stmt stmt : outgoingEdge.getStmts()) {
                    if (stmt instanceof AssumeStmt) {
                        canExecute = ((BoolLitExpr) ((AssumeStmt) stmt).getCond().eval(valuation)).getValue();
                        break;
                    }
                }
                if (!canExecute) continue;
                XcfaStackFrame xcfaStackFrame = new XcfaStackFrame(this, outgoingEdge, outgoingEdge.getStmts().get(0));
                if (outgoingEdge.getStmts().size() == 1) xcfaStackFrame.setLastStmt();
                offers.get(enabledProcess).add(xcfaStackFrame);

            }
        } else {
            int i = last.getEdge().getStmts().indexOf(last.getStmt()) + 1;
            Stmt stmt = last.getEdge().getStmts().get(i);
            XcfaStackFrame xcfaStackFrame = last.duplicate(this);
            xcfaStackFrame.setStmt(stmt);
            if (last.getEdge().getStmts().size() == i + 1) xcfaStackFrame.setLastStmt();
            offers.get(enabledProcess).add(xcfaStackFrame);
        }
    }

    private void collectProcedureOffers(XcfaProcess enabledProcess, XcfaProcedure procedure) {
        for (XcfaEdge edge : procedure.getInitLoc().getOutgoingEdges()) {
            XcfaStackFrame xcfaStackFrame = new XcfaStackFrame(this, edge, edge.getStmts().get(0));
            if (edge.getStmts().size() == 1) xcfaStackFrame.setLastStmt();
            xcfaStackFrame.setNewProcedure();
            offers.get(enabledProcess).add(xcfaStackFrame);
        }
    }
}
