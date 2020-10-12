package hu.bme.mit.theta.mcm.graph.filter;

import hu.bme.mit.theta.mcm.graph.GraphOrNodeSet;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;

import java.util.Set;
import java.util.Stack;

public abstract class Filter {
    public abstract Set<GraphOrNodeSet> filterMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal);
    public abstract Set<GraphOrNodeSet> filterRm(MemoryAccess source, MemoryAccess target, String label);

    public abstract Filter duplicate(Stack<ForEachNode> forEachNodes, Stack<ForEachVar> forEachVars, Stack<ForEachThread> forEachThreads);
}
