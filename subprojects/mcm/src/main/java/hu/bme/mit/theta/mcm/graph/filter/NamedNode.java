package hu.bme.mit.theta.mcm.graph.filter;

import hu.bme.mit.theta.mcm.graph.Graph;
import hu.bme.mit.theta.mcm.graph.GraphOrNodeSet;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class NamedNode extends Filter {
    private final Class<?> clazz;
    private final Graph graph;
    private final GraphOrNodeSet set;

    public NamedNode(Class<?> clazz) {
        this.clazz = clazz;
        set = GraphOrNodeSet.of(new HashSet<>());
        graph = Graph.empty();
    }

    public NamedNode(Class<?> clazz, Graph graph, GraphOrNodeSet set) {
        this.clazz = clazz;
        this.graph = graph.duplicate();
        this.set = set.duplicate();
    }


    @Override
    public Set<GraphOrNodeSet> filterMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal) {
        graph.addEdge(source, target, isFinal);
        if((clazz == null || clazz.isAssignableFrom(source.getClass())) && !set.getNodeSet().contains(source)) {
            set.getNodeSet().add(source);
            set.setChanged(true);
        }
        if((clazz == null || clazz.isAssignableFrom(target.getClass())) && !set.getNodeSet().contains(target)) {
            set.getNodeSet().add(target);
            set.setChanged(true);
        }
        return Set.of(set);
    }

    @Override
    public Set<GraphOrNodeSet> filterRm(MemoryAccess source, MemoryAccess target, String label) {
        graph.removeEdge(source, target);
        if(graph.isDisconnected(source)) {
            set.getNodeSet().remove(source);
            set.setChanged(true);
        }
        if(graph.isDisconnected(target)) {
            set.getNodeSet().remove(target);
            set.setChanged(true);
        }
        return Set.of(set);
    }

    @Override
    public Filter duplicate(Stack<ForEachNode> forEachNodes, Stack<ForEachVar> forEachVars, Stack<ForEachThread> forEachThreads) {
        return new NamedNode(clazz, graph, set);
    }
}
