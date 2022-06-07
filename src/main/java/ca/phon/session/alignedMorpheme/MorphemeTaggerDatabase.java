package ca.phon.session.alignedMorpheme;

import bibliothek.gui.dock.common.location.CFlapIndexLocation;
import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.project.Project;
import ca.phon.session.*;

import java.util.*;

public class MorphemeTaggerDatabase {

	private TernaryTree<TierDescription> tierDescriptionTree;

	private TernaryTree<MorphemeTaggerEntry[]> tree;

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
	public void addMorphemeForTier(String tierName, String morpheme) {
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

		TernaryTreeNode<MorphemeTaggerEntry[]> morphemeNode = tree.findNode(morpheme);
		if(morphemeNode == null) {
			MorphemeTaggerEntry entry = new MorphemeTaggerEntry(tierNameRef, new LinkedHashMap<>());
		}


	}

	public Collection<String> tierNames() {
		return tierDescriptionTree.keySet();
	}

	public Collection<TierDescription> getTierDescriptions() {
		return tierDescriptionTree.values();
	}

	private MorphemeTaggerEntry morphemeEntryForTier(String key, String tierName) {
		final MorphemeTaggerEntry[] entries = morphemeEntries(key);
		final Optional<MorphemeTaggerEntry> morphemeTaggerEntry =
			Arrays.stream(entries).filter((v) -> v.getTierName().equals(tierName)).findAny();
		if(morphemeTaggerEntry.isPresent())
			return morphemeTaggerEntry.get();
		else
			return null;
	}

	private MorphemeTaggerEntry[] morphemeEntries(String key) {
		final TernaryTreeNode<MorphemeTaggerEntry[]> node = tree.findNode(key);
		if(node == null) return new MorphemeTaggerEntry[0];

		return node.getValue();
	}

	private class MorphemeTaggerEntry {
		// reference to tier name node
		TernaryTreeNode<TierDescription> tierNameRef;

		// links to other tiers
		Map<TernaryTreeNode<TierDescription>, TernaryTreeNode<MorphemeTaggerEntry>> alignedTierRefs;

		public MorphemeTaggerEntry(TernaryTreeNode<TierDescription> tierNameRef,
		                           Map<TernaryTreeNode<TierDescription>, TernaryTreeNode<MorphemeTaggerEntry>> alignedTierRefs) {
			super();

			this.tierNameRef = tierNameRef;
			this.alignedTierRefs = alignedTierRefs;
		}

		public String getTierName() {
			return this.tierNameRef.getPrefix();
		}

		public Map<String, String> getAlignedTierMorphemes() {
			Map<String, String> retVal = new LinkedHashMap<>();
			for(var alignedTierRef:this.alignedTierRefs.entrySet()) {
				retVal.put(alignedTierRef.getKey().getPrefix(), alignedTierRef.getValue().getPrefix());
			}
			return retVal;
		}

	}

	public static class DuplicateTierEntry extends Exception {

		public DuplicateTierEntry(String tierName) {
			super(String.format("A tier with name %s already exists", tierName));
		}

	}

}
