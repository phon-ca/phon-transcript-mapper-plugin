package ca.phon.session.alignedMorpheme;

import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.project.Project;
import ca.phon.session.*;

import java.util.*;

public class AlignedMorphemeDatabase {

	private TernaryTree<TierDescription> tierDescriptionTree;

	private TernaryTree<Collection<MorphemeTaggerEntry>> tree;

	public AlignedMorphemeDatabase(Project project) {
		super();

		tierDescriptionTree = new TernaryTree<>();
		setupTierDescriptionTree();

		tree = new TernaryTree<>();
	}

	private void setupTierDescriptionTree() {
		SessionFactory factory = SessionFactory.newFactory();
		for(SystemTierType systemTier:SystemTierType.values()) {
			if(systemTier.isGrouped())
				tierDescriptionTree.put(systemTier.getName(),
						factory.createTierDescription(systemTier.getName(), systemTier.isGrouped(),
								systemTier.getDeclaredType()));
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
		SessionFactory factory = SessionFactory.newFactory();
		tierDescriptionTree.put(tierName, factory.createTierDescription(tierName, true, TierString.class));
	}

	/**
	 * Add tier value to database without aligned tier data
	 *
	 */
	public TernaryTreeNode<Collection<MorphemeTaggerEntry>> addMorphemeForTier(String tierName, String morpheme) {
		// ensure tier exists
		TernaryTreeNode<TierDescription> tierNameRef = tierDescriptionTree.findNode(tierName);
		if(tierNameRef == null) {
			try {
				addUserTier(tierName);
			} catch (Exception e) {
				LogUtil.warning(e);
			}
			tierNameRef = tierDescriptionTree.findNode(tierName);
		}
		if(tierNameRef == null)
			throw new IllegalStateException("Unable to add tier name to database");

		TernaryTreeNode<Collection<MorphemeTaggerEntry>> morphemeNode = tree.findNode(morpheme, true, true);
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
	}

	public void addAlignedMorphemes(Map<String, String> alignedMorphemes) {
		for(var entry:alignedMorphemes.entrySet()) {
			addMorphemeForTier(entry.getKey(), entry.getValue());
		}

		var entryList = alignedMorphemes.entrySet().toArray();
		for(int i = 0; i < entryList.length; i++) {
			var entry = (Map.Entry<String, String>)entryList[i];
			TernaryTreeNode<Collection<MorphemeTaggerEntry>> morphemeNodeRef = tree.findNode(entry.getValue());
			Optional<MorphemeTaggerEntry> morphemeEntryOpt =
					morphemeNodeRef.getValue().stream().filter((e) -> e.getTierName().equals(entry.getKey())).findAny();
			if (morphemeEntryOpt.isPresent()) {
				MorphemeTaggerEntry morphemeEntryForTier = morphemeEntryOpt.get();
				for(int j = 0; j < entryList.length; j++) {
					if(j == i) continue;
					var otherEntry = (Map.Entry<String, String>)entryList[j];
					TernaryTreeNode<TierDescription> tierNodeRef = tierDescriptionTree.findNode(otherEntry.getKey());
					TernaryTreeNode<Collection<MorphemeTaggerEntry>> otherNodeRef = tree.findNode(otherEntry.getValue());

					Optional<MorphemeTaggerLinkedEntry> linkedEntryOpt =
							morphemeEntryForTier.alignedTierLinkedEntries.stream().filter((e) -> e.getTierName().equals(otherEntry.getKey())).findAny();

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

	public Map<String, String[]> lookupMorphemeForTier(String tierName, String morpheme) {
		Map<String, String[]> retVal = new LinkedHashMap<>();

		TernaryTreeNode<Collection<MorphemeTaggerEntry>> morphemeNodeRef = tree.findNode(morpheme);
		if(morphemeNodeRef != null) {
			retVal.put(tierName, new String[]{morpheme});
			Optional<MorphemeTaggerEntry> entryOpt =
					morphemeNodeRef.getValue().stream().filter((e) -> e.getTierName().equals(tierName)).findAny();
			if(entryOpt.isPresent()) {
				MorphemeTaggerEntry entry = entryOpt.get();
				for(MorphemeTaggerLinkedEntry linkedEntry:entry.alignedTierLinkedEntries) {
					String alignedTierName = linkedEntry.getTierName();
					String alignedTierVals[] = new String[linkedEntry.linkedTierRefs.size()];
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

	public Collection<TierDescription> getTierDescriptions() {
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
		final TernaryTreeNode<Collection<MorphemeTaggerEntry>> node = tree.findNode(key);
		if(node == null) return new ArrayList<>();

		return node.getValue();
	}

	private class MorphemeTaggerEntry {
		// reference to tier name node, tier names are stored frequently
		// and storing a reference to the tree node reduces memory footprint
		TernaryTreeNode<TierDescription> tierNameRef;

		// map of links to aligned tier data
		List<MorphemeTaggerLinkedEntry> alignedTierLinkedEntries;

		public MorphemeTaggerEntry(TernaryTreeNode<TierDescription> tierNameRef) {
			this(tierNameRef, new ArrayList<>());
		}

		public MorphemeTaggerEntry(TernaryTreeNode<TierDescription> tierNameRef,
		                           List<MorphemeTaggerLinkedEntry> alignedTierLinkedEntries) {
			super();

			this.tierNameRef = tierNameRef;
			this.alignedTierLinkedEntries = alignedTierLinkedEntries;
		}

		public String getTierName() {
			return this.tierNameRef.getPrefix();
		}

	}

	private class MorphemeTaggerLinkedEntry {

		TernaryTreeNode<TierDescription> tierNameRef;

		Set<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> linkedTierRefs;

		public MorphemeTaggerLinkedEntry(TernaryTreeNode<TierDescription> tierNameRef) {
			this(tierNameRef, new LinkedHashSet<>());
		}

		public MorphemeTaggerLinkedEntry(TernaryTreeNode<TierDescription> tierNameRef,
		                                 Set<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> linkedTierRefs) {
			super();

			this.tierNameRef = tierNameRef;
			this.linkedTierRefs = linkedTierRefs;
		}

		public String getTierName() {
			return this.tierNameRef.getPrefix();
		}

	}

	public static class DuplicateTierEntry extends Exception {

		public DuplicateTierEntry(String tierName) {
			super(String.format("A tier with name %s already exists", tierName));
		}

	}

}
