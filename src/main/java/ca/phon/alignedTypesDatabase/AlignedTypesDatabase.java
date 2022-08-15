package ca.phon.alignedTypesDatabase;

import au.com.bytecode.opencsv.*;
import ca.hedlund.tst.*;
import ca.phon.app.log.LogUtil;
import ca.phon.session.*;
import ca.phon.util.Tuple;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A database of types (unique strings) along with the tiers in which they appear
 * and a list of other types to which they are linked.  This database also includes
 * a list of tiers along with ordering and visibility of those tiers. The visibility
 * parameter is used by the editor view and will not affect the return values for
 * the tierNames() or tierInfo() methods.
 *
 */
public final class AlignedTypesDatabase implements Serializable {

	private static final long serialVersionUID = -4436233595101310518L;

	private TernaryTree<TierInfo> tierDescriptionTree;

	private TernaryTree<Collection<TypeEntry>> tree;

	private TernaryTree<Collection<TernaryTreeNode<TierInfo>>> alwaysExcludeTree;

	public AlignedTypesDatabase() {
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
	public synchronized void addUserTier(String tierName) throws DuplicateTierEntry {
		if(tierDescriptionTree.containsKey(tierName)) {
			throw new DuplicateTierEntry(tierName);
		}
		final TierInfo userTierInfo = new TierInfo(tierName);
		userTierInfo.setOrder(tierDescriptionTree.size());
		tierDescriptionTree.put(tierName, userTierInfo);
	}

	/**
	 * Add type for specified tier
	 *
	 *
	 *
	 * @throws IllegalStateException if unable to add tier name or type to database
	 */
	public synchronized TernaryTreeNode<Collection<TypeEntry>> addTypeForTier(String tierName, String type) {
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

		Optional<TernaryTreeNode<Collection<TypeEntry>>> typeNodeOpt = tree.findNode(type, true, true);
		if(typeNodeOpt.isPresent()) {
			final TernaryTreeNode<Collection<TypeEntry>> typeNode = typeNodeOpt.get();
			if(!typeNode.isTerminated()) {
				List<TypeEntry> entryList = new ArrayList<>();
				typeNode.setValue(entryList);
			}
			Optional<TypeEntry> entryOpt =
					typeNode.getValue().stream().filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
			if(entryOpt.isEmpty()) {
				TypeEntry entry = new TypeEntry(tierNameRef);
				typeNode.getValue().add(entry);
			}
			return typeNode;
		} else {
			throw new IllegalStateException("Unable to add type to database");
		}
	}

	private Tuple<String[], String[]> alignedTypesToArrays(Map<String, String> alignedTypes) {
		final List<Tuple<String, String>> alignedInfo =
				alignedTypes.entrySet().stream()
						.map(e -> new Tuple<String, String>(e.getKey(), e.getValue()))
						.collect(Collectors.toList());
		final String[] tierNames = alignedInfo.stream().map(Tuple::getObj1).collect(Collectors.toList()).toArray(new String[0]);
		final String[] types = alignedInfo.stream().map(Tuple::getObj2).collect(Collectors.toList()).toArray(new String[0]);
		return new Tuple<>(tierNames, types);
	}

	/**
	 * Add aligned types to the database.  This method will add each type as a key in the database
	 * and setup tier links as necessary.
	 *
	 * @param alignedTypes a map of tierName -> types which will be added to the database
	 */
	public synchronized void addAlignedTypes(Map<String, String> alignedTypes) {
		final Tuple<String[], String[]> alignedArrays = alignedTypesToArrays(alignedTypes);
		final String[] tierNames = alignedArrays.getObj1();
		final String[] types = alignedArrays.getObj2();
		// don't include cycle which already exists
		if(hasAlignedTypes(tierNames, types)) {
			LogUtil.info(String.format("Alignment for tiers %s with types %s already exists",
					Arrays.toString(tierNames), Arrays.toString(types)));
			return;
		}

		addAlignedTypes(tierNames, types);
	}

	public synchronized void addAlignedTypes(String[] tierNames, String[] types) {
		for(int i = 0; i < tierNames.length; i++) {
			final String tierName = tierNames[i];
			final String type = types[i];

			for(int j = 0; j < tierNames.length; j++) {
				final String alignedTierName = tierNames[j];
				final String alignedType = types[j];

				addAlignment(tierName, type, alignedTierName, alignedType);
			}
		}
	}

	public synchronized void addAlignment(String tierName, String type, String alignedTierName, String alignedType) {
		final TernaryTreeNode<Collection<TypeEntry>> typeNode = addTypeForTier(tierName, type);
		final TernaryTreeNode<Collection<TypeEntry>> alignedTypeNode = addTypeForTier(alignedTierName, alignedType);
		final TernaryTreeNode<TierInfo> alignedTierNameNode = tierDescriptionTree.findNode(alignedTierName).get();

		Optional<TypeEntry> typeEntryOpt =
				typeNode.getValue()
						.stream()
						.filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
		if (typeEntryOpt.isPresent()) {
			TypeEntry typeEntryForTier = typeEntryOpt.get();

			Optional<TypeLinkedEntry> linkedEntryOpt =
					typeEntryForTier.getLinkedEntries()
							.stream()
							.filter((e) -> e.getTierName(tierDescriptionTree).equals(alignedTierName)).findAny();
			if(linkedEntryOpt.isEmpty()) {
				TypeLinkedEntry linkedEntry = new TypeLinkedEntry(alignedTierNameNode);
				typeEntryForTier.addLinkedEntry(linkedEntry);
				linkedEntry.addLinkedTier(tree, alignedTypeNode);
			} else {
				linkedEntryOpt.get().incrementLinkedTier(tree, alignedTypeNode);
			}
		}
	}

	public synchronized boolean removeAlignedTypes(Map<String, String> alignedTypes) {
		final Tuple<String[], String[]> alignedArrays = alignedTypesToArrays(alignedTypes);
		final String[] tierNames = alignedArrays.getObj1();
		final String[] types = alignedArrays.getObj2();
		// don't include cycle which already exists
		if(!hasAlignedTypes(tierNames, types)) {
			LogUtil.info(String.format("Alignment for tiers %s with types %s does not exist",
					Arrays.toString(tierNames), Arrays.toString(types)));
			return false;
		}

		return removeAlignedTypes(tierNames, types);
	}

	public synchronized boolean removeAlignedTypes(String[] tierNames, String[] types) {
		for(int i = 0; i < tierNames.length; i++) {
			final String tierName = tierNames[i];
			final String type = types[i];

			for(int j = 0; j < tierNames.length; j++) {
				if(i == j) continue;
				final String alignedTier = tierNames[j];
				final String alignedType = types[j];

				removeAlignment(tierName, type, alignedTier, alignedType);
			}
		}

		return true;
	}

	/**
	 * Return a set of aligned types given a tier name and type
	 * that exists for that tier.
	 *
	 * @param tierName
	 * @param type
	 *
	 * @return a map of aligned tier values for the given tier and type
	 */
	public Map<String, String[]> alignedTypesForTier(String tierName, String type) {
		return alignedTypesForTier(tierName, type, List.of());
	}

	/**
	 * Return a set of aligned types given a tier name and type
	 * that exists for that tier.
	 *
	 * @param tierName
	 * @param type
	 * @param tierList
	 *
	 * @return a map of aligned tier values for the given tier and type
	 */
	public Map<String, String[]> alignedTypesForTier(String tierName, String type, List<String> tierList) {
		Map<String, String[]> retVal = new LinkedHashMap<>();
		if(tierName == null || type == null) return retVal;

		Optional<TernaryTreeNode<Collection<TypeEntry>>> typeNodeRefOpt = tree.findNode(type);
		if(typeNodeRefOpt.isPresent()) {
			final TernaryTreeNode<Collection<TypeEntry>> typeNodeRef = typeNodeRefOpt.get();
			if(typeNodeRef.getValue() == null) {
				return retVal;
			}
			retVal.put(tierName, new String[]{type});
			Optional<TypeEntry> entryOpt =
					typeNodeRef.getValue().stream().filter((e) -> e.getTierName(tierDescriptionTree).equals(tierName)).findAny();
			if(entryOpt.isPresent()) {
				TypeEntry entry = entryOpt.get();
				retVal = alignedTypesForEntry(entry, tierList);
			}
		}

		return retVal;
	}

	private Map<String, String[]> alignedTypesForEntry(TypeEntry entry) {
		return alignedTypesForEntry(entry, List.of());
	}

	private Map<String, String[]> alignedTypesForEntry(TypeEntry entry, List<String> tierList) {
		Map<String, String[]> retVal = new LinkedHashMap<>();

		for(TypeLinkedEntry linkedEntry:entry.getLinkedEntries()) {
			String alignedTierName = linkedEntry.getTierName(tierDescriptionTree);
			boolean includeTier = tierList.size() > 0 ? tierList.contains(alignedTierName) : true;
			if(!includeTier) continue;
			var linkedTierCounts = linkedEntry.getLinkedTierCounts(tree)
					.entrySet()
					.stream()
					.filter(e -> e.getValue() != null && e.getValue() > 0)
					.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
			String[] alignedTierVals = new String[linkedTierCounts.size()];
			int i = 0;
			for(Map.Entry<TernaryTreeNode<Collection<TypeEntry>>, Integer> alignedEntry:linkedTierCounts.entrySet()) {
				alignedTierVals[i++] = alignedEntry.getKey().getPrefix();
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

	private TypeEntry typeEntryForTier(String key, String tierName) {
		final Collection<TypeEntry> entries = typeEntries(key);
		final Optional<TypeEntry> typeTaggerEntry =
			entries.stream().filter((v) -> v.getTierName(tierDescriptionTree).equals(tierName)).findAny();
		if(typeTaggerEntry.isPresent())
			return typeTaggerEntry.get();
		else
			return null;
	}

	private Collection<TypeEntry> typeEntries(String key) {
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
			Map<String, String> alignedTypes = new HashMap<>();
			for(int i = 0; i < cols.length; i++) {
				String tierName = cols[i];
				String type = (i < row.length ? row[i] : "");
				alignedTypes.put(tierName, type);
			}
			addAlignedTypes(alignedTypes);
		}
	}

	public boolean typeExists(String type) {
		return this.tree.containsKey(type);
	}

	public boolean typeExistsInTier(String type, String tier) {
		Optional<TernaryTreeNode<Collection<TypeEntry>>> treeNodeOpt = this.tree.findNode(type);
		if(treeNodeOpt.isPresent()) {
			TernaryTreeNode<Collection<TypeEntry>> treeNode = treeNodeOpt.get();
			if(treeNode.getValue() != null) {
				Optional<TypeEntry> typeEntryForTier = treeNode.getValue().stream()
						.filter(e -> e.getTierName(this.tierDescriptionTree).equals(tier))
						.findAny();
				return typeEntryForTier.isPresent();
			} else {
				return false;
			}
		} else {
			return false;
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
	public boolean alignmentExists(String tierName, String tierVal, String linkedTier, String linkedVal) {
		final Optional<TernaryTreeNode<Collection<TypeEntry>>> nodeOpt = tree.findNode(tierVal);
		if(nodeOpt.isEmpty()) return false;

		final var node = nodeOpt.get();
		return alignmentExists(node, tierName, linkedTier, linkedVal);
	}

	/**
	 * Remove the link between two tier values.  If all links for the tier are removed, the
	 * type is also removed for that tier
	 *
	 * @param tierName
	 * @param tierVal
	 * @param linkedTier
	 * @param linkedVal
	 *
	 * @return true if link was removed
	 */
	public synchronized boolean removeAlignment(String tierName, String tierVal, String linkedTier, String linkedVal) {
		final Optional<TernaryTreeNode<Collection<TypeEntry>>> nodeOpt = tree.findNode(tierVal);
		if(nodeOpt.isEmpty()) return false;

		final var node = nodeOpt.get();
		if(node.getValue() == null) return false;

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
		final Optional<TernaryTreeNode<Collection<TypeEntry>>> linkedValOpt = linkedEntry.getLinkedTierRefs(tree)
				.stream()
				.filter((r) -> r.getPrefix().equals(linkedVal))
				.findAny();
		if(linkedValOpt.isPresent()) {
			int linkCnt = linkedEntry.getLinkedTierCount(tree, linkedValOpt.get());
			if(linkCnt > 0) {
				linkCnt = linkedEntry.decrementLinkedTier(tree, linkedValOpt.get());
				if(linkCnt == 0) {
					if (linkedEntry.getLinkedTierRefs(tree).size() == 0) {
						taggerEntry.getLinkedEntries().remove(linkedEntry);

						if (taggerEntry.getLinkedEntries().size() == 0) {
							node.getValue().remove(taggerEntry);
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	private boolean alignmentExists(TernaryTreeNode<Collection<TypeEntry>> node, String tierName, String linkedTier, String linkedVal) {
		if(node.getValue() == null) return false;
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
		if(linkedValOpt.isPresent()) {
			final int linkCnt = linkedEntry.getLinkedTierCount(tree, linkedValOpt.get());
			return linkCnt > 0;
		} else {
			return false;
		}
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
				Map<String, String[]> alignedTypes = alignedTypesForEntry(entryForKeyTier.get());

				String[][] typeOpts = new String[tiers.size()][];
				typeOpts[0] = new String[] { entry.getKey() };
				for(int i = 1; i < tiers.size(); i++) {
					String tierName = tiers.get(i);
					String[] tierOpts = alignedTypes.get(tierName);
					if(tierOpts == null)
						tierOpts = new String[0];
					typeOpts[i] = tierOpts;
				}

				String[][] filteredCartesianProduct =
						CartesianProduct.stringArrayProduct(typeOpts, (set) -> this.hasAlignedTypes(tierNames().toArray(new String[0]), set));
				for(String[] row:filteredCartesianProduct) {
					writer.writeNext(row);
				}
			}
		}
		writer.flush();
	}

	public Boolean hasAlignedTypes(Map<String, String> alignedTypes) {
		final Tuple<String[], String[]> alignedArrays = alignedTypesToArrays(alignedTypes);
		final String[] tierNames = alignedArrays.getObj1();
		final String[] types = alignedArrays.getObj2();

		return hasAlignedTypes(tierNames, types);
	}

	public Boolean hasAlignedTypes(String tierNames[], String[] rowVals) {
		if(rowVals.length != tierNames.length) return false;

		boolean retVal = true;
		// only include row if all values have links between them
		for(int i = 1; i < rowVals.length-1; i++) {
			String v1 = rowVals[i];
			if(v1 == null) continue; // ignore empty tier values
			String t1 = tierNames[i];

			for(int j = i + 1; j < rowVals.length; j++) {
				String v2= rowVals[j];
				if(v2 == null) continue; // ignore empty tier values
				String t2 = tierNames[j];

				retVal &= alignmentExists(t1, v1, t2, v2);
			}
		}

		return retVal;
	}

	/**
	 * Import all entries from given database into this database.
	 *
	 * @param importDb
	 */
	public void importDatabase(AlignedTypesDatabase importDb) {
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

		// first add all types to tree
		// we will need to add all keys first to ensure we have nodes for linked types
		for(Map.Entry<String, Collection<TypeEntry>> entry:entrySet) {
			String type = entry.getKey();
			for(TypeEntry taggerEntry:entry.getValue()) {
				addTypeForTier(taggerEntry.getTierName(importDb.tierDescriptionTree), type);
			}
		}

		// now create links for aligned types
		for(Map.Entry<String, Collection<TypeEntry>> entry:entrySet) {
			String type = entry.getKey();

			Optional<TernaryTreeNode<Collection<TypeEntry>>> keyNodeOpt = tree.findNode(type);
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

					for(Map.Entry<TernaryTreeNode<Collection<TypeEntry>>, Integer> importTierNodeRef:importTaggerLinkedEntry.getLinkedTierCounts(importDb.tree).entrySet()) {
						// find the tree node for the linked type
						Optional<TernaryTreeNode<Collection<TypeEntry>>> tierNodeRef =
								tree.findNode(importTierNodeRef.getKey().getPrefix());
						if(tierNodeRef.isPresent()) {
							taggerLinkedEntry.getLinkedTierCounts(tree).put(tierNodeRef.get(), importTierNodeRef.getValue());
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
