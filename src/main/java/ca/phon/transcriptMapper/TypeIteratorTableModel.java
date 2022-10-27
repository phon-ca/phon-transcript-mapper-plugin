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
import ca.phon.app.log.LogUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Table model for {@link javax.swing.JTable} which accepts a database type iterator
 * for providing data.  Items are loaded in the background up to a specified maximum.
 */
public class TypeIteratorTableModel extends AbstractTableModel {

	private List<String> cachedValues;

	private final AlignedTypesDatabase db;

	private TypeIterator typeIterator;

	public TypeIteratorTableModel(AlignedTypesDatabase db) {
		super();

		this.db = db;
		this.cachedValues = new ArrayList<>();
	}

	public AlignedTypesDatabase getDb() {
		return this.db;
	}

	public void setTypeIterator(TypeIterator itr) {
		this.typeIterator = itr;
	}

	@Override
	public int getRowCount() {
		return this.cachedValues.size();
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	public void loadItemsAsync(int numToLoad, Consumer<Integer> onFinish) {
		final TypeLoader loader = new TypeLoader(numToLoad, onFinish);
		loader.execute();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return this.cachedValues.get(rowIndex);
	}

	private class TypeLoader extends SwingWorker<Integer, String> {

		private Consumer<Integer> onFinish;

		private final int numToLoad;

		public TypeLoader(int numToLoad, Consumer<Integer> onFinish) {
			this.numToLoad = numToLoad;
			this.onFinish = onFinish;
		}

		@Override
		protected Integer doInBackground() throws Exception {
			int numLoaded = 0;
			while(typeIterator.hasNext() && numLoaded < numToLoad) {
				++numLoaded;
				publish(typeIterator.next());
			}
			return numLoaded;
		}

		@Override
		public void process(List<String> types) {
			int numRows = cachedValues.size();
			cachedValues.addAll(types);
			fireTableRowsInserted(numRows, cachedValues.size());
		}

		@Override
		public void done() {
			if(this.onFinish != null) {
				try {
					this.onFinish.accept(get());
				} catch (InterruptedException | ExecutionException e) {
					LogUtil.warning(e);
				}
			}
		}

	}

}
