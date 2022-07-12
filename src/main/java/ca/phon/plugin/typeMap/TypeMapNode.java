package ca.phon.plugin.typeMap;

import ca.phon.util.Tuple;

import java.util.*;

class TypeMapNode {

	// index of group/word/morpheme
	private int index;

	private Map<String, String> currentAlignedMorphemes;

	private Map<String, String[]> alignedMorphemeOptions;

	private List<Tuple<Character, TypeMapNode>> children = new ArrayList<>();

	public TypeMapNode(int idx) {
		this(idx, null, null);
	}

	public TypeMapNode(int idx, Map<String, String> currentAlignedMorphemes) {
		this(idx, currentAlignedMorphemes, null);
	}

	public TypeMapNode(int idx, Map<String, String> currentAlignedMorphemes, Map<String, String[]> alignedMorphemeOptions) {
		super();
		this.index = idx;
		this.currentAlignedMorphemes = currentAlignedMorphemes;
		this.alignedMorphemeOptions = alignedMorphemeOptions;
	}

	public boolean isRoot() {
		return this.index < 0;
	}

	public List<Tuple<Character, TypeMapNode>> getChildren() {
		return this.children;
	}

	public boolean isTerminated() {
		return this.currentAlignedMorphemes != null;
	}

	public String getMorpheme(String tierName) {
		return (this.currentAlignedMorphemes == null ? null : this.currentAlignedMorphemes.get(tierName));
	}

	public void setCurrentAlignedMorphemes(Map<String, String> currentAlignedMorphemes) {
		this.currentAlignedMorphemes = currentAlignedMorphemes;
	}

	public Map<String, String[]> getAlignedMorphemeOptions() {
		return this.alignedMorphemeOptions;
	}

	public void setAlignedMorphemeOptions(Map<String, String[]> alignedMorphemeOptions) {
		this.alignedMorphemeOptions = alignedMorphemeOptions;
	}

	public int childCount() {
		return this.children.size();
	}

	public TypeMapNode getChild(int cidx) {
		return this.children.get(cidx).getObj2();
	}

	public void addChild(TypeMapNode cnode) {
		addChild('~', cnode);
	}

	public void addChild(Character ch, TypeMapNode cnode) {
		this.children.add(new Tuple<>(ch, cnode));
	}

//	private void printTree(MorphemeTaggerNode tree) {
//		StringBuffer buffer = new StringBuffer();
//		treeToString(buffer, tree);
//		System.out.println(buffer.toString());
//	}
//
//	private void treeToString(StringBuffer buffer, MorphemeTaggerNode node) {
//		if(node.isTerminated()) {
//			buffer.append(String.format("%s\n", node.getMorpheme()));
//			for(String alignedTier:node.getAlignedMorphemeOptions().keySet()) {
//				String[] opts = node.alignedMorphemeOptions.get(alignedTier);
//				buffer.append(String.format("\t%s:\n", alignedTier));
//				buffer.append(String.format("\t\t%s\n", Arrays.toString(opts)));
//			}
//		} else {
//			for(int cidx = 0; cidx < node.childCount(); cidx++) {
//				treeToString(buffer, node.getChild(cidx));
//			}
//		}
//	}

	/**
	 * Returns the number of leaves in the tree starting from this node
	 *
	 * @return number of leaves which have this node as a parent in its
	 *  hierarchy
	 */
	public int getLeafCount() {
		return countLeaves(this);
	}

	private int countLeaves(TypeMapNode node) {
		int retVal = 0;
		for(int i = 0; i < node.childCount(); i++) {
			TypeMapNode child = node.getChild(i);
			if(child.isTerminated())
				++retVal;
			else
				retVal += countLeaves(child);
		}
		return retVal;
	}

	/**
	 * Return all leaves in the tree starting from this node
	 *
	 * @return list of all leaves
	 */
	public List<TypeMapNode> getLeaves() {
		List<TypeMapNode> retVal = new ArrayList<>();
		collectLeaves(this, retVal);
		return retVal;
	}

	private void collectLeaves(TypeMapNode node, List<TypeMapNode> leaves) {
		for(int i = 0; i < node.childCount(); i++) {
			TypeMapNode child = node.getChild(i);
			if(child.isTerminated())
				leaves.add(child);
			else
				collectLeaves(child, leaves);
		}
	}

}
