package ca.phon.util.alignedTypesDatabase;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/**
 * Some helpful array methods used by the database
 */
public final class CartesianProduct {

	/**
	 * Calculate cartesian set of provided string arrays
	 *
	 * @param arrays
	 *
	 * @return cartestian product of arrays, no null values are returned
	 */
	public static String[][] stringArrayProduct(String[][] arrays) {
		return stringArrayProduct(arrays, (set) -> true);
	}

	/**
	 * Calculate cartesian set of provided string arrays
	 *
	 * @param arrays
	 * @param includeSet
	 *
	 * @return cartestian product of arrays, no null values are returned
	 */
	public static String[][] stringArrayProduct(String[][] arrays, Function<String[], Boolean> includeSet) {
		String[][] retVal = CartesianProduct.product(String.class, arrays, includeSet);
		for(String[] set:retVal) {
			for(int i = 0; i < set.length; i++) {
				if(set[i] == null)
					set[i] = "";
			}
		}
		return retVal;
	}

	/**
	 * Calculate and return cartesian product of arrays
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
	 *
	 * @param clazz type of array for return value
	 * @param arrays array of arrays of type T
	 *
	 * @return
	 */
	public static <T> T[][] product(Class<T> clazz, T[][] arrays) {
		return product(clazz, arrays, (set) -> true);
	}

	/**
	 * Calculate and return cartesian product of arrays
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
	 *
	 * @param clazz
	 * @param arrays array of arrays of type T
	 * @param includeSet used to filter sets returned by the product
	 *
	 * @return
	 */
	public static <T> T[][] product(Class<T> clazz, T[][] arrays, Function<T[], Boolean> includeSet) {
		final List<T[]> retVal = new ArrayList<>();

		final int[] currentVals = new int[arrays.length];
		final int[] maxVals = new int[arrays.length];
		for(int i = 0; i < arrays.length; i++) {
			maxVals[i] = arrays[i].length;
		}

		final T[] rowVals = (T[]) Array.newInstance(clazz, arrays.length);
		while(currentVals[0] < maxVals[0]) {
			for(int i = 0; i < arrays.length; i++) {
				T[] colVals = arrays[i];

				T colVal = currentVals[i] < colVals.length ? colVals[currentVals[i]] : null;
				rowVals[i] = colVal;
			}

			if(includeSet.apply((T[])rowVals))
				retVal.add(Arrays.copyOf(rowVals, rowVals.length));

			bump(currentVals, maxVals);
		}

		return retVal.toArray((T[][]) Array.newInstance(rowVals.getClass(), retVal.size()));
	}

	/**
	 * Adjust currentVals to move to next set in product
	 *
	 * @param currentVals
	 * @param maxVals
	 */
	private static void bump(int[] currentVals, int[] maxVals) {
		int lastArray = currentVals.length - 1;
		for(int i = currentVals.length-1; i >= 1; i--) {
			if(currentVals[i] < maxVals[i] - 1) {
				++currentVals[i];
				for(int j = i + 1; j < currentVals.length; j++) {
					currentVals[j] = 0;
				}
				return;
			}
			lastArray = i;
		}

		++currentVals[lastArray-1];
		for(int i = lastArray; i < currentVals.length; i++)
			currentVals[i] = 0;
	}

}
