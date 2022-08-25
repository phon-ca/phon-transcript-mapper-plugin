/*
 * Copyright (C) 2005-2022 Gregory Hedlund & Yvan Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.phon.transcriptMapper;

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
