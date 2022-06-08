package ca.phon.session.alignedMorpheme;

import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.project.Project;
import ca.phon.session.*;

import java.util.*;

public class MorphemeTaggerDatabase {

	private TernaryTree<TierDescription> tierDescriptionTree;

	private TernaryTree<Collection<MorphemeTaggerEntry>> tree;

	public MorphemeTaggerDatabase(Project project) {
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
		MorphemeTaggerEntry entry = new MorphemeTaggerEntry(tierNameRef, new LinkedHashMap<>());
		morphemeNode.getValue().add(entry);

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
					TernaryTreeNode<TierDescription> tierNodeRef = tierDescriptionTree.findNode(otherEntry.getValue());
					TernaryTreeNode<Collection<MorphemeTaggerEntry>> otherNodeRef = tree.findNode(otherEntry.getValue());
					morphemeEntryForTier.alignedTierMap.put(tierNodeRef, otherNodeRef);
				}
			}
		}
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
		Map<TernaryTreeNode<TierDescription>, TernaryTreeNode<Collection<MorphemeTaggerEntry>>> alignedTierMap;

		public MorphemeTaggerEntry(TernaryTreeNode<TierDescription> tierNameRef,
		                           Map<TernaryTreeNode<TierDescription>, TernaryTreeNode<Collection<MorphemeTaggerEntry>>> alignedTierMap) {
			super();

			this.tierNameRef = tierNameRef;
			this.alignedTierMap = alignedTierMap;
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
