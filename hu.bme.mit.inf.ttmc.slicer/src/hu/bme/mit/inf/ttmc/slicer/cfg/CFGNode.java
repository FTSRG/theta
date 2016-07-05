package hu.bme.mit.inf.ttmc.slicer.cfg;

import java.util.Collection;
import java.util.HashSet;

import hu.bme.mit.inf.ttmc.slicer.graph.GraphNode;

public abstract class CFGNode implements GraphNode {

	private Collection<CFGNode> parents = new HashSet<CFGNode>();
	private Collection<CFGNode> children = new HashSet<CFGNode>();

	public void addChild(CFGNode node) {
		this.children.add(node);
		node.parents.add(this);
	}

	public void addParent(CFGNode node) {
		this.parents.add(node);
		node.children.add(this);
	}

	public void parentsReplace(CFGNode newNode)
	{
		for (CFGNode parent : this.parents) {
			parent.children.remove(this);
			newNode.addParent(parent);
		}
		this.parents.clear();
	}

	public void childrenReplace(CFGNode newNode)
	{
		for (CFGNode child : this.children) {
			child.parents.remove(this);
			newNode.addChild(child);
		}

		this.children.clear();
	}

	public void clearParents()
	{
		this.parents.clear();
	}

	public void clearChildren()
	{
		this.children.clear();
	}

	public void replace(CFGNode newNode)
	{
		this.parentsReplace(newNode);
		this.childrenReplace(newNode);
	}

	@Override
	public Collection<CFGNode> getChildren()
	{
		return this.children;
	}

	public Collection<CFGNode> getParents()
	{
		return this.children;
	}

}
