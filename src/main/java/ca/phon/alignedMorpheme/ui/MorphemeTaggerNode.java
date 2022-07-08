package ca.phon.alignedMorpheme.ui;

import ca.phon.util.Tuple;
import com.teamdev.jxbrowser.chromium.be;

import java.util.*;

class MorphemeTaggerNode {

	// index of group/word/morpheme
	private int index;

	private Map<String, String> currentAlignedMorphemes;

	private Map<String, String[]> alignedMorphemeOptions;

	private List<Tuple<Character, MorphemeTaggerNode>> children = new ArrayList<>();

	public MorphemeTaggerNode(int idx) {
		this(idx, null, null);
	}

	public MorphemeTaggerNode(int idx, Map<String, String> currentAlignedMorphemes) {
		this(idx, currentAlignedMorphemes, null);
	}

	public MorphemeTaggerNode(int idx, Map<String, String> currentAlignedMorphemes, Map<String, String[]> alignedMorphemeOptions) {
		super();
		this.index = idx;
		this.currentAlignedMorphemes = currentAlignedMorphemes;
		this.alignedMorphemeOptions = alignedMorphemeOptions;
	}

	public boolean isRoot() {
		return this.index < 0;
	}

	public List<Tuple<Character, MorphemeTaggerNode>> getChildren() {
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

	public MorphemeTaggerNode getChild(int cidx) {
		return this.children.get(cidx).getObj2();
	}

	public void addChild(MorphemeTaggerNode cnode) {
		addChild('~', cnode);
	}

	public void addChild(Character ch, MorphemeTaggerNode cnode) {
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

	private int countLeaves(MorphemeTaggerNode node) {
		int retVal = 0;
		for(int i = 0; i < node.childCount(); i++) {
			MorphemeTaggerNode child = node.getChild(i);
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
	public List<MorphemeTaggerNode> getLeaves() {
		List<MorphemeTaggerNode> retVal = new ArrayList<>();
		collectLeaves(this, retVal);
		return retVal;
	}

	private void collectLeaves(MorphemeTaggerNode node, List<MorphemeTaggerNode> leaves) {
		for(int i = 0; i < node.childCount(); i++) {
			MorphemeTaggerNode child = node.getChild(i);
			if(child.isTerminated())
				leaves.add(child);
			else
				collectLeaves(child, leaves);
		}
	}

}
