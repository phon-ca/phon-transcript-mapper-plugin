package ca.phon.session.alignedMorpheme;

import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.session.*;

import java.io.*;
import java.util.*;

public class AlignedMorphemeDatabase implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private TernaryTree<TierInfo> tierDescriptionTree;

	private TernaryTree<Collection<MorphemeTaggerEntry>> tree;

	public AlignedMorphemeDatabase() {
		super();

		tierDescriptionTree = new TernaryTree<>();
		setupTierDescriptionTree();

		tree = new TernaryTree<>();
	}

	private void setupTierDescriptionTree() {
		for(SystemTierType systemTier:SystemTierType.values()) {
			if(systemTier.isGrouped())
				tierDescriptionTree.put(systemTier.getName(),
						new TierInfo(systemTier.getName()));
		}
	}

	/**
	 * Adds a user tier to the list of tiers in the database
	 * Tier data type is assumed to be TierString
	 *
	 * @param tierName
	 */
	public void addUserTier(String tierName) throws DuplicateTierEntry {
		if(tierDescriptionTree.containsKey(tierName)) {
			throw new DuplicateTierEntry(tierName);
		}
		tierDescriptionTree.put(tierName, new TierInfo(tierName));
	}

	/**
	 * Add tier value to database without aligned tier data
	 *
	 */
	public TernaryTreeNode<Collection<MorphemeTaggerEntry>> addMorphemeForTier(String tierName, String morpheme) {
		// ensure tier exists
		Optional<TernaryTreeNode<TierInfo>> tierNameRefOpt = tierDescriptionTree.findNode(tierName);
		if(tierNameRefOpt.isEmpty()) {
			try {
				addUserTier(tierName);
			} catch (Exception e) {
				LogUtil.warning(e);
			}
			tierNameRefOpt = tierDescriptionTree.findNode(tierName);
		}
		if(tierNameRefOpt.isEmpty())
			throw new IllegalStateException("Unable to add tier name to database");
		final TernaryTreeNode<TierInfo> tierNameRef = tierNameRefOpt.get();

		Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> morphemeNodeOpt = tree.findNode(morpheme, true, true);
		if(morphemeNodeOpt.isPresent()) {
			final TernaryTreeNode<Collection<MorphemeTaggerEntry>> morphemeNode = morphemeNodeOpt.get();
			if(!morphemeNode.isTerminated()) {
				List<MorphemeTaggerEntry> entryList = new ArrayList<>();
				morphemeNode.setValue(entryList);
			}
			Optional<MorphemeTaggerEntry> entryOpt =
					morphemeNode.getValue().stream().filter((e) -> e.getTierName().equals(tierName)).findAny();
			if(entryOpt.isEmpty()) {
				MorphemeTaggerEntry entry = new MorphemeTaggerEntry(tierNameRef);
				morphemeNode.getValue().add(entry);
			}
			return morphemeNode;
		} else {
			throw new IllegalStateException("Unable to add morpheme to database");
		}
	}

	public void addAlignedMorphemes(Map<String, String> alignedMorphemes) {
		for(var entry:alignedMorphemes.entrySet()) {
			addMorphemeForTier(entry.getKey(), entry.getValue());
		}

		var entryList = alignedMorphemes.entrySet().toArray();
		for(int i = 0; i < entryList.length; i++) {
			var entry = (Map.Entry<String, String>)entryList[i];
			TernaryTreeNode<Collection<MorphemeTaggerEntry>> morphemeNodeRef = tree.findNode(entry.getValue()).get();
			Optional<MorphemeTaggerEntry> morphemeEntryOpt =
					morphemeNodeRef.getValue()
							.stream()
							.filter((e) -> e.getTierName().equals(entry.getKey())).findAny();
			if (morphemeEntryOpt.isPresent()) {
				MorphemeTaggerEntry morphemeEntryForTier = morphemeEntryOpt.get();
				for(int j = 0; j < entryList.length; j++) {
					if(j == i) continue;
					var otherEntry = (Map.Entry<String, String>)entryList[j];
					TernaryTreeNode<TierInfo> tierNodeRef = tierDescriptionTree.findNode(otherEntry.getKey()).get();
					TernaryTreeNode<Collection<MorphemeTaggerEntry>> otherNodeRef = tree.findNode(otherEntry.getValue()).get();

					Optional<MorphemeTaggerLinkedEntry> linkedEntryOpt =
							morphemeEntryForTier.alignedTierLinkedEntries
									.stream()
									.filter((e) -> e.getTierName().equals(otherEntry.getKey())).findAny();
					if(linkedEntryOpt.isEmpty()) {
						MorphemeTaggerLinkedEntry linkedEntry = new MorphemeTaggerLinkedEntry(tierNodeRef);
						morphemeEntryForTier.alignedTierLinkedEntries.add(linkedEntry);
						linkedEntry.linkedTierRefs.add(otherNodeRef);
					} else {
						linkedEntryOpt.get().linkedTierRefs.add(otherNodeRef);
					}
				}
			}
		}
	}

	/**
	 * Return a set of aligned morphemes given a tier name and morpheme
	 * that exists for that tier.
	 *
	 * @param tierName
	 * @param morpheme
	 *
	 * @return a map of aligned tier values for the given tier and morpheme
	 */
	public Map<String, String[]> alignedMorphemesForTier(String tierName, String morpheme) {
		Map<String, String[]> retVal = new LinkedHashMap<>();

		Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> morphemeNodeRefOpt = tree.findNode(morpheme);
		if(morphemeNodeRefOpt.isPresent()) {
			final TernaryTreeNode<Collection<MorphemeTaggerEntry>> morphemeNodeRef = morphemeNodeRefOpt.get();
			retVal.put(tierName, new String[]{morpheme});
			Optional<MorphemeTaggerEntry> entryOpt =
					morphemeNodeRef.getValue().stream().filter((e) -> e.getTierName().equals(tierName)).findAny();
			if(entryOpt.isPresent()) {
				MorphemeTaggerEntry entry = entryOpt.get();
				for(MorphemeTaggerLinkedEntry linkedEntry:entry.alignedTierLinkedEntries) {
					String alignedTierName = linkedEntry.getTierName();
					String[] alignedTierVals = new String[linkedEntry.linkedTierRefs.size()];
					int i = 0;
					for(TernaryTreeNode<Collection<MorphemeTaggerEntry>> alignedEntry:linkedEntry.linkedTierRefs) {
						alignedTierVals[i++] = alignedEntry.getPrefix();
					}
					retVal.put(alignedTierName, alignedTierVals);
				}
			}
		}

		return retVal;
	}

	public Collection<String> tierNames() {
		return tierDescriptionTree.keySet();
	}

	public Collection<TierInfo> getTierInfo() {
		return tierDescriptionTree.values();
	}

	private MorphemeTaggerEntry morphemeEntryForTier(String key, String tierName) {
		final Collection<MorphemeTaggerEntry> entries = morphemeEntries(key);
		final Optional<MorphemeTaggerEntry> morphemeTaggerEntry =
			entries.stream().filter((v) -> v.getTierName().equals(tierName)).findAny();
		if(morphemeTaggerEntry.isPresent())
			return morphemeTaggerEntry.get();
		else
			return null;
	}

	private Collection<MorphemeTaggerEntry> morphemeEntries(String key) {
		final Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> node = tree.findNode(key);
		if(node.isEmpty()) return new ArrayList<>();

		return node.get().getValue();
	}

	public class TierInfo implements Serializable {

		private final static long serialVersionUID = 1L;

		final String tierName;

		final String tierFont;

		public TierInfo(String tierName) {
			this(tierName, "default");
		}

		public TierInfo(String tierName, String tierFont) {
			this.tierName = tierName;
			this.tierFont = tierFont;
		}

	}

	private class MorphemeTaggerEntry implements Serializable {

		private static final long serialVersionUID = 1L;

		// reference to tier name node, tier names are stored frequently
		// and storing a reference to the tree node reduces memory footprint
		private transient TernaryTreeNode<TierInfo> tierNameRef;

		// used for serialization to lazy-load linked values after tree structure is fully loaded into memory
		private transient TernaryTreeNodePath tierNameNodePath;

		// map of links to aligned tier data
		private transient List<MorphemeTaggerLinkedEntry> alignedTierLinkedEntries;

		public MorphemeTaggerEntry(TernaryTreeNode<TierInfo> tierNameRef) {
			this(tierNameRef, new ArrayList<>());
		}

		public MorphemeTaggerEntry(TernaryTreeNode<TierInfo> tierNameRef,
		                           List<MorphemeTaggerLinkedEntry> alignedTierLinkedEntries) {
			super();

			this.tierNameRef = tierNameRef;
			this.alignedTierLinkedEntries = alignedTierLinkedEntries;
		}

		public String getTierName() {
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

		@Serial
		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			this.tierNameRef = null;

			this.tierNameNodePath = (TernaryTreeNodePath) ois.readObject();

			this.alignedTierLinkedEntries = new ArrayList<>();
			final int numEntries = ois.readInt();
			for(int i = 0; i < numEntries; i++) {
				this.alignedTierLinkedEntries.add((MorphemeTaggerLinkedEntry) ois.readObject());
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

	private class MorphemeTaggerLinkedEntry implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private transient TernaryTreeNode<TierInfo> tierNameRef;

		private transient TernaryTreeNodePath tierNamePath;

		private transient Set<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> linkedTierRefs;

		private transient Collection<TernaryTreeNodePath> linkedNodePaths;

		public MorphemeTaggerLinkedEntry(TernaryTreeNode<TierInfo> tierNameRef) {
			this(tierNameRef, new LinkedHashSet<>());
		}

		public MorphemeTaggerLinkedEntry(TernaryTreeNode<TierInfo> tierNameRef,
		                                 Set<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> linkedTierRefs) {
			super();

			this.tierNameRef = tierNameRef;
			this.linkedTierRefs = linkedTierRefs;
		}

		public String getTierName() {
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

		public Set<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> getLinkedTierRefs() {
			if(this.linkedTierRefs == null) {
				if(this.linkedNodePaths != null) {
					this.linkedTierRefs = new LinkedHashSet<>();
					for(var path:this.linkedNodePaths) {
						Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> tierNodeOpt =
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

	public static class DuplicateTierEntry extends Exception {

		public DuplicateTierEntry(String tierName) {
			super(String.format("A tier with name %s already exists", tierName));
		}

	}

}
