package ca.phon.alignedType;

import ca.hedlund.tst.*;

import java.io.*;
import java.util.*;

class TypeLinkedEntry implements Serializable {

	private static final long serialVersionUID = -5323402869852524374L;

	private transient TernaryTreeNode<TierInfo> tierNameRef;

	private transient TernaryTreeNodePath tierNamePath;

	private transient Set<TernaryTreeNode<Collection<TypeEntry>>> linkedTierRefs;

	private transient Collection<TernaryTreeNodePath> linkedNodePaths;

	public TypeLinkedEntry(TernaryTreeNode<TierInfo> tierNameRef) {
		this(tierNameRef, new LinkedHashSet<>());
	}

	public TypeLinkedEntry(TernaryTreeNode<TierInfo> tierNameRef,
	                       Set<TernaryTreeNode<Collection<TypeEntry>>> linkedTierRefs) {
		super();

		this.tierNameRef = tierNameRef;
		this.linkedTierRefs = linkedTierRefs;
	}

	public String getTierName(TernaryTree<TierInfo> tierDescriptionTree) {
		if(this.tierNameRef == null) {
			if(this.tierNamePath != null) {
				Optional<TernaryTreeNode<TierInfo>> tierInfoOpt =
						tierDescriptionTree.findNode(this.tierNamePath);
				if(tierInfoOpt.isEmpty())
					throw new IllegalStateException("Invalid tier node path");
				this.tierNameRef = tierInfoOpt.get();
			} else {
				throw new IllegalStateException("No tier node path");
			}
		}
		return this.tierNameRef.getPrefix();
	}

	public Set<TernaryTreeNode<Collection<TypeEntry>>> getLinkedTierRefs(TernaryTree<Collection<TypeEntry>> tree) {
		if(this.linkedTierRefs == null) {
			if(this.linkedNodePaths != null) {
				this.linkedTierRefs = new LinkedHashSet<>();
				for(var path:this.linkedNodePaths) {
					Optional<TernaryTreeNode<Collection<TypeEntry>>> tierNodeOpt =
							tree.findNode(path);
					if(tierNodeOpt.isEmpty())
						throw new IllegalStateException("Invalid value path");
					this.linkedTierRefs.add(tierNodeOpt.get());
				}
			} else {
				throw new IllegalStateException("No linked values");
			}
		}
		return this.linkedTierRefs;
	}

	public void addLinkedTier(TernaryTree<Collection<TypeEntry>> tree,
	                          TernaryTreeNode<Collection<TypeEntry>> linkedNode) {
		getLinkedTierRefs(tree).add(linkedNode);
	}

	private void readObject(ObjectInputStream oin) throws IOException, ClassNotFoundException {
		this.tierNameRef = null;
		this.linkedTierRefs = null;

		this.tierNamePath = (TernaryTreeNodePath) oin.readObject();
		final int numLinks = oin.readInt();
		this.linkedNodePaths = new LinkedHashSet<>();
		for(int i = 0; i < numLinks; i++) {
			TernaryTreeNodePath linkedPath = (TernaryTreeNodePath) oin.readObject();
			this.linkedNodePaths.add(linkedPath);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		if(this.tierNameRef != null) {
			out.writeObject(this.tierNameRef.getPath());
		} else if(this.tierNamePath != null) {
			out.writeObject(this.tierNamePath);
		} else {
			throw new IOException("No path to tier name");
		}

		if(this.linkedTierRefs != null) {
			out.writeInt(this.linkedTierRefs.size());
			for(var linkedNode:linkedTierRefs) {
				out.writeObject(linkedNode.getPath());
			}
		} else if(this.linkedNodePaths != null) {
			out.writeInt(this.linkedNodePaths.size());
			for(var linkedPath:linkedNodePaths) {
				out.writeObject(linkedPath);
			}
		} else {
			out.writeInt(0);
		}
	}

}
