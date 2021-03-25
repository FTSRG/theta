package hu.bme.mit.theta.xcfa.model;

import hu.bme.mit.theta.core.stmt.Stmt;

public class XcfaStackFrame {
    private final XcfaState owner;
    private final XcfaEdge edge;
    private Stmt stmt;
    private boolean lastStmt;
    private boolean newProcedure;

    XcfaStackFrame(XcfaState owner, XcfaEdge edge, Stmt stmt) {
        this.owner = owner;
        this.edge = edge;
        this.stmt = stmt;
        this.lastStmt = false;
        this.newProcedure = false;
    }

    public XcfaEdge getEdge() {
        return edge;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public boolean isLastStmt() {
        return lastStmt;
    }

    void setLastStmt() {
        this.lastStmt = true;
    }

    void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    XcfaStackFrame duplicate(XcfaState newOwner) {
        return new XcfaStackFrame(newOwner, edge, stmt);
    }

    public XcfaProcess getProcess() {
        return edge.getParent().getParent();
    }

    public XcfaState getOwner() {
        return owner;
    }

    public void accept() {
        owner.acceptOffer(this);
    }

    public boolean isNewProcedure() {
        return newProcedure;
    }

    public void setNewProcedure() {
        this.newProcedure = true;
    }
}