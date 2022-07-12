package ca.phon.alignedMorpheme.db;

import java.util.List;

/**
 * Return cartesian sets of provided arrays.
 *
 * Values are only included in the product if they exist in
 * the database.
 *
 */
public class MorphemeMapCartesianProduct extends StringArrayCartesianProduct {

	private final AlignedMorphemeDatabase db;

	private final String[] tierNames;

	/**
	 * Constructor
	 *
	 * @param db aligned type database
	 * @param tierNames list of tier names, <code>tierNames.length = tierOpts.length</code>
	 * @param tierOpts list of tier values for each tier listed in <code>tierNames</code>
	 */
	public MorphemeMapCartesianProduct(AlignedMorphemeDatabase db, String[] tierNames, String[][] tierOpts) {
		super(tierOpts);

		this.db = db;
		this.tierNames = tierNames;
	}

	@Override
	public boolean includeRow(List<String> rowVals) {
		if(rowVals.size() != tierNames.length) return false;

		boolean retVal = true;
		// only include row if all values have links between them
		for(int i = 1; i < rowVals.size()-1; i++) {
			String v1 = rowVals.get(i);
			if(v1.trim().length() == 0) continue; // ignore empty tier values
			String t1 = tierNames[i];

			for(int j = i + 1; j < rowVals.size(); j++) {
				String v2= rowVals.get(j);
				if(v2.trim().length() == 0) continue; // ignore empty tier values
				String t2 = tierNames[j];

				retVal &= db.linkExists(t1, v1, t2, v2);
			}
		}

		return retVal;
	}

}
