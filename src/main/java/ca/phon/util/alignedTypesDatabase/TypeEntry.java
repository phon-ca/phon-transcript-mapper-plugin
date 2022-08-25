package ca.phon.util.alignedTypesDatabase;

import ca.hedlund.tst.*;

import java.io.*;
import java.util.*;

final class TypeEntry implements Serializable {

	private static final long serialVersionUID = -8095511445561636192L;

	// reference to tier name node, tier names are stored frequently
	// and storing a reference to the tree node reduces memory footprint
	private transient TernaryTreeNode<TierInfo> tierNameRef;

	// used for serialization to lazy-load linked values after tree structure is fully loaded into memory
	private transient TernaryTreeNodePath tierNameNodePath;

	// map of links to aligned tier data
	private transient List<TypeLinkedEntry> alignedTierLinkedEntries;

	public TypeEntry(TernaryTreeNode<TierInfo> tierNameRef) {
		this(tierNameRef, new ArrayList<>());
	}

	public TypeEntry(TernaryTreeNode<TierInfo> tierNameRef,
	                 List<TypeLinkedEntry> alignedTierLinkedEntries) {
		super();

		this.tierNameRef = tierNameRef;
		this.alignedTierLinkedEntries = alignedTierLinkedEntries;
	}

	public String getTierName(TernaryTree<TierInfo> tierDescriptionTree) {
		if(this.tierNameRef == null) {
			if(this.tierNameNodePath != null) {
				Optional<TernaryTreeNode<TierInfo>> tierNodeOpt =
						tierDescriptionTree.findNode(this.tierNameNodePath);
				if(tierNodeOpt.isEmpty()) {
					throw new IllegalStateException("Invalid tier name path");
				}
				this.tierNameRef = tierNodeOpt.get();
			} else {
				throw new IllegalStateException("No path to tier name");
			}
		}
		return this.tierNameRef.getPrefix();
	}

	public List<TypeLinkedEntry> getLinkedEntries() {
		return this.alignedTierLinkedEntries;
	}

	public void addLinkedEntry(TypeLinkedEntry entry) {
		this.alignedTierLinkedEntries.add(entry);
	}

	@Serial
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		this.tierNameRef = null;

		this.tierNameNodePath = (TernaryTreeNodePath) ois.readObject();

		this.alignedTierLinkedEntries = new ArrayList<>();
		final int numEntries = ois.readInt();
		for(int i = 0; i < numEntries; i++) {
			this.alignedTierLinkedEntries.add((TypeLinkedEntry) ois.readObject());
		}
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		if(tierNameRef != null) {
			out.writeObject(tierNameRef.getPath());
		} else if(tierNameNodePath != null) {
			out.writeObject(tierNameNodePath);
		} else {
			throw new IOException("No tree path to tier name");
		}

		if(this.alignedTierLinkedEntries != null) {
			out.writeInt(this.alignedTierLinkedEntries.size());
			for(var linkedEntry:this.alignedTierLinkedEntries) {
				out.writeObject(linkedEntry);
			}
		}
	}

}
