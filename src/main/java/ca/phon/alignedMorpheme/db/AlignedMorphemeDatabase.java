package ca.phon.alignedMorpheme.db;

import au.com.bytecode.opencsv.*;
import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.session.*;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A database of tokens, or morphemes, along with the tiers in which they appear
 * and a list of other tokens to which they are linked.  This database includes
 * a list of tiers along with ordering and visibility of those tiers. The visibility
 * parameter is used by the editor view and will not affect the return values for
 * the tierNames() or tierInfo() methods.
 *
 */
public class AlignedMorphemeDatabase implements Serializable {

	private static final long serialVersionUID = -4436233595101310518L;

	private TernaryTree<TierInfo> tierDescriptionTree;

	private TernaryTree<Collection<MorphemeTaggerEntry>> tree;

	private TernaryTree<Collection<TernaryTreeNode<TierInfo>>> alwaysExcludeTree;

	public AlignedMorphemeDatabase() {
		super();

		tierDescriptionTree = new TernaryTree<>();
		setupTierDescriptionTree();

		tree = new TernaryTree<>();
		alwaysExcludeTree = new TernaryTree<>();
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
				retVal = alignedMorphemesForEntry(entry);
			}
		}

		return retVal;
	}

	private Map<String, String[]> alignedMorphemesForEntry(MorphemeTaggerEntry entry) {
		Map<String, String[]> retVal = new LinkedHashMap<>();

		for(MorphemeTaggerLinkedEntry linkedEntry:entry.getLinkedEntries()) {
			String alignedTierName = linkedEntry.getTierName(tierDescriptionTree);
			String[] alignedTierVals = new String[linkedEntry.getLinkedTierRefs(tree).size()];
			int i = 0;
			for(TernaryTreeNode<Collection<MorphemeTaggerEntry>> alignedEntry:linkedEntry.getLinkedTierRefs(tree)) {
				alignedTierVals[i++] = alignedEntry.getPrefix();
			}
			retVal.put(alignedTierName, alignedTierVals);
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

	/**
	 * Import entries from the provided (utf-8) csv file
	 *
	 * @param csvFile
	 */
	public void importFromCSV(File csvFile) throws IOException {
		importFromCSV(csvFile, "UTF-8");
	}

	/**
	 * Import entries from the provided csv file. The first row of the
	 * csv file will be read as the tier name
	 *
	 * @param csvFile
	 * @param encoding
	 */
	public void importFromCSV(File csvFile, String encoding) throws IOException {
		try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), encoding))) {
			importFromCSV(reader);
		}
	}

	/**
	 * Import entries from the given csv data.  The first row of the
	 * 	 * csv file will be read as the tier name
	 *
	 * @param csvData
	 */
	public void importFromCSV(String csvData) throws IOException {
		try (CSVReader reader = new CSVReader(new InputStreamReader(
				new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8))))) {
			importFromCSV(reader);
		}
	}

	private void importFromCSV(CSVReader csvReader) throws IOException {
		final String[] cols = csvReader.readNext();
		for(String tierName:cols) {
			if(!tierDescriptionTree.containsKey(tierName)) {
				try {
					addUserTier(tierName);
				} catch (DuplicateTierEntry e) {
					throw new IOException(e);
				}
			}
		}

		String[] row = null;
		while((row = csvReader.readNext()) != null) {
			Map<String, String> alignedMorphemes = new HashMap<>();
			for(int i = 0; i < cols.length; i++) {
				String tierName = cols[i];
				String morpheme = (i < row.length ? row[i] : "");
				if(morpheme.trim().length() > 0) {
					alignedMorphemes.put(tierName, morpheme);
				}
			}
			addAlignedMorphemes(alignedMorphemes);
		}
	}

	/**
	 * Is there a link between the two tier values
	 *
	 * @param tierName
	 * @param tierVal
	 * @param linkedTier
	 * @param linkedVal
	 */
	public boolean linkExists(String tierName, String tierVal, String linkedTier, String linkedVal) {
		final Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> nodeOpt = tree.findNode(tierVal);
		if(nodeOpt.isEmpty()) return false;

		final var node = nodeOpt.get();
		return linkExists(node, tierName, linkedTier, linkedVal);
	}

	private boolean linkExists(TernaryTreeNode<Collection<MorphemeTaggerEntry>> node, String tierName, String linkedTier, String linkedVal) {
		final Optional<MorphemeTaggerEntry> entryForTier = node.getValue()
				.stream()
				.filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName))
				.findAny();
		if(entryForTier.isEmpty()) return false;

		final MorphemeTaggerEntry taggerEntry = entryForTier.get();
		final Optional<MorphemeTaggerLinkedEntry> linkedEntryOpt = taggerEntry.getLinkedEntries()
				.stream()
				.filter((e) -> e.getTierName(tierDescriptionTree).equals(linkedTier))
				.findAny();
		if(linkedEntryOpt.isEmpty()) return false;

		final MorphemeTaggerLinkedEntry linkedEntry = linkedEntryOpt.get();
		final Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> linkedValOpt
				= linkedEntry.getLinkedTierRefs(tree)
				.stream()
				.filter((r) -> r.getPrefix().equals(linkedVal))
				.findAny();

		return linkedValOpt.isPresent();
	}

	/**
	 * Export alignment data for given keyTier to csvFile include all tiers
	 *
	 * @param keyTier
	 * @param csvFile
	 * @throws IOException
	 */
	public void exportToCSV(String keyTier, File csvFile, String encoding) throws IOException {
		exportToCSV(keyTier, tierNames(), csvFile, encoding);
	}

	/**
	 * Export alignment data for given keyTier to csvFile include only specified tiers
	 *
	 * @param keyTier
	 * @param tierNames
	 * @param csvFile
	 * @throws IOException
	 */
	public void exportToCSV(String keyTier, Collection<String> tierNames, File csvFile, String encoding) throws IOException {
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile), encoding))) {
			exportToCSV(keyTier, tierNames, writer);
		}
	}

	/**
	 * Export alignment data for given keyTier to StringBuilder include all tiers
	 *
	 * @param keyTier
	 * @param builder
	 * @throws IOException
	 */
	public void exportToCSV(String keyTier, StringBuilder builder) throws IOException {
		exportToCSV(keyTier, tierNames(), builder);
	}

	/**
	 * Export alignment data for given keyTier to StringBuilder include only specified tiers
	 *
	 * @param keyTier
	 * @param tierNames
	 * @param builder
	 * @throws IOException
	 */
	public void exportToCSV(String keyTier, Collection<String> tierNames, StringBuilder builder) throws IOException {
		try (CSVWriter writer = new CSVWriter(new StringBuilderWriter(builder))) {
			exportToCSV(keyTier, tierNames, writer);
		}
	}

	private void exportToCSV(String keyTier, Collection<String> tierNames, CSVWriter writer) throws IOException {
		List<String> tiers = new ArrayList<>(tierNames);
		tiers.remove(keyTier);
		tiers.add(0, keyTier);

		// write header
		writer.writeNext(tiers.toArray(new String[0]));

		Set<Map.Entry<String, Collection<MorphemeTaggerEntry>>> entrySet = tree.entrySet();
		for(Map.Entry<String, Collection<MorphemeTaggerEntry>> entry:entrySet) {
			Optional<MorphemeTaggerEntry> entryForKeyTier = entry.getValue()
					.stream()
					.filter((e) -> e.getTierName(tierDescriptionTree).equals(keyTier))
					.findAny();
			if(entryForKeyTier.isPresent()) {
				Map<String, String[]> alignedMorphemes = alignedMorphemesForEntry(entryForKeyTier.get());

				String[][] morphemeOpts = new String[tiers.size()][];
				morphemeOpts[0] = new String[] { entry.getKey() };
				for(int i = 1; i < tiers.size(); i++) {
					String tierName = tiers.get(i);
					String[] tierOpts = alignedMorphemes.get(tierName);
					if(tierOpts == null)
						tierOpts = new String[0];
					morphemeOpts[i] = tierOpts;
				}

				MorphemeMapZipper zipper = new MorphemeMapZipper(this, tierNames().toArray(new String[0]), morphemeOpts);
				List<String[]> rows = zipper.zippedValues();

				for(String[] row:rows) {
					writer.writeNext(row);
				}
			}
		}
		writer.flush();
	}

	/**
	 * Import all entries from given database into this database.
	 *
	 * @param importDb
	 */
	public void importDatabase(AlignedMorphemeDatabase importDb) {
		// add all tiers
		for(TierInfo ti: importDb.getTierInfo()) {
			if(!tierDescriptionTree.containsKey(ti.getTierName())) {
				TierInfo cloneInfo = ti.clone();
				cloneInfo.setOrder(tierDescriptionTree.size());
				tierDescriptionTree.put(cloneInfo.getTierName(), cloneInfo);
			}
		}

		// walk tree and add all entries
		Set<Map.Entry<String, Collection<MorphemeTaggerEntry>>> entrySet = tree.entrySet();

		// first add all morphemes to tree
		// we will need to add all keys first to ensure we have nodes for linked morphemes
		for(Map.Entry<String, Collection<MorphemeTaggerEntry>> entry:entrySet) {
			String morpheme = entry.getKey();
			for(MorphemeTaggerEntry taggerEntry:entry.getValue()) {
				addMorphemeForTier(taggerEntry.getTierName(importDb.tierDescriptionTree), morpheme);
			}
		}

		// now create links for aligned morphemes
		for(Map.Entry<String, Collection<MorphemeTaggerEntry>> entry:entrySet) {
			String morpheme = entry.getKey();

			Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> keyNodeOpt = tree.findNode(morpheme);
			// shouldn't happen because we added it above
			if(!keyNodeOpt.isPresent()) continue;

			TernaryTreeNode<Collection<MorphemeTaggerEntry>> keyNode = keyNodeOpt.get();
			for(MorphemeTaggerEntry importTaggerEntry:entry.getValue()) {
				// create tagger entry for tier or use existing
				final String tierName = importTaggerEntry.getTierName(importDb.tierDescriptionTree);
				Optional<MorphemeTaggerEntry> existingEntry =
						keyNode.getValue().stream().filter(e -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
				final MorphemeTaggerEntry taggerEntry = existingEntry.isPresent() ? existingEntry.get() :
						new MorphemeTaggerEntry(tierDescriptionTree.findNode(tierName).get());
				if(existingEntry.isEmpty())
					keyNode.getValue().add(taggerEntry);

				for(MorphemeTaggerLinkedEntry importTaggerLinkedEntry:importTaggerEntry.getLinkedEntries()) {
					// create tagger linked entry for linked tier or use existing
					String linkedTierName = importTaggerLinkedEntry.getTierName(importDb.tierDescriptionTree);
					Optional<MorphemeTaggerLinkedEntry> existingLinkedEntry = taggerEntry.getLinkedEntries().stream()
							.filter((e) -> e.getTierName(tierDescriptionTree).equals(linkedTierName))
							.findAny();
					MorphemeTaggerLinkedEntry taggerLinkedEntry = (existingLinkedEntry.isPresent()
						? existingLinkedEntry.get()
						: new MorphemeTaggerLinkedEntry(
							tierDescriptionTree.findNode(importTaggerEntry.getTierName(importDb.tierDescriptionTree)).get()));
					if(existingLinkedEntry.isEmpty())
						taggerEntry.getLinkedEntries().add(taggerLinkedEntry);

					for(TernaryTreeNode<Collection<MorphemeTaggerEntry>> importTierNodeRef:importTaggerLinkedEntry.getLinkedTierRefs(importDb.tree)) {
						// find the tree node for the linked morpheme
						Optional<TernaryTreeNode<Collection<MorphemeTaggerEntry>>> tierNodeRef =
								tree.findNode(importTierNodeRef.getPrefix());
						if(tierNodeRef.isPresent() && !taggerLinkedEntry.getLinkedTierRefs(tree).contains(tierNodeRef.get())) {
							taggerLinkedEntry.addLinkedTier(tree, tierNodeRef.get());
						}
					}
				}
			}
		}
	}

	public static class DuplicateTierEntry extends Exception {

		public DuplicateTierEntry(String tierName) {
			super(String.format("A tier with name %s already exists", tierName));
		}

	}

}
