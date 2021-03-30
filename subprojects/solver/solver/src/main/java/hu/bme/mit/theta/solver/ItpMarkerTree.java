package hu.bme.mit.theta.solver;

import java.util.Arrays;
import java.util.List;

public final class ItpMarkerTree<T extends ItpMarker> {
    private final T marker;
    private final List<ItpMarkerTree<T>> children;

    private ItpMarkerTree(final T marker, final List<ItpMarkerTree<T>> children) {
        this.marker = marker;
        this.children = children;
    }

    public T getMarker() {
        return marker;
    }

    public List<ItpMarkerTree<T>> getChildren() {
        return children;
    }

    public ItpMarkerTree<T> getChild(int i) {
        return children.get(i);
    }

    public int getChildrenNumber() {
        return children.size();
    }

    @SafeVarargs
    public static <T extends ItpMarker> ItpMarkerTree<T> Tree(final T marker, final ItpMarkerTree<T> ...subtrees) {
        return new ItpMarkerTree<>(marker, Arrays.asList(subtrees));
    }

    @SafeVarargs
    public static <T extends ItpMarker> ItpMarkerTree<T> Subtree(final T marker, final ItpMarkerTree<T> ...subtrees) {
        return Tree(marker, subtrees);
    }

    public static <T extends ItpMarker> ItpMarkerTree<T> Leaf(final T marker) {
        return Tree(marker);
    }
}
