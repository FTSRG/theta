package hu.bme.mit.theta.mcm.graph.filter;

import hu.bme.mit.theta.mcm.graph.GraphOrNodeSet;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkState;

public class Target extends Filter {
    private final Filter op;
    private Set<GraphOrNodeSet> last;

    public Target(Filter op) {
        this.op = op;
        this.last = new HashSet<>();
    }

    public Target(Stack<ForEachNode> forEachNodes, Stack<ForEachVar> forEachVars, Stack<ForEachThread> forEachThreads, Filter op, Set<GraphOrNodeSet> last) {
        this.op = op.duplicate(forEachNodes, forEachVars, forEachThreads);
        this.last = new HashSet<>();
        last.forEach(graphOrNodeSet -> this.last.add(graphOrNodeSet.duplicate()));
    }

    @Override
    public Set<GraphOrNodeSet> filterMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal) {
        Set<GraphOrNodeSet> opSet = this.op.filterMk(source, target, label, isFinal);
        return getTargets(opSet);
    }

    @Override
    public Set<GraphOrNodeSet> filterRm(MemoryAccess source, MemoryAccess target, String label) {
        Set<GraphOrNodeSet> opSet = this.op.filterRm(source, target, label);
        return getTargets(opSet);
    }

    @Override
    public Filter duplicate(Stack<ForEachNode> forEachNodes, Stack<ForEachVar> forEachVars, Stack<ForEachThread> forEachThreads) {
        return new Target(forEachNodes, forEachVars, forEachThreads, op, last);
    }

    private Set<GraphOrNodeSet> getTargets(Set<GraphOrNodeSet> opSet) {
        boolean changed = false;
        for (GraphOrNodeSet op : opSet) {
            if(op.isChanged()) {
                changed = true;
                op.setChanged(false);
            }
        }
        if(!changed) {
            return last;
        }
        Set<GraphOrNodeSet> retSet = new HashSet<>();
        for (GraphOrNodeSet op : opSet) {
            checkState(op.isGraph(), "Only graphs can have targets!");
            retSet.add(GraphOrNodeSet.of(op.getGraph().extractTargetNodes()));
        }
        return last = retSet;
    }
}
