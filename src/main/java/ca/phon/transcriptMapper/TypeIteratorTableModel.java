/*
 * Copyright (C) 2005-2022 Gregory Hedlund & Yvan Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.worker.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.function.Consumer;

/**
 * Table model for {@link javax.swing.JTable} which accepts a database type iterator
 * for providing data.  Items are loaded in the background up to a specified maximum.
 */
public class TypeIteratorTableModel extends AbstractTableModel {

	private List<String> cachedValues;

	private final AlignedTypesDatabase db;

	private TypeIterator typeIterator;

	private boolean showTiers = false;

	public TypeIteratorTableModel(AlignedTypesDatabase db) {
		super();

		this.db = db;
		this.cachedValues = new ArrayList<>();
		this.typeIterator = db.typeIterator();
	}

	public AlignedTypesDatabase getDb() {
		return this.db;
	}

	public void setTypeIterator(TypeIterator itr) {
		this.typeIterator = itr;
		reset();
	}

	public boolean isShowTiers() {
		return this.showTiers;
	}

	public void setShowTiers(boolean showTiers) {
		this.showTiers = showTiers;
		super.fireTableStructureChanged();
	}

	public void reset() {
		int numRows = this.cachedValues.size();
		this.cachedValues.clear();
		if(numRows > 0)
			super.fireTableRowsDeleted(0, numRows-1);
	}

	@Override
	public int getRowCount() {
		return this.cachedValues.size();
	}

	@Override
	public int getColumnCount() {
		return (showTiers ? 2 : 1);
	}

	@Override
	public String getColumnName(int col) {
		if(col == 0) return "Type";
		else if(col == 1) return "Tier(s)";
		else return super.getColumnName(col);
	}

	public PhonTask loadItemsAsync(int numToLoad, Consumer<Integer> onFinish) {
		PhonTask loadTask = new TypeLoader(numToLoad, onFinish);
		PhonWorker.getInstance().invokeLater(loadTask);
		return loadTask;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final String type = this.cachedValues.get(rowIndex);
		if(columnIndex == 0) return type;
		else if(columnIndex == 1) {
			StringBuilder tierNames = new StringBuilder();
			for(String tierName:db.tierNames()) {
				if(db.typeExistsInTier(type, tierName)) {
					if (tierNames.length() > 0)
						tierNames.append(", ");
					tierNames.append(tierName);
				}
			}
			return tierNames.toString();
		}
		return this.cachedValues.get(rowIndex);
	}

	public void addCachedTypes(List<String> types) {
		int numRows = cachedValues.size();
		this.cachedValues.addAll(types);
		fireTableRowsInserted(numRows, cachedValues.size()-1);
	}

	public class TypeLoader extends PhonTask {

		private Consumer<Integer> onFinish;

		private final int numToLoad;

		public TypeLoader(int numToLoad, Consumer<Integer> onFinish) {
			this.numToLoad = numToLoad;
			this.onFinish = onFinish;
		}

		@Override
		public void performTask() {
			super.setStatus(TaskStatus.RUNNING);

			int numLoaded = 0;
			List<String> types = new ArrayList<>(10);
			while(typeIterator.hasNext() && numLoaded < numToLoad && !super.isShutdown()) {
				final String type = typeIterator.next();
				types.add(type);
				++numLoaded;
				if(types.size() % 10 == 0) {
					final List<String> typesToAdd = new ArrayList<>(types);
					SwingUtilities.invokeLater(() -> addCachedTypes(typesToAdd));
					types.clear();
				}
			}
			if(types.size() > 0) {
				final List<String> typesToAdd = new ArrayList<>(types);
				SwingUtilities.invokeLater(() -> addCachedTypes(typesToAdd));
				types.clear();
			}

			final int num = numLoaded;
			SwingUtilities.invokeLater(() -> onFinish.accept(num));

			super.setStatus(TaskStatus.FINISHED);
		}


	}

}
