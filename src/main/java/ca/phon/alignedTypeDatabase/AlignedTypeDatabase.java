package ca.phon.alignedTypeDatabase;

import au.com.bytecode.opencsv.*;
import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.session.*;
import ca.phon.session.alignedType.ArrayUtils;
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
public class AlignedTypeDatabase implements Serializable {

	private static final long serialVersionUID = -4436233595101310518L;

	private TernaryTree<TierInfo> tierDescriptionTree;

	private TernaryTree<Collection<TypeEntry>> tree;

	private TernaryTree<Collection<TernaryTreeNode<TierInfo>>> alwaysExcludeTree;

	public AlignedTypeDatabase() {
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
	public TernaryTreeNode<Collection<TypeEntry>> addMorphemeForTier(String tierName, String morpheme) {
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

		Optional<TernaryTreeNode<Collection<TypeEntry>>> morphemeNodeOpt = tree.findNode(morpheme, true, true);
		if(morphemeNodeOpt.isPresent()) {
			final TernaryTreeNode<Collection<TypeEntry>> morphemeNode = morphemeNodeOpt.get();
			if(!morphemeNode.isTerminated()) {
				List<TypeEntry> entryList = new ArrayList<>();
				morphemeNode.setValue(entryList);
			}
			Optional<TypeEntry> entryOpt =
					morphemeNode.getValue().stream().filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
			if(entryOpt.isEmpty()) {
				TypeEntry entry = new TypeEntry(tierNameRef);
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
			TernaryTreeNode<Collection<TypeEntry>> morphemeNodeRef = tree.findNode(entry.getValue()).get();
			Optional<TypeEntry> morphemeEntryOpt =
					morphemeNodeRef.getValue()
							.stream()
							.filter((e) -> e.getTierName(tierDescriptionTree).equals(entry.getKey())).findAny();
			if (morphemeEntryOpt.isPresent()) {
				TypeEntry morphemeEntryForTier = morphemeEntryOpt.get();
				for(int j = 0; j < entryList.length; j++) {
					if(j == i) continue;
					var otherEntry = (Map.Entry<String, String>)entryList[j];
					TernaryTreeNode<TierInfo> tierNodeRef = tierDescriptionTree.findNode(otherEntry.getKey()).get();
					TernaryTreeNode<Collection<TypeEntry>> otherNodeRef = tree.findNode(otherEntry.getValue()).get();

					Optional<TypeLinkedEntry> linkedEntryOpt =
							morphemeEntryForTier.getLinkedEntries()
									.stream()
									.filter((e) -> e.getTierName(tierDescriptionTree).equals(otherEntry.getKey())).findAny();
					if(linkedEntryOpt.isEmpty()) {
						TypeLinkedEntry linkedEntry = new TypeLinkedEntry(tierNodeRef);
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

		Optional<TernaryTreeNode<Collection<TypeEntry>>> morphemeNodeRefOpt = tree.findNode(morpheme);
		if(morphemeNodeRefOpt.isPresent()) {
			final TernaryTreeNode<Collection<TypeEntry>> morphemeNodeRef = morphemeNodeRefOpt.get();
			retVal.put(tierName, new String[]{morpheme});
			Optional<TypeEntry> entryOpt =
					morphemeNodeRef.getValue().stream().filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
			if(entryOpt.isPresent()) {
				TypeEntry entry = entryOpt.get();
				retVal = alignedMorphemesForEntry(entry);
			}
		}

		return retVal;
	}

	private Map<String, String[]> alignedMorphemesForEntry(TypeEntry entry) {
		Map<String, String[]> retVal = new LinkedHashMap<>();

		for(TypeLinkedEntry linkedEntry:entry.getLinkedEntries()) {
			String alignedTierName = linkedEntry.getTierName(tierDescriptionTree);
			String[] alignedTierVals = new String[linkedEntry.getLinkedTierRefs(tree).size()];
			int i = 0;
			for(TernaryTreeNode<Collection<TypeEntry>> alignedEntry:linkedEntry.getLinkedTierRefs(tree)) {
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

	private TypeEntry morphemeEntryForTier(String key, String tierName) {
		final Collection<TypeEntry> entries = morphemeEntries(key);
		final Optional<TypeEntry> morphemeTaggerEntry =
			entries.stream().filter((v) -> v.getTierName(tierDescriptionTree).equals(tierName)).findAny();
		if(morphemeTaggerEntry.isPresent())
			return morphemeTaggerEntry.get();
		else
			return null;
	}

	private Collection<TypeEntry> morphemeEntries(String key) {
		final Optional<TernaryTreeNode<Collection<TypeEntry>>> node = tree.findNode(key);
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
		final Optional<TernaryTreeNode<Collection<TypeEntry>>> nodeOpt = tree.findNode(tierVal);
		if(nodeOpt.isEmpty()) return false;

		final var node = nodeOpt.get();
		return linkExists(node, tierName, linkedTier, linkedVal);
	}

	private boolean linkExists(TernaryTreeNode<Collection<TypeEntry>> node, String tierName, String linkedTier, String linkedVal) {
		final Optional<TypeEntry> entryForTier = node.getValue()
				.stream()
				.filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName))
				.findAny();
		if(entryForTier.isEmpty()) return false;

		final TypeEntry taggerEntry = entryForTier.get();
		final Optional<TypeLinkedEntry> linkedEntryOpt = taggerEntry.getLinkedEntries()
				.stream()
				.filter((e) -> e.getTierName(tierDescriptionTree).equals(linkedTier))
				.findAny();
		if(linkedEntryOpt.isEmpty()) return false;

		final TypeLinkedEntry linkedEntry = linkedEntryOpt.get();
		final Optional<TernaryTreeNode<Collection<TypeEntry>>> linkedValOpt
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

		Set<Map.Entry<String, Collection<TypeEntry>>> entrySet = tree.entrySet();
		for(Map.Entry<String, Collection<TypeEntry>> entry:entrySet) {
			Optional<TypeEntry> entryForKeyTier = entry.getValue()
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

				String[][] filteredCartesianProduct =
						ArrayUtils.stringArrayCartesianProduct(morphemeOpts, this::includeInCartesianProduct);
				for(String[] row:filteredCartesianProduct) {
					writer.writeNext(row);
				}
			}
		}
		writer.flush();
	}

	protected Boolean includeInCartesianProduct(String[] rowVals) {
		final String[] tierNames = tierNames().toArray(new String[0]);
		if(rowVals.length != tierNames.length) return false;

		boolean retVal = true;
		// only include row if all values have links between them
		for(int i = 1; i < rowVals.length-1; i++) {
			String v1 = rowVals[i];
			if(v1.trim().length() == 0) continue; // ignore empty tier values
			String t1 = tierNames[i];

			for(int j = i + 1; j < rowVals.length; j++) {
				String v2= rowVals[j];
				if(v2.trim().length() == 0) continue; // ignore empty tier values
				String t2 = tierNames[j];

				retVal &= linkExists(t1, v1, t2, v2);
			}
		}

		return retVal;
	}

	/**
	 * Import all entries from given database into this database.
	 *
	 * @param importDb
	 */
	public void importDatabase(AlignedTypeDatabase importDb) {
		// add all tiers
		for(TierInfo ti: importDb.getTierInfo()) {
			if(!tierDescriptionTree.containsKey(ti.getTierName())) {
				TierInfo cloneInfo = ti.clone();
				cloneInfo.setOrder(tierDescriptionTree.size());
				tierDescriptionTree.put(cloneInfo.getTierName(), cloneInfo);
			}
		}

		// walk tree and add all entries
		Set<Map.Entry<String, Collection<TypeEntry>>> entrySet = tree.entrySet();

		// first add all morphemes to tree
		// we will need to add all keys first to ensure we have nodes for linked morphemes
		for(Map.Entry<String, Collection<TypeEntry>> entry:entrySet) {
			String morpheme = entry.getKey();
			for(TypeEntry taggerEntry:entry.getValue()) {
				addMorphemeForTier(taggerEntry.getTierName(importDb.tierDescriptionTree), morpheme);
			}
		}

		// now create links for aligned morphemes
		for(Map.Entry<String, Collection<TypeEntry>> entry:entrySet) {
			String morpheme = entry.getKey();

			Optional<TernaryTreeNode<Collection<TypeEntry>>> keyNodeOpt = tree.findNode(morpheme);
			// shouldn't happen because we added it above
			if(!keyNodeOpt.isPresent()) continue;

			TernaryTreeNode<Collection<TypeEntry>> keyNode = keyNodeOpt.get();
			for(TypeEntry importTaggerEntry:entry.getValue()) {
				// create tagger entry for tier or use existing
				final String tierName = importTaggerEntry.getTierName(importDb.tierDescriptionTree);
				Optional<TypeEntry> existingEntry =
						keyNode.getValue().stream().filter(e -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
				final TypeEntry taggerEntry = existingEntry.isPresent() ? existingEntry.get() :
						new TypeEntry(tierDescriptionTree.findNode(tierName).get());
				if(existingEntry.isEmpty())
					keyNode.getValue().add(taggerEntry);

				for(TypeLinkedEntry importTaggerLinkedEntry:importTaggerEntry.getLinkedEntries()) {
					// create tagger linked entry for linked tier or use existing
					String linkedTierName = importTaggerLinkedEntry.getTierName(importDb.tierDescriptionTree);
					Optional<TypeLinkedEntry> existingLinkedEntry = taggerEntry.getLinkedEntries().stream()
							.filter((e) -> e.getTierName(tierDescriptionTree).equals(linkedTierName))
							.findAny();
					TypeLinkedEntry taggerLinkedEntry = (existingLinkedEntry.isPresent()
						? existingLinkedEntry.get()
						: new TypeLinkedEntry(
							tierDescriptionTree.findNode(importTaggerEntry.getTierName(importDb.tierDescriptionTree)).get()));
					if(existingLinkedEntry.isEmpty())
						taggerEntry.getLinkedEntries().add(taggerLinkedEntry);

					for(TernaryTreeNode<Collection<TypeEntry>> importTierNodeRef:importTaggerLinkedEntry.getLinkedTierRefs(importDb.tree)) {
						// find the tree node for the linked morpheme
						Optional<TernaryTreeNode<Collection<TypeEntry>>> tierNodeRef =
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
