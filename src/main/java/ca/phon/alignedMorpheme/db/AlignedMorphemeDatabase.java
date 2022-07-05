package ca.phon.alignedMorpheme.db;

import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.session.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AlignedMorphemeDatabase implements Serializable {

	private static final long serialVersionUID = -4436233595101310518L;

	private TernaryTree<TierInfo> tierDescriptionTree;

	private TernaryTree<Collection<MorphemeTaggerEntry>> tree;

	public AlignedMorphemeDatabase() {
		super();

		tierDescriptionTree = new TernaryTree<>();
		setupTierDescriptionTree();

		tree = new TernaryTree<>();
	}

	private void setupTierDescriptionTree() {
		final TierInfo orthoInfo = new TierInfo(SystemTierType.Orthography.getName());
		orthoInfo.setOrder(0);
		tierDescriptionTree.put(SystemTierType.Orthography.getName(), orthoInfo);

		final TierInfo ipaTInfo = new TierInfo(SystemTierType.IPATarget.getName());
		ipaTInfo.setOrder(1);
		tierDescriptionTree.put(SystemTierType.IPATarget.getName(), ipaTInfo);

		final TierInfo ipaAInfo = new TierInfo(SystemTierType.IPAActual.getName());
		ipaAInfo.setOrder(2);
		tierDescriptionTree.put(SystemTierType.IPAActual.getName(), ipaAInfo);
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
		final TierInfo userTierInfo = new TierInfo(tierName);
		userTierInfo.setOrder(tierDescriptionTree.size());
		tierDescriptionTree.put(tierName, userTierInfo);
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
					morphemeNode.getValue().stream().filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
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
							.filter((e) -> e.getTierName(tierDescriptionTree).equals(entry.getKey())).findAny();
			if (morphemeEntryOpt.isPresent()) {
				MorphemeTaggerEntry morphemeEntryForTier = morphemeEntryOpt.get();
				for(int j = 0; j < entryList.length; j++) {
					if(j == i) continue;
					var otherEntry = (Map.Entry<String, String>)entryList[j];
					TernaryTreeNode<TierInfo> tierNodeRef = tierDescriptionTree.findNode(otherEntry.getKey()).get();
					TernaryTreeNode<Collection<MorphemeTaggerEntry>> otherNodeRef = tree.findNode(otherEntry.getValue()).get();

					Optional<MorphemeTaggerLinkedEntry> linkedEntryOpt =
							morphemeEntryForTier.getLinkedEntries()
									.stream()
									.filter((e) -> e.getTierName(tierDescriptionTree).equals(otherEntry.getKey())).findAny();
					if(linkedEntryOpt.isEmpty()) {
						MorphemeTaggerLinkedEntry linkedEntry = new MorphemeTaggerLinkedEntry(tierNodeRef);
						morphemeEntryForTier.addLinkedEntry(linkedEntry);
						linkedEntry.addLinkedTier(tree, otherNodeRef);
					} else {
						linkedEntryOpt.get().addLinkedTier(tree, otherNodeRef);
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
					morphemeNodeRef.getValue().stream().filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
			if(entryOpt.isPresent()) {
				MorphemeTaggerEntry entry = entryOpt.get();
				for(MorphemeTaggerLinkedEntry linkedEntry:entry.getLinkedEntries()) {
					String alignedTierName = linkedEntry.getTierName(tierDescriptionTree);
					String[] alignedTierVals = new String[linkedEntry.getLinkedTierRefs(tree).size()];
					int i = 0;
					for(TernaryTreeNode<Collection<MorphemeTaggerEntry>> alignedEntry:linkedEntry.getLinkedTierRefs(tree)) {
						alignedTierVals[i++] = alignedEntry.getPrefix();
					}
					retVal.put(alignedTierName, alignedTierVals);
				}
			}
		}

		return retVal;
	}

	public Collection<String> tierNames() {
		return tierDescriptionTree.values()
				.stream().sorted(Comparator.comparingInt(TierInfo::getOrder))
				.map(TierInfo::getTierName)
				.collect(Collectors.toList());
	}

	public Collection<TierInfo> getTierInfo() {
		return tierDescriptionTree.values()
				.stream().sorted(Comparator.comparingInt(TierInfo::getOrder))
				.collect(Collectors.toList());
	}

	private MorphemeTaggerEntry morphemeEntryForTier(String key, String tierName) {
		final Collection<MorphemeTaggerEntry> entries = morphemeEntries(key);
		final Optional<MorphemeTaggerEntry> morphemeTaggerEntry =
			entries.stream().filter((v) -> v.getTierName(tierDescriptionTree).equals(tierName)).findAny();
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

	public static class DuplicateTierEntry extends Exception {

		public DuplicateTierEntry(String tierName) {
			super(String.format("A tier with name %s already exists", tierName));
		}

	}

}
