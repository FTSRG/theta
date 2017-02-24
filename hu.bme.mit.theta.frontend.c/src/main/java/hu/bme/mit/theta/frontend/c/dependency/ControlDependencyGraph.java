package hu.bme.mit.theta.frontend.c.dependency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import hu.bme.mit.theta.frontend.c.ir.BasicBlock;
import hu.bme.mit.theta.frontend.c.ir.Function;
import hu.bme.mit.theta.frontend.c.ir.utils.CfgEdge;

public class ControlDependencyGraph {

	private final CDGNode entry;
	private final Map<BasicBlock, CDGNode> nodes;

	public static class CDGNode {
		public final BasicBlock block;
		public final List<CdgEdge> childEdges = new ArrayList<>();
		public final List<CdgEdge> parentEdges = new ArrayList<>();

		public CDGNode(final BasicBlock block) {
			this.block = block;
		}

		@Override
		public String toString() {
			return this.block.toString();
		}
	}

	public static class CdgEdge {
		private final CDGNode source;
		private final CDGNode target;

		private final CfgEdge dependingEdge;

		public CdgEdge(final CDGNode source, final CDGNode target, final CfgEdge parentEdge) {
			this.source = source;
			this.target = target;
			this.dependingEdge = parentEdge;
		}

		public CDGNode getSource() {
			return this.source;
		}

		public CDGNode getTarget() {
			return this.target;
		}

		public CfgEdge getDependingEdge() {
			return this.dependingEdge;
		}
	}

	private ControlDependencyGraph(final Map<BasicBlock, CDGNode> nodes, final CDGNode entry) {
		this.nodes = nodes;
		this.entry = entry;
	}

	public Collection<CDGNode> getNodes() {
		return Collections.unmodifiableCollection(this.nodes.values());
	}

	public List<BasicBlock> getParentBlocks(final BasicBlock block) {
		return this.nodes.get(block).parentEdges.stream().map(p -> p.source.block).collect(Collectors.toList());
	}

	public Multimap<CDGNode, CfgEdge> cdgPredecessors(final CDGNode node) {
		final Queue<CdgEdge> wl = new ArrayDeque<>();
		final Multimap<CDGNode, CfgEdge> pred = ArrayListMultimap.create();

		for (final CdgEdge edge : node.parentEdges) {
			wl.add(edge);
		}

		while (!wl.isEmpty()) {
			final CdgEdge current = wl.poll();
			pred.put(current.source, current.dependingEdge);

			for (final CdgEdge parentEdge : current.source.parentEdges) {
				if (!pred.containsEntry(parentEdge.source, parentEdge.dependingEdge)) {
					wl.add(parentEdge);
				}
			}
		}

		return pred;
	}

	public List<BasicBlock> predecessors(final BasicBlock block) {
		final CDGNode node = this.getNode(block);

		return this.cdgPredecessors(node).keySet().stream().map(cdg -> cdg.block).collect(Collectors.toList());
	}

	public static ControlDependencyGraph buildGraph(final Function function) {
		final List<BasicBlock> blocks = function.getBlocksDFS();
		final DominatorTree pdt = DominatorTree.createPostDominatorTree(function);

		/*
		 * Control dependence algorithm from J. Ferrante et al.
		 *
		 * Given the post-dominator tree, we can determine control dependences
		 * by examining certain control flow graph edges and annotating nodes on
		 * corresponding tree paths. Let S consist of all edges (A, B) in the
		 * control flow graph such that B is not an ancestor of A in the
		 * post-dominator tree (i.e., B does not post- dominate A).
		 *
		 * The control dependence determination algorithm proceeds by examining
		 * each edge (A, B) in S. Let L denote the least common ancestor of A
		 * and B in the post-dominator tree. By construction, we cannot have L
		 * equal B.
		 *
		 * Case 1. L = parent of A. All nodes in the post-dominator tree on the
		 * path from L to B, including B but not L, should be made control
		 * dependent on A.
		 *
		 * Case 2. L = A. All nodes in the post-dominator tree on the path from
		 * A to B, including A and B, should be made control dependent on A.
		 * This case captures loop dependence.)
		 *
		 * It should be clear that, given (A, B), the desired effect will be
		 * achieved by traversing backwards from B in the post-dominator tree
		 * until we reach A’s parent (if it exists), marking all nodes visited
		 * before A’s parent as control dependent on A.
		 */
		final Map<BasicBlock, Multimap<CfgEdge, BasicBlock>> controlDeps = new HashMap<>();
		blocks.forEach(b -> controlDeps.put(b, ArrayListMultimap.create()));

		for (final BasicBlock block : blocks) {
			// Get the block's (A's) parent
			final BasicBlock blockIdom = pdt.getParent(block);

			final Multimap<CfgEdge, BasicBlock> dependants = controlDeps.get(block);

			// Get all block -> child (A -> B) edges
			for (final BasicBlock child : block.children()) {
				if (!pdt.dominates(child, block)) { // B must not dominate A
					final CfgEdge edge = new CfgEdge(block, child);

					BasicBlock parent = child;
					while (parent != block && parent != blockIdom) {
						if (parent == null)
							break;

						// dependants.add(parent);
						dependants.put(edge, parent);
						parent = pdt.getParent(parent);
					}
				}
			}
		}

		/*
		 * After finding the control dependency relation, we can build the
		 * control dependency graph
		 */
		final Map<BasicBlock, CDGNode> nodes = new HashMap<>();
		for (final BasicBlock block : blocks) {
			nodes.put(block, new CDGNode(block));
		}

		final CDGNode entry = nodes.get(function.getEntryBlock());
		controlDeps.forEach((final BasicBlock block, final Multimap<CfgEdge, BasicBlock> deps) -> {
			final CDGNode cdg = nodes.get(block);
			deps.asMap().forEach((edge, dependants) -> {
				dependants.forEach(d -> {
					final CDGNode depCdg = nodes.get(d);
					final CdgEdge cdgEdge = new CdgEdge(cdg, depCdg, edge);

					cdg.childEdges.add(cdgEdge);
					depCdg.parentEdges.add(cdgEdge);
				});
			});

		});

		// nodes.values().stream().filter(n -> n.parentEdges.size() == 0 && n !=
		// entry).forEach(n -> {
		// CdgEdge edge = new CdgEdge(entry, n, null);
		// n.parentEdges.add(edge);
		// entry.childEdges.add(edge);
		// });

		return new ControlDependencyGraph(nodes, entry);
	}

	public CDGNode getNode(final BasicBlock block) {
		final CDGNode node = this.nodes.get(block);
		if (node == null)
			throw new RuntimeException("Cannot find block " + block.getName() + " in the control dependency graph.");

		return node;
	}

}
