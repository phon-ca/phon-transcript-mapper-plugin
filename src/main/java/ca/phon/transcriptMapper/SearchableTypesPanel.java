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
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.util.PrefHelper;
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

	private final static int NUM_TYPES_TO_LOAD = 100;

	private AlignedTypesDatabase db;

	private Predicate<String> typeFilter;

	private PromptedTextField searchField;

	private JXTable typeTable;
	private TypeIteratorTableModel tblModel;

	private final static String PLAIN_TEXT_SEARCH_PROP = TranscriptMapperEditorView.class.getTypeName() + ".plainTextSearch";
	private ButtonGroup btnGrp;
	private JRadioButton startsWithBtn;
	private JRadioButton containsBtn;
	private JRadioButton endsWithBtn;

	private final static String CASE_SENSITIVE_PROP = TranscriptMapperEditorView.class.getName() + ".caseSensitive";
	private JCheckBox caseSensitiveBox;

	private PhonTask typeLoader = null;

	private boolean finishedLoad = false;

	public final static String SELECTED_TYPE = SearchableTypesPanel.class.getTypeName() + ".selectedType";

	private String selectedType;

	public SearchableTypesPanel(AlignedTypesDatabase db, Predicate<String> typeFilter) {
		super();

		this.db = db;
		this.typeFilter = typeFilter;

		init();
	}

	private void init() {
		searchField = new PromptedTextField("Search");
		searchField.getDocument().addDocumentListener(searchFieldListener);
		searchField.setFont(FontPreferences.getTierFont());

		final String plainTextSearch = PrefHelper.get(PLAIN_TEXT_SEARCH_PROP, "starts with");
		btnGrp = new ButtonGroup();
		startsWithBtn = new JRadioButton("starts with");
		startsWithBtn.setSelected(plainTextSearch.equals(startsWithBtn.getText()));
		btnGrp.add(startsWithBtn);

		containsBtn = new JRadioButton("contains");
		containsBtn.setSelected(plainTextSearch.equals(containsBtn.getText()));
		btnGrp.add(containsBtn);

		endsWithBtn = new JRadioButton("ends with");
		endsWithBtn.setSelected(plainTextSearch.equals(endsWithBtn.getText()));
		btnGrp.add(endsWithBtn);

		final ActionListener btnListener = (e) -> {
			PrefHelper.getUserPreferences().put(PLAIN_TEXT_SEARCH_PROP, ((JRadioButton)e.getSource()).getText());
			updateIterator();
		};
		startsWithBtn.addActionListener(btnListener);
		containsBtn.addActionListener(btnListener);
		endsWithBtn.addActionListener(btnListener);

		final boolean caseSensitive = PrefHelper.getBoolean(CASE_SENSITIVE_PROP, true);
		caseSensitiveBox = new JCheckBox("case sensitive");
		caseSensitiveBox.setSelected(caseSensitive);
		caseSensitiveBox.addActionListener((e) -> {
			PrefHelper.getUserPreferences().putBoolean(CASE_SENSITIVE_PROP, caseSensitiveBox.isSelected());
			updateIterator();
		});

		tblModel = new TypeIteratorTableModel(db);
		tblModel.setTypeIterator(db.typeIterator(this::checkTypeFilter));
		typeTable = new JXTable(tblModel);
		typeTable.setFont(FontPreferences.getTierFont());
		typeTable.setSortable(false);
		typeTable.setColumnControlVisible(false);
		typeTable.setVisibleRowCount(10);
		typeTable.getSelectionModel().addListSelectionListener((e) -> {
			if(e.getValueIsAdjusting()) return;
			final String oldSelectedType = this.selectedType;
			final int selectedIdx = typeTable.getSelectedRow();
			if(selectedIdx < 0 || selectedIdx >= tblModel.getRowCount())
				selectedType = null;
			else
				selectedType = (String)tblModel.getValueAt(selectedIdx, 0);
			firePropertyChange(SELECTED_TYPE, oldSelectedType, selectedType);
		});

		final JScrollPane typeScroller = new JScrollPane(typeTable);
		typeScroller.getViewport().addChangeListener((e) -> {
			if(finishedLoad) return;
			final int tblRow = typeTable.rowAtPoint(typeScroller.getViewport().getViewPosition());
			if(tblRow > 0) {
				if(tblRow >= tblModel.getRowCount() - (2 * typeTable.getVisibleRowCount())) {
					if(typeLoader == null) {
						typeLoader = tblModel.loadItemsAsync(NUM_TYPES_TO_LOAD, SearchableTypesPanel.this::onFinishLoad);
					}
				}
			}
		});
		typeLoader = tblModel.loadItemsAsync(NUM_TYPES_TO_LOAD, this::onFinishLoad);

		final JPanel btnPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		btnPanel.add(startsWithBtn, gbc);
		++gbc.gridx;
		btnPanel.add(containsBtn, gbc);
		++gbc.gridx;
		btnPanel.add(endsWithBtn, gbc);
		++gbc.gridx;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		btnPanel.add(Box.createHorizontalGlue(), gbc);
		++gbc.gridy;
		gbc.gridx = 0;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		btnPanel.add(caseSensitiveBox, gbc);
		btnPanel.setOpaque(false);

		JPanel topPanel = new JPanel(new VerticalLayout());
		topPanel.add(searchField);
		topPanel.add(btnPanel);
		topPanel.setOpaque(false);

		setLayout(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);
		add(typeScroller, BorderLayout.CENTER);

		addPropertyChangeListener("db", (e) -> updateIterator());
		addPropertyChangeListener("typeFilter", (e) -> updateIterator());
	}

	public JTable getTypeTable() {
		return this.typeTable;
	}

	public String getSelectedType() {
		return this.selectedType;
	}

	public AlignedTypesDatabase getDb() {
		return this.db;
	}

	public void setDb(AlignedTypesDatabase db) {
		final AlignedTypesDatabase oldDb = this.db;
		this.db = db;
		firePropertyChange("db", oldDb, db);
	}

	public Predicate<String> getTypeFilter() {
		return this.typeFilter;
	}

	public void setTypeFilter(Predicate<String> typeFilter) {
		final Predicate<String> oldFilter = this.typeFilter;
		this.typeFilter = typeFilter;
		firePropertyChange("typeFilter", oldFilter, typeFilter);
	}

	private boolean checkTypeFilter(String type) {
		return (typeFilter != null ? typeFilter.test(type) : true);
	}

	private void onFinishLoad(Integer numLoaded) {
		typeLoader = null;
		if(numLoaded != NUM_TYPES_TO_LOAD) {
			finishedLoad = true;
		}
	}

	public void updateIterator() {
		final String query = searchField.getText();
		final boolean prefixSearch = startsWithBtn.isSelected();
		final boolean containsSearch = containsBtn.isSelected();
		final boolean endsWithSearch = endsWithBtn.isSelected();
		final boolean caseSensitive = caseSensitiveBox.isSelected();

		if(typeLoader != null) {
			typeLoader.shutdown();
		}
		setupIterator(query, prefixSearch, containsSearch, endsWithSearch, caseSensitive);
	}

	private void setupIterator(String query, boolean prefixSearch, boolean containsSearch, boolean endsWithSearch, boolean caseSensitive) {
		if(query.trim().length() == 0) {
			tblModel.setTypeIterator(db.typeIterator(this::checkTypeFilter));
		} else {
			if(prefixSearch) {
				tblModel.setTypeIterator(db.typesWithPrefix(query, caseSensitive, this::checkTypeFilter));
			} else if(containsSearch) {
				tblModel.setTypeIterator(db.typesContaining(query, caseSensitive, this::checkTypeFilter));
			} else if(endsWithSearch) {
				tblModel.setTypeIterator(db.typesWithSuffix(query, caseSensitive, this::checkTypeFilter));
			}
		}
		this.finishedLoad = false;
		typeLoader = tblModel.loadItemsAsync(NUM_TYPES_TO_LOAD, this::onFinishLoad);
	}

	private DocumentListener searchFieldListener = new DocumentListener() {
		@Override
		public void insertUpdate(DocumentEvent e) {
			if(searchField.getState() == PromptedTextField.FieldState.INPUT)
				updateIterator();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			if(searchField.getState() == PromptedTextField.FieldState.INPUT)
				updateIterator();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {

		}
	};

}
