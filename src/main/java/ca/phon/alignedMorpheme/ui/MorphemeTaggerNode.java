package ca.phon.alignedMorpheme.ui;

import ca.phon.util.Tuple;

import java.util.*;

class MorphemeTaggerNode {

	// index of group/word/morpheme
	private int index;

	// group/word/morpheme value
	private String morpheme;

	private Map<String, String[]> alignedMorphemeOptions;

	private List<Tuple<Character, MorphemeTaggerNode>> children = new ArrayList<>();

	public MorphemeTaggerNode(int idx) {
		this(idx, null, null);
	}

	public MorphemeTaggerNode(int idx, String token) {
		this(idx, token, null);
	}

	public MorphemeTaggerNode(int idx, String morpheme, Map<String, String[]> alignedMorphemeOptions) {
		super();
		this.index = idx;
		this.morpheme = morpheme;
		this.alignedMorphemeOptions = alignedMorphemeOptions;
	}

	public boolean isRoot() {
		return this.index < 0;
	}

	public List<Tuple<Character, MorphemeTaggerNode>> getChildren() {
		return this.children;
	}

	public boolean isTerminated() {
		return this.morpheme != null;
	}

	public String getMorpheme() {
		return this.morpheme;
	}

	public void setMorpheme(String morpheme) {
		this.morpheme = morpheme;
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

	private void printTree(MorphemeTaggerNode tree) {
		StringBuffer buffer = new StringBuffer();
		treeToString(buffer, tree);
		System.out.println(buffer.toString());
	}

	private void treeToString(StringBuffer buffer, MorphemeTaggerNode node) {
		if(node.isTerminated()) {
			buffer.append(String.format("%s\n", node.getMorpheme()));
			for(String alignedTier:node.getAlignedMorphemeOptions().keySet()) {
				String[] opts = node.alignedMorphemeOptions.get(alignedTier);
				buffer.append(String.format("\t%s:\n", alignedTier));
				buffer.append(String.format("\t\t%s\n", Arrays.toString(opts)));
			}
		} else {
			for(int cidx = 0; cidx < node.childCount(); cidx++) {
				treeToString(buffer, node.getChild(cidx));
			}
		}
	}

}
