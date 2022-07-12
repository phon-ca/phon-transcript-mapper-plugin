package ca.phon.alignedMorpheme.db;

import java.util.*;

/**
 * Create cartesian sets of provided arrays.
 *
 * E.g.,
 * Given:
 * <pre>
 * String[][] arrays = new String[3][];
 * arrays[0] = new String[]{ "v1", "v2" };
 * arrays[1] = new String[]{ "v3", "v4" };
 * arrays[2] = new String[]{ "v5", "v6" };
 * </pre>
 *
 * return
 *
 * <pre>
 * [v1, v3, v5]
 * [v1, v3, v6]
 * [v1, v4, v5]
 * [v1, v4, v6]
 * [v2, v3, v5]
 * [v2, v3, v6]
 * [v2, v4, v5]
 * [v2, v4, v6]
 * </pre>
 */
public class StringArrayCartesianProduct {

	private final String[][] arrays;

	private final int[] maxVals;

	private final int[] currentVals;

	private final int lastArrayWithValues;

	public StringArrayCartesianProduct(String[][] arrays) {
		super();

		this.arrays = arrays;
		this.maxVals = new int[arrays.length];
		int lastArray = 0;
		for(int i = 0; i < arrays.length; i++) {
			this.maxVals[i] = arrays[i].length;
			if(this.maxVals[i] > 0) lastArray = i;
		}
		this.lastArrayWithValues = lastArray;
		this.currentVals = new int[arrays.length];
	}

	/**
	 * Calculate ard return cartesian sets of arrays
	 *
	 * @return
	 */
	public List<String[]> product() {
		final List<String[]> retVal = new ArrayList<>();

		while(this.currentVals[0] < this.maxVals[0]) {
			List<String> rowVals = new ArrayList<>();

			for(int i = 0; i < this.arrays.length; i++) {
				String[] colVals = this.arrays[i];

				String colVal = this.currentVals[i] < colVals.length ? colVals[this.currentVals[i]] : "";
				rowVals.add(colVal);
			}

			if(includeRow(rowVals))
				retVal.add(rowVals.toArray(new String[0]));

			bump();
		}

		return retVal;
	}

	/**
	 * Subclasses override to exclude rows
	 *
	 * @param rowVals
	 *
	 * @return true if row should be included in results
	 */
	protected boolean includeRow(List<String> rowVals) {
		return true;
	}

	private void bump() {
		int lastArray = this.currentVals.length - 1;
		for(int i = this.currentVals.length-1; i >= 1; i--) {
			if(this.currentVals[i] < this.maxVals[i] - 1) {
				++this.currentVals[i];
				for(int j = i + 1; j < this.currentVals.length; j++) {
					this.currentVals[j] = 0;
				}
				return;
			}
			lastArray = i;
		}

		++this.currentVals[lastArray-1];
		for(int i = lastArray; i < this.currentVals.length; i++)
			this.currentVals[i] = 0;
	}

}
