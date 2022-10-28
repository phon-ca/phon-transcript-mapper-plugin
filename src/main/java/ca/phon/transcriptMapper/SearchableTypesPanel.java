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

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.worker.*;
import org.jdesktop.swingx.*;
import org.pushingpixels.substance.internal.contrib.randelshofer.quaqua.colorchooser.ColorSliderTextFieldHandler;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.*;

/**
 * UI providing a searchable table of types in the database. Additional filters may be applied
 * to the type iterator.
 */
public class SearchableTypesPanel extends JPanel {

	private AlignedTypesDatabase db;

	private Predicate<String> typeFilter;

	private PromptedTextField searchField;

	private JXTable typeTable;

	private ButtonGroup btnGrp;
	private JRadioButton startsWithBtn;
	private JRadioButton containsBtn;
	private JRadioButton endsWithBtn;

	private JXBusyLabel busyLabel;

	private PhonTask typeLoader = null;

	private boolean finishedLoad = false;

	public SearchableTypesPanel(AlignedTypesDatabase db, Predicate<String> typeFilter) {
		super();

		this.db = db;
		this.typeFilter = typeFilter;

		init();
	}

	private void init() {
		searchField = new PromptedTextField("Search");
		searchField.getDocument().addDocumentListener(serchFieldListener);

		btnGrp = new ButtonGroup();
		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		startsWithBtn = new JRadioButton("starts with");
		startsWithBtn.setSelected(true);
		btnGrp.add(startsWithBtn);

		containsBtn = new JRadioButton("contains");
		containsBtn.setSelected(false);
		btnGrp.add(containsBtn);

		endsWithBtn = new JRadioButton("ends with");
		endsWithBtn.setSelected(false);
		btnGrp.add(endsWithBtn);

		btnPanel.add(startsWithBtn);
		btnPanel.add(containsBtn);
		btnPanel.add(endsWithBtn);

		final TypeIteratorTableModel tblModel = new TypeIteratorTableModel(db);
		tblModel.setTypeIterator(db.typeIterator((type) -> db.typeExistsInTier(type, "Orthography")));
		typeTable = new JXTable(tblModel);
		typeTable.setSortable(false);
		typeTable.setColumnControlVisible(false);
		typeTable.setVisibleRowCount(20);
		final JScrollPane typeScroller = new JScrollPane(typeTable);

		final int numToLoad = 100;
		final Consumer<Integer> finishLoad = (numLoaded) -> {
			typeLoader = null;
			if(numLoaded != numToLoad) {
				finishedLoad = true;
			}
		};
		typeScroller.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if(finishedLoad) return;
				final int tblRow = typeTable.rowAtPoint(new Point(0, typeScroller.getVerticalScrollBar().getValue()));
				if(tblRow > 0) {
					if(tblRow >= tblModel.getRowCount() - 30) {
						if(typeLoader == null) {
							typeLoader = tblModel.loadItemsAsync(numToLoad, finishLoad);
						}
					}
				}
			}
		});
		typeLoader = tblModel.loadItemsAsync(numToLoad, finishLoad);

		JPanel topPanel = new JPanel(new VerticalLayout());
		topPanel.add(searchField);
		topPanel.add(btnPanel);

		setLayout(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);
		add(typeScroller, BorderLayout.CENTER);
	}

	private DocumentListener serchFieldListener = new DocumentListener() {
		@Override
		public void insertUpdate(DocumentEvent e) {

		}

		@Override
		public void removeUpdate(DocumentEvent e) {

		}

		@Override
		public void changedUpdate(DocumentEvent e) {

		}
	};

}
