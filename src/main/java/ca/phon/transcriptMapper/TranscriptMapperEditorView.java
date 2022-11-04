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
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.extensions.UnvalidatedValue;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.*;
import ca.phon.ipadictionary.IPADictionaryLibrary;
import ca.phon.orthography.Orthography;
import ca.phon.project.Project;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.syllabifier.*;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.*;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import org.jdesktop.swingx.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aligned morpheme editor view for Phon sessions. This view will display the aligned morpheme lookup data
 * for the current record and provide method for inserting aligned morpheme data and updating the aligned
 * morpheme database(s).
 *
 */
public final class TranscriptMapperEditorView extends EditorView {

	private JToolBar toolbar;

	private DropDownButton databaseButton;

	private DropDownButton tiersButton;

	private JLabel keyLabel;

	private JComboBox<String> keyTierBox;

	private ButtonGroup modeBtnGrp;
	private JRadioButton searchAndInsertBtn;
	private JRadioButton modifyRecordBtn;

	private SearchableTypesPanel searchableTypesPanel;

	private WordTableModel wordTableModel;

	private JXTable wordTable;

	private JLabel alignmentOptionsLabel;

	private AlignmentOptionsTableModel alignmentOptionsTableModel;

	private JXTable alignmentOptionsTable;

	private TierDataLayoutPanel typeSelectionPanel;

	public final static String NAME = "Transcript Mapper";

	public final static String ICON = "transcript-mapper";

	// current internal state represented in a tree structure
	private TypeMapNode currentState;

	// shared properties
	private final static String SHARED_PROPS_FILE = PrefHelper.getUserDataFolder() + "/transcriptMapper/transcriptMapper.props";

	private static Properties _sharedProps;

	synchronized static Properties getSharedProps() {
		if(_sharedProps == null) {
			try {
				_sharedProps = loadSharedProps();
			} catch (IOException e) {
				_sharedProps = new Properties();
			}
		}
		return _sharedProps;
	}

	private static Properties loadSharedProps() throws IOException {
		final File propsFile = new File(SHARED_PROPS_FILE);
		Properties retVal = new Properties();
		if(propsFile.exists()) {
			retVal.load(new InputStreamReader(new FileInputStream(propsFile)));
		}
		return retVal;
	}

	synchronized static void saveSharedProps() throws IOException {
		final File propsFile = new File(SHARED_PROPS_FILE);
		final File parentFolder = propsFile.getParentFile();
		if(!parentFolder.exists()) {
			parentFolder.mkdirs();
		}
		getSharedProps().store(new OutputStreamWriter(new FileOutputStream(propsFile)), LocalDateTime.now().toString());
	}

	private static String tierVisiblityProp(Project project, SessionPath sessionPath) {
		final String prop = String.format("%s.%s.%s.hiddenTiers",
				project.getUUID(), sessionPath.getCorpus(), sessionPath.getSession());
		return prop;
	}

	private static String dbOnlyTierVisibilityProp(Project project, SessionPath sessionPath) {
		final String prop = String.format("%s.%s.%s.dbOnlyVisibleTiers",
				project.getUUID(), sessionPath.getCorpus(), sessionPath.getSession());
		return prop;
	}

	private synchronized static void setTierVisible(Project project, SessionPath sessionPath,
	                                               String tierName, boolean visible) {
		final Properties sharedProps = getSharedProps();
		final String tierVisiblityProp = tierVisiblityProp(project, sessionPath);

		String hiddenTiers = sharedProps.getProperty(tierVisiblityProp, "");
		final List<String> tierNames = new ArrayList<>();
		if(hiddenTiers.trim().length() > 0)
			tierNames.addAll(List.of(hiddenTiers.split(",")));

		if(visible && tierNames.contains(tierName)) {
			tierNames.remove(tierName);
		} else if(!visible && !tierNames.contains(tierName)) {
			tierNames.add(tierName);
		}

		final String newHiddenTiers = tierNames.stream().collect(Collectors.joining(","));
		sharedProps.setProperty(tierVisiblityProp, newHiddenTiers);

		try {
			saveSharedProps();
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}

	private synchronized static void setDbOnlyTierVisible(Project project, SessionPath sessionPath,
	                                                String tierName, boolean visible) {
		final Properties sharedProps = getSharedProps();
		final String tierVisiblityProp = dbOnlyTierVisibilityProp(project, sessionPath);

		String visibleTiers = sharedProps.getProperty(tierVisiblityProp, "");
		final List<String> tierNames = new ArrayList<>();
		if(visibleTiers.trim().length() > 0)
			tierNames.addAll(List.of(visibleTiers.split(",")));

		if(visible && !visibleTiers.contains(tierName)) {
			tierNames.add(tierName);
		} else if(!visible && visibleTiers.contains(tierName)) {
			tierNames.remove(tierName);
		}

		final String newHiddenTiers = tierNames.stream().collect(Collectors.joining(","));
		sharedProps.setProperty(tierVisiblityProp, newHiddenTiers);

		try {
			saveSharedProps();
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}

	private synchronized static void setTierHidden(Project project, SessionPath sessionPath,
	                                               String tierName, boolean hidden) {
		final Properties sharedProps = getSharedProps();
		final String tierVisiblityProp = tierVisiblityProp(project, sessionPath);

		String hiddenTiers = sharedProps.getProperty(tierVisiblityProp, "");
		final List<String> tierNames = new ArrayList<>();
		if(hiddenTiers.trim().length() > 0)
			tierNames.addAll(List.of(hiddenTiers.split(",")));

		if(!hidden && tierNames.contains(tierName)) {
			tierNames.remove(tierName);
		} else if(hidden && !tierNames.contains(tierName)) {
			tierNames.add(tierName);
		}

		final String newHiddenTiers = tierNames.stream().collect(Collectors.joining(","));
		sharedProps.setProperty(tierVisiblityProp, newHiddenTiers);

		try {
			saveSharedProps();
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}

	private synchronized static boolean isTierHidden(Project project, SessionPath sessionPath, String tierName) {
		final Properties sharedProps = getSharedProps();
		final String tierVisiblityProp = tierVisiblityProp(project, sessionPath);

		String hiddenTiers = sharedProps.getProperty(tierVisiblityProp, "");
		final List<String> tierNames = new ArrayList<>();
		if(hiddenTiers.trim().length() > 0)
			tierNames.addAll(List.of(hiddenTiers.split(",")));

		return tierNames.contains(tierName);
	}

	private synchronized static boolean isDbOnlyTierVisible(Project project, SessionPath sessionPath, String tierName) {
		final Properties sharedProps = getSharedProps();
		final String tierVisiblityProp = dbOnlyTierVisibilityProp(project, sessionPath);

		String visibleTiers = sharedProps.getProperty(tierVisiblityProp, "");
		final List<String> tierNames = new ArrayList<>();
		if(visibleTiers.trim().length() > 0)
			tierNames.addAll(List.of(visibleTiers.split(",")));

		return tierNames.contains(tierName);
	}

	public TranscriptMapperEditorView(SessionEditor editor) {
		super(editor);

		init();

		final PropertyChangeListener changeListener = (e) -> {
			updateDatabaseButtonState();
		};
		if(UserATDB.getInstance().isATDBLoaded()) {
			updateAfterDbLoad();
			UserATDB.getInstance().addPropertyChangeListener("modified", changeListener);
		} else {
			UserATDB.getInstance().addPropertyChangeListener("loaded", new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					SwingUtilities.invokeLater(() -> updateAfterDbLoad());
					UserATDB.getInstance().removePropertyChangeListener("loaded", this);
					UserATDB.getInstance().addPropertyChangeListener("modified", changeListener);
				}
			});
		}

		setupEditorEvenListeners();
	}

	private void setupEditorEvenListeners() {
		getEditor().getEventManager().registerActionForEvent(EditorEventType.TierChanged, this::onTierChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		getEditor().getEventManager().registerActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
	}

	private void onRecordChanged(EditorEvent<EditorEventType.RecordChangedData> ee) {
		updateStateAsync(this::updateFromCurrentState);
	}

	private void onTierChanged(EditorEvent<EditorEventType.TierChangeData> ee) {
		final int selectedRow = this.wordTable.getSelectedRow();
		final int selectedOption = this.alignmentOptionsTable.getSelectedRow();
		updateStateAsync(() -> {
			this.updateFromCurrentState();
			if(selectedRow >= 0 && selectedRow < this.wordTableModel.getRowCount()) {
				SwingUtilities.invokeLater(() -> {
					this.wordTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
					if(selectedOption >= 0) {
						SwingUtilities.invokeLater(() -> {
							if(selectedOption < this.alignmentOptionsTable.getRowCount()) {
								this.alignmentOptionsTable.getSelectionModel().setSelectionInterval(selectedOption, selectedOption);
							}
						});
					}
				});
			}
		});
	}

	private void onTierViewChanged(EditorEvent<EditorEventType.TierViewChangedData> ee) {
		if (this.alignmentOptionsTableModel != null)
			this.alignmentOptionsTableModel.fireTableStructureChanged();
		if (this.wordTableModel != null) {
			this.wordTableModel.fireTableStructureChanged();
		}
		updateAfterDbLoad();
	}

	AlignedTypesDatabase getUserDb() {
		final UserATDB userATDB = UserATDB.getInstance();
		if(!userATDB.isATDBLoaded()) {
			return (new AlignedTypesDatabaseFactory()).createDatabase();
		} else {
			return userATDB.getATDB();
		}
	}

	void saveUserDbAsync(Runnable onFinish) {
		final PhonTask task = PhonWorker.invokeOnNewWorker(this::saveUserDb, onFinish, LogUtil::warning);
		task.setName("Saving aligned morpheme database");
		getEditor().getStatusBar().watchTask(task);
		SwingUtilities.invokeLater(() -> {
			getEditor().getStatusBar().watchTask(task);
		});
	}

	private void saveUserDb() {
		final UserATDB userATDB = UserATDB.getInstance();
		if(userATDB != null) {
			try {
				userATDB.saveDb();
			} catch (IOException e) {
				LogUtil.severe(e);
			}
		}
	}

	private void setupToolbar() {
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		add(toolbar, BorderLayout.NORTH);

		final JPopupMenu dbMenu = new JPopupMenu("Database");
		dbMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				dbMenu.removeAll();
				final MenuBuilder dbMenuBuilder = new MenuBuilder(dbMenu);
				setupDatabaseMenu(dbMenuBuilder);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		});

		PhonUIAction<Void> dbMenuAct = PhonUIAction.runnable(() -> {});
		dbMenuAct.putValue(PhonUIAction.NAME, "Database");
		dbMenuAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show database menu");
		dbMenuAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("development-database", IconSize.SMALL));
		dbMenuAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		dbMenuAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		dbMenuAct.putValue(DropDownButton.BUTTON_POPUP, dbMenu);

		final JPopupMenu tiersMenu = new JPopupMenu("Tiers");
		final MenuBuilder tiersMenuBuilder = new MenuBuilder(tiersMenu);
		tiersMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				tiersMenu.removeAll();
				setupTiersMenu(tiersMenuBuilder);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		});

		PhonUIAction<Void> tiersMenuAct = PhonUIAction.runnable(() -> {});
		tiersMenuAct.putValue(PhonUIAction.NAME, "Tiers");
		tiersMenuAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show tiers menu");
		tiersMenuAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("misc/record", IconSize.SMALL));
		tiersMenuAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		tiersMenuAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		tiersMenuAct.putValue(DropDownButton.BUTTON_POPUP, tiersMenu);

		databaseButton = new DropDownButton(dbMenuAct);
		databaseButton.setOnlyPopup(true);

		tiersButton = new DropDownButton(tiersMenuAct);
		tiersButton.setOnlyPopup(true);

		toolbar.add(databaseButton);
		toolbar.add(tiersButton);
	}

	private void setupDatabaseMenu(MenuBuilder builder) {
		final SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(this);
		if(UserATDB.getInstance().isSaving())
			saveDatabaseAction.putValue(PhonUIAction.NAME, "Saving...");
		builder.addItem(".", saveDatabaseAction).setEnabled(UserATDB.getInstance().isModified() && !UserATDB.getInstance().isSaving());
		builder.addSeparator(".", "save");
		builder.addItem(".", new ScanProjectAction(this));
		builder.addSeparator(".", "scan");
		builder.addItem(".", new ImportCSVAction(this));
		builder.addItem(".", new ExportCSVAction(this));
		builder.addSeparator(".", "csv");

		final JMenu importDictMenu = builder.addMenu(".", "Import IPA dictionary");
		final MenuBuilder importBuilder = new MenuBuilder(importDictMenu);
		setupImportDictionaryMenu(importBuilder);

		// TODO Fix database import
//		builder.addSeparator(".", "import_dict");
//		builder.addItem(".", new ImportDatabaseAction(this));
	}

	private void setupImportDictionaryMenu(MenuBuilder builder) {
		for(Language dictLang:IPADictionaryLibrary.getInstance().availableLanguages()) {
			builder.addItem(".", new ImportIPADictionaryAction(this, dictLang));
		}
	}

	private void setupTiersMenu(MenuBuilder builder) {
		final List<String> allTiers = allTiers();

		final List<String> dbOnlyTiers = new ArrayList<>();
		// tier visibility menu
		for(String tierName:allTiers) {
			if(sessionHasTier(tierName)) {
				final PhonUIAction<String> toggleTierVisibleAct = PhonUIAction.consumer(this::toggleTier, tierName);
				toggleTierVisibleAct.putValue(PhonUIAction.NAME, tierName);
				toggleTierVisibleAct.putValue(PhonUIAction.SHORT_DESCRIPTION, String.format("Toggle tier %s", tierName));
				toggleTierVisibleAct.putValue(PhonUIAction.SELECTED_KEY, dbTierVisible(tierName));
				final JCheckBoxMenuItem toggleTierItem = new JCheckBoxMenuItem(toggleTierVisibleAct);
				toggleTierItem.setEnabled(!tierName.equals(keyTier()));
				if(!isGroupedTier(tierName)) {
					toggleTierItem.setEnabled(false);
					toggleTierItem.setToolTipText(String.format("%s is not a group tier", tierName));
				} else if(!sessionTierVisible(tierName)) {
					toggleTierItem.setEnabled(false);
					toggleTierItem.setToolTipText(String.format("%s is not visible in record data", tierName));
				}
				builder.addItem(".", toggleTierItem);
			} else {
				if(!tierName.startsWith("__"))
					dbOnlyTiers.add(tierName);
			}
		}

		if(dbOnlyTiers.size() > 0) {
			builder.addSeparator(".", "create_tiers");
			final String missingTiersMsg = String.format("<html><body><div style='background: #f3ca4f; " +
					"border: 1px solid gray; border-radius: 10; padding: 5'>The database contains %d tier(s) " +
					"not present in the session<div></body></html>", dbOnlyTiers.size());
			final JMenuItem msgItem = new JMenuItem(missingTiersMsg);
			msgItem.setEnabled(false);
			msgItem.setForeground(Color.black);
			builder.addItem(".", msgItem);

			for(String tierName:dbOnlyTiers) {
				final JMenu dbOnlyTierMenu = builder.addMenu(".", tierName);
				final MenuBuilder dbOnlyTierMenuBuilder = new MenuBuilder(dbOnlyTierMenu);

				final PhonUIAction<List<String>> createTierAct = PhonUIAction.eventConsumer(this::createTiers, List.of(tierName));
				createTierAct.putValue(PhonUIAction.NAME, "Add tier to session");
				createTierAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Add tier " + tierName + " to session");
				dbOnlyTierMenuBuilder.addItem(".", createTierAct);

				final PhonUIAction<String> showTierAct = PhonUIAction.eventConsumer(this::toggleDbOnlyTier, tierName);
				showTierAct.putValue(PhonUIAction.NAME, "Show tier in alignment options table");
				showTierAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show tier '" + tierName + "' in alignment options table");
				showTierAct.putValue(PhonUIAction.SELECTED_KEY, dbOnlyTierVisible(tierName));
				final JCheckBoxMenuItem showTierItem = new JCheckBoxMenuItem(showTierAct);
				dbOnlyTierMenuBuilder.addItem(".", showTierItem);
			}

			if(dbOnlyTiers.size() > 1) {
				final PhonUIAction<List<String>> createAllTiersAct = PhonUIAction.eventConsumer(this::createTiers, dbOnlyTiers);
				createAllTiersAct.putValue(PhonUIAction.NAME, "Add all missing tiers");
				createAllTiersAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Add all database tiers missing from session");
				builder.addItem(".", createAllTiersAct);
			}
		}
	}

	private void init() {
		setLayout(new BorderLayout());
		setupToolbar();

		this.keyTierBox = new JComboBox<>();
		this.keyTierBox.addItemListener(e -> {
			updateStateAsync(this::updateFromCurrentState);
			if(this.wordTableModel != null)
				this.wordTableModel.fireTableStructureChanged();
			if(this.alignmentOptionsTableModel != null)
				this.alignmentOptionsTableModel.fireTableStructureChanged();
			if(this.searchableTypesPanel != null)
				this.searchableTypesPanel.updateIterator();
		});

		typeSelectionPanel = new TierDataLayoutPanel();

		int row = 0;
		keyLabel = new JLabel("Key tier");
		keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		keyLabel.setFont(FontPreferences.getTitleFont());
		typeSelectionPanel.add(keyLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));
		typeSelectionPanel.add(keyTierBox, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		++row;
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		typeSelectionPanel.add(sep, new TierDataConstraint(TierDataConstraint.FULL_TIER_COLUMN, row));

		++row;
		modeBtnGrp = new ButtonGroup();
		modifyRecordBtn = new JRadioButton("Morpheme list");
		modifyRecordBtn.setFont(FontPreferences.getTitleFont());
		modifyRecordBtn.setSelected(true);
		modeBtnGrp.add(modifyRecordBtn);

		searchAndInsertBtn = new JRadioButton("Search");
		searchAndInsertBtn.setSelected(false);
		searchAndInsertBtn.setFont(FontPreferences.getTitleFont());
		modeBtnGrp.add(searchAndInsertBtn);

		final JPanel modePanel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		modePanel.setOpaque(false);
		modePanel.add(modifyRecordBtn, gbc);
		++gbc.gridy;
		modePanel.add(searchAndInsertBtn, gbc);
		typeSelectionPanel.add(modePanel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));

		setupWordTable();
		final JScrollPane morphemeTableScroller = new JScrollPane(wordTable);
		final int morphemeTableRow = row;
		typeSelectionPanel.add(morphemeTableScroller, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, morphemeTableRow));

		setupSearchPanel();

		final ActionListener modeListener = (e) -> {
			if(searchAndInsertBtn.isSelected()) {
				typeSelectionPanel.remove(morphemeTableScroller);
				typeSelectionPanel.add(searchableTypesPanel, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, morphemeTableRow));
			} else {
				typeSelectionPanel.remove(searchableTypesPanel);
				typeSelectionPanel.add(morphemeTableScroller, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, morphemeTableRow));
			}
			revalidate();
			repaint();
		};
		modifyRecordBtn.addActionListener(modeListener);
		searchAndInsertBtn.addActionListener(modeListener);

		++row;
		alignmentOptionsLabel = new JLabel("Alignment Options");
		alignmentOptionsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		alignmentOptionsLabel.setFont(FontPreferences.getTitleFont());
		typeSelectionPanel.add(alignmentOptionsLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));

		setupAlignmentOptionsTable();
		typeSelectionPanel.add(new JScrollPane(alignmentOptionsTable), new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		add(new JScrollPane(typeSelectionPanel), BorderLayout.CENTER);
	}

	private void setupSearchPanel() {
		searchableTypesPanel = new SearchableTypesPanel(new AlignedTypesDatabaseFactory().createDatabase(),
				(type) -> getUserDb().typeExistsInTier(type, keyTier()));
		searchableTypesPanel.addPropertyChangeListener(SearchableTypesPanel.SELECTED_TYPE, (e) -> {
			updateAlignmentOptions();
		});
		searchableTypesPanel.setOpaque(false);

		final JTable typeTable = searchableTypesPanel.getTypeTable();
		final MouseAdapter ctxHandler = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showCtxMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showCtxMenu(e);
				}
			}

			private void showCtxMenu(MouseEvent e) {
				final int row = typeTable.rowAtPoint(e.getPoint());
				if(row >= 0 && row < typeTable.getRowCount()) {
					typeTable.getSelectionModel().setSelectionInterval(row, row);

					final String type = (String)typeTable.getModel().getValueAt(row, 0);
					final Map<String, String[]> alignedTypes = getUserDb().alignedTypesForTier(keyTier(), type);
					final String[][] optionsForMorpheme = alignmentOptionsForType(type, alignedTypes);
					final JPopupMenu menu = new JPopupMenu();
					setupWordMenu(new MenuBuilder(menu), currentState.getLeafCount(), optionsForMorpheme);
					menu.show(typeTable, e.getX(), e.getY());
				}
			}

		};
		typeTable.addMouseListener(ctxHandler);
	}

	private void setupWordTable() {
		wordTableModel = new WordTableModel();
		wordTable = new JXTable(wordTableModel) {
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				// alternate row color based on group index
				if (!isRowSelected(row))
					c.setBackground(tableRowToGroupIndex(row) % 2 == 0 ? getBackground() : PhonGuiConstants.PHON_UI_STRIP_COLOR);
				return c;
			}
		};
		wordTable.setFont(FontPreferences.getTierFont());
		wordTable.setDefaultRenderer(Object.class, new WordTableCellRenderer());
		wordTable.setSortable(false);
		wordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		wordTable.setVisibleRowCount(10);

		wordTable.getSelectionModel().addListSelectionListener(e -> {
			updateAlignmentOptions();
		});

		final MouseAdapter ctxHandler = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showCtxMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showCtxMenu(e);
				}
			}

			private void showCtxMenu(MouseEvent e) {
				final int row = wordTable.rowAtPoint(e.getPoint());
				if(row >= 0 && row < wordTable.getRowCount()) {
					wordTable.getSelectionModel().setSelectionInterval(row, row);

					final String[][] optionsForMorpheme = alignmentOptionsForWord(row);
					final JPopupMenu menu = new JPopupMenu();
					setupWordMenu(new MenuBuilder(menu), row, optionsForMorpheme);
					menu.show(wordTable, e.getX(), e.getY());
				}
			}

		};
		wordTable.addMouseListener(ctxHandler);

		final InputMap inputMap = wordTable.getInputMap(JComponent.WHEN_FOCUSED);
		final ActionMap actionMap = wordTable.getActionMap();

		final PhonUIAction<Void> showMorphemeMenuAction = PhonUIAction.eventConsumer(this::showWordMenu);
		showMorphemeMenuAction.putValue(PhonUIAction.NAME, "Show menu for selected word/morpheme");
		final String morphemeMenuActId = "show_morpheme_menu";
		actionMap.put(morphemeMenuActId, showMorphemeMenuAction);
		final KeyStroke showMorphemeMenuKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(showMorphemeMenuKs, morphemeMenuActId);

		final PhonUIAction<Integer> focusAct = PhonUIAction.consumer(this::onFocusAlignmentOptions, 0);
		final String focusActId = "focus_alignment_options";
		final KeyStroke focusKs = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		actionMap.put(focusActId, focusAct);
		inputMap.put(focusKs, focusActId);
	}

	private void setupAlignmentOptionsTable() {
		alignmentOptionsTableModel = new AlignmentOptionsTableModel();
		alignmentOptionsTable = new JXTable(alignmentOptionsTableModel) {
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				// alternate row color based on group index
				if (!isRowSelected(row))
					c.setBackground(row % 2 == 0 ? getBackground() : PhonGuiConstants.PHON_UI_STRIP_COLOR);
				return c;
			}
		};
		alignmentOptionsTable.setFont(FontPreferences.getTierFont());
		alignmentOptionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		alignmentOptionsTable.setVisibleRowCount(10);
		alignmentOptionsTable.setSortable(false);

		final MouseAdapter ctxHandler = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showCtxMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger()) {
					showCtxMenu(e);
				}
			}

			private void showCtxMenu(MouseEvent e) {
				final int row = alignmentOptionsTable.rowAtPoint(e.getPoint());
				if(row >= 0 && row < alignmentOptionsTable.getRowCount()) {
					alignmentOptionsTable.getSelectionModel().setSelectionInterval(row, row);

					final JPopupMenu menu = new JPopupMenu();
					int morphemeIdx =
							(modifyRecordBtn.isSelected() ? wordTable.getSelectedRow() : currentState.getLeafCount());
					setupAlignmentOptionsMenu(new MenuBuilder(menu), morphemeIdx, row, alignmentOptionsTableModel.alignmentRows);
					menu.show(alignmentOptionsTable, e.getX(), e.getY());
				}
			}

		};
		alignmentOptionsTable.addMouseListener(ctxHandler);

		final InputMap inputMap = alignmentOptionsTable.getInputMap(JComponent.WHEN_FOCUSED);
		final ActionMap actionMap = alignmentOptionsTable.getActionMap();

		final PhonUIAction<Void> showAlignmentOptionsMenuAct = PhonUIAction.eventConsumer(this::showAlignmentOptionsMenu);
		final String alignmentOptionsMenuActId = "show_alignment_options_menu";
		actionMap.put(alignmentOptionsMenuActId, showAlignmentOptionsMenuAct);
		final KeyStroke showAlignmentOptionsMenuKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(showAlignmentOptionsMenuKs, alignmentOptionsMenuActId);

		final PhonUIAction<Integer> focusAct = PhonUIAction.eventConsumer(this::onFocusWordTable, 0);
		final String focusActId = "focus_morpheme_table";
		final KeyStroke focusKs = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		actionMap.put(focusActId, focusAct);
		inputMap.put(focusKs, focusActId);

		final PhonUIAction<Void> deleteAlignedTypesAct = PhonUIAction.eventConsumer(this::onDeleteAlignedTypes);
		final String deleteAlignedTypesId = "delete_aligned_types";
		actionMap.put(deleteAlignedTypesId, deleteAlignedTypesAct);
		final KeyStroke deleteAlignedTypesKs = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		final KeyStroke deleteAlignedTypesKs2 = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		inputMap.put(deleteAlignedTypesKs, deleteAlignedTypesId);
		inputMap.put(deleteAlignedTypesKs2, deleteAlignedTypesId);

		for(int i = 1; i < 10; i++) {
			final PhonUIAction<Integer> selectTypeAction = PhonUIAction.consumer(this::onSelectTierOption, Integer.valueOf(i));
			final String selectTypeId = "select_tier_option_" + i;
			final KeyStroke selectTypeKs = KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0);
			actionMap.put(selectTypeId, selectTypeAction);
			inputMap.put(selectTypeKs, selectTypeId);
		}
	}

	private void setState(TypeMapNode state) {
		this.currentState = state;
	}

	private void updateStateAsync(Runnable onFinish) {
		PhonWorker.invokeOnNewWorker(this::updateState, onFinish);
	}

	private void updateState() {
		setState(stateFromRecord(getEditor().currentRecord()));
	}

	private void updateFromCurrentState() {
		final Record record = getEditor().currentRecord();
		if(record == null || currentState == null) return;

		if(wordTableModel != null)
			wordTableModel.fireTableDataChanged();
		updateAlignmentOptions();
	}

	private void updateAlignmentOptions() {
		if(modifyRecordBtn.isSelected()) {
			final int selectedMorpheme = wordTable.getSelectedRow();
			if(selectedMorpheme >= 0) {
				final String[][] alignmentOptions = alignmentOptionsForWord(selectedMorpheme);
				alignmentOptionsTableModel.setAlignmentRows(alignmentOptions);
			} else {
				alignmentOptionsTableModel.setAlignmentRows(new String[0][]);
			}
		} else if(searchAndInsertBtn.isSelected()) {
			String morpheme = searchableTypesPanel.getSelectedType();
			if(morpheme == null) morpheme = "";
			List<String> tierList = getVisibleOptionsTiers();
			Map<String, String[]> alignedTypes =
					getUserDb().alignedTypesForTier(keyTier(), morpheme, tierList);
			final String[][] alignmentOptions = alignmentOptionsForType(morpheme, alignedTypes);
			alignmentOptionsTableModel.setAlignmentRows(alignmentOptions);
		}
	}
	public void updateDatabaseButtonState() {
		if(UserATDB.getInstance().isModified())
			databaseButton.setText("Database *");
		else
			databaseButton.setText("Database");
	}

	void updateAfterDbLoad() {
		final AlignedTypesDatabase db = getUserDb();
		if(db == null) return;

		final String prevKeyTier = (this.keyTierBox.getSelectedItem() != null ?
				this.keyTierBox.getSelectedItem().toString() : null);
		final List<String> alignmentTiers = getVisibleAlignmentTiers();
		this.keyTierBox.setModel(new DefaultComboBoxModel<>(alignmentTiers.toArray(new String[0])));
		if(prevKeyTier != null && alignmentTiers.contains(prevKeyTier))
			this.keyTierBox.setSelectedItem(prevKeyTier);
		else
			this.keyTierBox.setSelectedItem(SystemTierType.Orthography.getName());

		searchableTypesPanel.setDb(db);

		if(getEditor().currentRecord() != null) {
			updateStateAsync(() -> {
				SwingUtilities.invokeLater(() -> {
					if (this.alignmentOptionsTableModel != null)
						this.alignmentOptionsTableModel.fireTableStructureChanged();
					if (this.wordTableModel != null)
						this.wordTableModel.fireTableStructureChanged();
					this.updateFromCurrentState();
				});
			});
		}
	}

	/**
	 * Called after adding or removing aligned types to the database retains table selection
	 *
	 */
	void updateAfterDbChange() {
		final int selectedMorpheme = this.wordTable.getSelectedRow();
		final int selectedOption = this.alignmentOptionsTable.getSelectedRow();

		updateStateAsync(() -> {
			updateFromCurrentState();
			SwingUtilities.invokeLater(() -> {
				if(selectedMorpheme >= 0 && selectedMorpheme < this.wordTableModel.getRowCount()) {
					this.wordTable.getSelectionModel().setSelectionInterval(selectedMorpheme, selectedMorpheme);
					if(selectedOption >= 0) {
						SwingUtilities.invokeLater(() -> {
							if(selectedOption < this.alignmentOptionsTableModel.getRowCount()) {
								this.alignmentOptionsTable.getSelectionModel().setSelectionInterval(selectedOption, selectedOption);
							}
						});
					}
				}
			});
		});
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ImageIcon getIcon() {
		return IconManager.getInstance().getIcon(ICON, IconSize.SMALL);
	}

	@Override
	public JMenu getMenu() {
		JMenu retVal = new JMenu();
		MenuBuilder builder = new MenuBuilder(retVal);
		setupDatabaseMenu(builder);

		builder.addSeparator(".", "tiers");
		final JMenu tiersMenu = builder.addMenu(".", "Tiers");
		tiersMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				tiersMenu.removeAll();
				setupTiersMenu(new MenuBuilder(tiersMenu));
			}

			@Override
			public void menuDeselected(MenuEvent e) {

			}

			@Override
			public void menuCanceled(MenuEvent e) {

			}
		});

		return retVal;
	}

	private void updateRecord(int morphemeIdx, String[] tiers, String[] selectedTypes) {
		boolean needsRefresh = false;
		getEditor().getUndoSupport().beginUpdate();
		for(int i = 0; i < selectedTypes.length; i++) {
			needsRefresh |= updateTier(morphemeIdx, tiers[i], selectedTypes[i]);
		}
		getEditor().getUndoSupport().endUpdate();
		if(needsRefresh) {
			final EditorEvent<EditorEventType.RecordChangedData> ee =
					new EditorEvent<>(EditorEventType.RecordRefresh, this,
							new EditorEventType.RecordChangedData(getEditor().getCurrentRecordIndex(), getEditor().currentRecord()));
			getEditor().getEventManager().queueEvent(ee);
		}
	}

	private boolean updateTier(int wordIndex, String tier, String selectedWord) {
		if(selectedWord.length() == 0) return false;

		boolean needsRefresh = false;
		int gIdx = tableRowToGroupIndex(wordIndex);
		int wIdx = 0;
		for(int i = 0; i < gIdx; i++)
			wIdx += currentState.getChild(i).getLeafCount();
		final TypeMapNode groupNode = this.currentState.getChild(gIdx);
		final StringBuilder builder = new StringBuilder();
		for(int childIndex = 0; childIndex < groupNode.childCount(); childIndex++) {
			if(childIndex > 0) builder.append(' ');
			TypeMapNode childNode = groupNode.getChild(childIndex);

			if(wIdx++ == wordIndex) {
				builder.append(selectedWord);
			} else {
				final String cval = childNode.getType(tier);
				if(cval.length() > 0)
					builder.append(childNode.getType(tier));
				else
					builder.append('*');
			}
		}

		final Record currentRecord = getEditor().currentRecord();
		final SystemTierType systemTier = SystemTierType.tierFromString(tier);
		if(systemTier != null) {
			switch(systemTier) {
				case Orthography -> {
					final Orthography currentOrtho = currentRecord.getOrthography().getGroup(gIdx);
					int currentWordIndex = 0;
					for(int i = 0; i < gIdx; i++) {
						Group group = currentRecord.getGroup(i);
						currentWordIndex += group.getAlignedWordCount();
					}
					final OrthographyWordReplacementVisitor visitor = new OrthographyWordReplacementVisitor(wordIndex, selectedWord, currentWordIndex);
					currentOrtho.accept(visitor);
					final Orthography newOrtho = visitor.getOrthography();
					if(!currentOrtho.toString().equals(newOrtho.toString())) {
						final TierEdit<Orthography> edit =
								new TierEdit<>(getEditor(), currentRecord.getOrthography(), gIdx, newOrtho);
						edit.setFireHardChangeOnUndo(true);
						getEditor().getUndoSupport().postEdit(edit);
					}
				}

				case IPATarget, IPAActual -> {
					final Tier<IPATranscript> ipaTier = systemTier == SystemTierType.IPATarget
							? currentRecord.getIPATarget()
							: currentRecord.getIPAActual();
					final IPATranscript currentIpa = ipaTier.getGroup(gIdx);
					if (!currentIpa.toString().equals(builder.toString())) {
						try {
							final IPATranscript newIpa = IPATranscript.parseIPATranscript(builder.toString());
							// update syllabification
							Syllabifier syllabifier = SyllabifierLibrary.getInstance().defaultSyllabifier();
							final SyllabifierInfo syllabifierInfo = getEditor().getExtension(SyllabifierInfo.class);
							if (syllabifierInfo != null) {
								final Syllabifier s = SyllabifierLibrary.getInstance().getSyllabifierForLanguage(
										syllabifierInfo.getSyllabifierLanguageForTier(tier));
								if (s != null)
									syllabifier = s;
							}
							syllabifier.syllabify(newIpa.toList());
							final TierEdit<IPATranscript> edit =
									new TierEdit<>(getEditor(), ipaTier, gIdx, newIpa);
							edit.setFireHardChangeOnUndo(true);
							getEditor().getUndoSupport().postEdit(edit);
						} catch (ParseException e) {
							LogUtil.warning(e);
							final IPATranscript ipa = new IPATranscript();
							final UnvalidatedValue uv = new UnvalidatedValue(builder.toString(), e);
							ipa.putExtension(UnvalidatedValue.class, uv);
							final TierEdit<IPATranscript> edit =
									new TierEdit<>(getEditor(), ipaTier, gIdx, ipa);
							getEditor().getUndoSupport().postEdit(edit);
						}

						// update alignment
						final PhoneMap newAlignment = (new PhoneAligner()).calculatePhoneAlignment(
								currentRecord.getIPATarget().getGroup(gIdx), currentRecord.getIPAActual().getGroup(gIdx));
						final TierEdit<PhoneMap> edit =
								new TierEdit<>(getEditor(), currentRecord.getPhoneAlignment(), gIdx, newAlignment);
						edit.setFireHardChangeOnUndo(true);
						getEditor().getUndoSupport().postEdit(edit);
					}
				}
			}
		} else {
			final Tier<TierString> userTier = currentRecord.getTier(tier, TierString.class);
			final TierString currentVal = (gIdx < userTier.numberOfGroups() ? userTier.getGroup(gIdx) : new TierString());
			needsRefresh = userTier.numberOfGroups() <= gIdx;
			if(!currentVal.toString().equals(builder.toString())) {
				final TierEdit<TierString> edit =
						new TierEdit<>(getEditor(), userTier, gIdx, new TierString(builder.toString()));
				edit.setFireHardChangeOnUndo(true);
				getEditor().getUndoSupport().postEdit(edit);
			}
		}
		return needsRefresh;
	}

	private TypeMapNode stateFromRecord(Record record) {
		final TypeMapNode root = new TypeMapNode(-1);

		final AlignedTypesDatabase db = getUserDb();
		if(db == null) return root;

		final String keyTier = keyTier();
		if(keyTier == null) return root;

		List<String> tierList = getVisibleOptionsTiers();
		for(int gidx = 0; gidx < record.numberOfGroups(); gidx++) {
			Group grp = record.getGroup(gidx);
			TypeMapNode grpNode = new TypeMapNode(gidx);
			root.addChild(grpNode);
			for(int widx = 0; widx < grp.getAlignedWordCount(); widx++) {
				Word wrd = grp.getAlignedWord(widx);
				Map<String, String> currentWords = new HashMap<>();
				for(String tierName:tierList) {
					Object tierValue = wrd.getTier(tierName);
					currentWords.put(tierName, tierValue != null ? tierValue.toString() : "");
				}
				Map<String, String[]> alignedTypes = db.alignedTypesForTier(keyTier, currentWords.get(keyTier), tierList);

				TypeMapNode wrdNode = new TypeMapNode(widx, currentWords, alignedTypes);
				grpNode.addChild(widx == 0 ? '\u0000' : ' ', wrdNode);

//				AlignedMorphemes morphemes = wrd.getExtension(AlignedMorphemes.class);
//				if(morphemes != null) {
//					for(int midx = 0; midx < morphemes.getMorphemeCount(); midx++) {
//						AlignedMorpheme morpheme = morphemes.getAlignedMorpheme(midx);
//
//						Map<String, String> currentMorphemes = new HashMap<>();
//						for(String tierName:tierList) {
//							currentMorphemes.put(tierName, morpheme.getMorphemeText(tierName));
//						}
//						Map<String, String[]> alignedTypes =
//								db.alignedTypesForTier(keyTier, currentMorphemes.get(keyTier), tierList);
//
//						TypeMapNode morphemeNode = new TypeMapNode(midx, currentMorphemes, alignedTypes);
//
//						// start of word
//						char ch = '\u0000';
//						if(midx > 0) {
//							int orthoIdx = morpheme.getOrthographyWordLocation();
//							int chIdx = orthoIdx - 1;
//							ch = (chIdx >= 0 ? wrd.getOrthography().toString().charAt(chIdx) : '\u0000');
//						}
//
//						wrdNode.addChild(ch, morphemeNode);
//					}
//				}
			}
		}

		return root;
	}

	/**
	 * Get the selected key tier (or Orthography)
	 *
	 * @return selected key tier
	 */
	public String keyTier() {
		return (keyTierBox.getSelectedItem() != null ? keyTierBox.getSelectedItem().toString()
				: SystemTierType.Orthography.getName());
	}

	/**
	 * Set key tier, no action if key tier is not in list
	 *
	 * @param keyTier
	 */
	public void setKeyTier(String keyTier) {
		keyTierBox.setSelectedItem(keyTier);
	}

	/**
	 * Toggle database visibility of specified tier
	 *
	 * @param tierName
	 */
	public void toggleTier(String tierName) {
		final Project project = getEditor().getProject();
		final SessionPath sp = new SessionPath(getEditor().getSession().getCorpus(), getEditor().getSession().getName());
		setTierHidden(project, sp, tierName, !isTierHidden(project, sp, tierName));
		SwingUtilities.invokeLater(this::updateAfterDbLoad);
	}

	/**
	 * Create the given list of tiers in the session
	 *
	 * @param pae
	 */
	public void createTiers(PhonActionEvent<List<String>> pae) {
		final List<String> tierList = pae.getData();
		final SessionFactory factory = SessionFactory.newFactory();
		getEditor().getUndoSupport().beginUpdate();
		for(String tierName:tierList) {
			final AddTierEdit tierEdit = new AddTierEdit(getEditor(),
					factory.createTierDescription(tierName, true, TierString.class),
					factory.createTierViewItem(tierName));
			getEditor().getUndoSupport().postEdit(tierEdit);
		}
		getEditor().getUndoSupport().endUpdate();
	}

	public void toggleDbOnlyTier(PhonActionEvent<String> pae) {
		final String tierName = pae.getData();
		setDbOnlyTierVisible(getEditor().getProject(), new SessionPath(getEditor().getSession().getCorpus(), getEditor().getSession().getName()),
				tierName, !dbOnlyTierVisible(tierName));
		if(this.alignmentOptionsTableModel != null) {
			this.updateState();
			this.alignmentOptionsTableModel.fireTableStructureChanged();
			this.updateFromCurrentState();
		}
	}

	public void onFocusWordTable(PhonActionEvent<Integer> pae) {
		if(this.wordTable != null)
			this.wordTable.requestFocusInWindow();
	}

	public void onAddAlignedTypes(PhonActionEvent<Void> pae) {
		int selectedWord = this.wordTable.getSelectedRow();
		if(selectedWord >= 0 && selectedWord < this.wordTableModel.getRowCount()) {
			final TypeMapNode wordNode = this.currentState.getLeaves().get(selectedWord);
			final Map<String, String> alignedTypes = new LinkedHashMap<>();
			for(String tierName:getVisibleAlignmentTiers()) {
				final String word = wordNode.getType(tierName);
				alignedTypes.put(tierName, "*".equals(word) ? "" : wordNode.getType(tierName));
			}

			final String sessionLanguages = getEditor().getSession().getLanguage();
			if(!alignedTypes.containsKey(TypeMapMetadataTier.LANGUAGE.getTierName())) {
				if(sessionLanguages != null && sessionLanguages.length() > 0) {
					String[] langIds = sessionLanguages.split(",");
					if (langIds.length > 0) {
						LanguageEntry primaryLang = LanguageParser.getInstance().getEntryById(langIds[0]);
						if (primaryLang != null) {
							alignedTypes.put(TypeMapMetadataTier.LANGUAGE.getTierName(), primaryLang.getId());
						}
					}
				} else {
					// use syllabifier language
					alignedTypes.put(TypeMapMetadataTier.LANGUAGE.getTierName(),
							SyllabifierLibrary.getInstance().defaultSyllabifierLanguage().getPrimaryLanguage().getId());
				}
			}

			alignedTypes.put(TypeMapMetadataTier.PROJECT_ID.getTierName(), getEditor().getProject().getUUID().toString());

			final Tuple<String[], String[]> alignedTypeArrays = AlignedTypesUtil.alignedTypesToArrays(alignedTypes);
			final AlignedTypesEdit edit = new AlignedTypesEdit(getEditor(), this,
					AlignedTypesEdit.Operation.ADD, alignedTypeArrays.getObj1(), alignedTypeArrays.getObj2());
			getEditor().getUndoSupport().postEdit(edit);
		}
	}

	public void onDeleteAlignedTypes(PhonActionEvent<Void> pae) {
		int selectedWord = this.wordTable.getSelectedRow();
		if(selectedWord >= 0 && selectedWord < this.currentState.getLeafCount()) {
			final int selectedAlignment = this.alignmentOptionsTable.getSelectedRow();
			if(selectedAlignment >= 0 && selectedAlignment < this.alignmentOptionsTableModel.alignmentRows.length) {
				final String[] alignedTypes = this.alignmentOptionsTableModel.alignmentRows[selectedAlignment];

				final AlignedTypesEdit edit = new AlignedTypesEdit(getEditor(), this,
						AlignedTypesEdit.Operation.REMOVE, getVisibleOptionsTiers().toArray(new String[0]), alignedTypes);
				getEditor().getUndoSupport().postEdit(edit);
			}
		}
	}

	/**
	 * Show morpheme menu for selected word/morpheme in table
	 *
	 * @param pae
	 */
	public void showWordMenu(PhonActionEvent<Void> pae) {
		final JPopupMenu popupMenu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(popupMenu);

		if(this.currentState == null) return;

		final String keyTier = keyTier();
		if(keyTier == null) return;

		final int selectedRow = this.wordTable.getSelectedRow();

		if(selectedRow >= 0) {
			final String[][] optionsForMorpheme = alignmentOptionsForWord(selectedRow);
			setupWordMenu(builder, selectedRow, optionsForMorpheme);

			int ypos = 0;
			for (int i = 0; i <= selectedRow; i++) {
				ypos += wordTable.getRowHeight(i);
			}
			popupMenu.show(this.wordTable, 0, ypos);
		}
	}

	private String wordSetMenuItemText(String[] optionSet) {
		final List<String> visibleTiers = getVisibleAlignmentTiers();
		final StringBuilder builder = new StringBuilder();

		for(int i = 1; i < visibleTiers.size(); i++) {
			final String option = optionSet[i];
			if(i > 1)
				builder.append(" \u2194 ");
			builder.append(option);
		}

		return builder.toString();
	}

	private void setupWordMenu(MenuBuilder builder, int wordIndex, String[][] options) {
		final String headerTxt = wordSetMenuItemText(getVisibleAlignmentTiers().toArray(new String[0]));
		builder.addItem(".", headerTxt).setEnabled(false);

		for (int i = 0; i < options.length && i < 10; i++) {
			final String optionTxt = wordSetMenuItemText(options[i]);
			final InsertAlignedWordsData data = new InsertAlignedWordsData();
			data.wordIndex = wordIndex;
			data.options = options[i];
			final PhonUIAction<InsertAlignedWordsData> insertAlignedWordsAct = PhonUIAction.eventConsumer(this::insertAlignedWords, data);
			insertAlignedWordsAct.putValue(PhonUIAction.NAME, optionTxt);
			insertAlignedWordsAct.putValue(PhonUIAction.SHORT_DESCRIPTION,
					"Insert aligned values into record, replacing current words/morphemes");
			builder.addItem(".", insertAlignedWordsAct);
		}

		if(options.length > 10) {
			builder.addSeparator(".", "more_options");
			final PhonUIAction<Integer> focusOptionsAct = PhonUIAction.consumer(this::onFocusAlignmentOptions, Integer.valueOf(10));
			focusOptionsAct.putValue(PhonUIAction.NAME, "See more below...");
			focusOptionsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "View more options in the Alignment Options table");
			builder.addItem(".", focusOptionsAct);
		}

		if(wordIndex >= this.currentState.getLeafCount()) {
			TypeMapNode wordNode = new TypeMapNode(this.currentState.getLeafCount(), new LinkedHashMap<>());
			this.currentState.getChild(this.currentState.childCount()-1).addChild(wordNode);
		}
		TypeMapNode morphemeNode = this.currentState.getLeaves().get(wordIndex);

		final Map<String, String> alignedTypes = new LinkedHashMap<>();
		for(String tierName:getVisibleAlignmentTiers()) {
			final String morpheme = morphemeNode.getType(tierName);
			alignedTypes.put(tierName, "*".equals(morpheme) ? "" : morphemeNode.getType(tierName));
		}
		if(!this.getUserDb().hasAlignedTypes(alignedTypes)) {
			builder.addSeparator(".", "add_alignment");
			final PhonUIAction<Void> onAddAlignedTypesAct = PhonUIAction.eventConsumer(this::onAddAlignedTypes);
			onAddAlignedTypesAct.putValue(PhonUIAction.NAME, "Add alignment to database");
			onAddAlignedTypesAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Add aligned types to database");
			builder.addItem(".", onAddAlignedTypesAct);
		}
	}

	public void onFocusAlignmentOptions(Integer focusRow) {
		this.alignmentOptionsTable.getSelectionModel().setSelectionInterval(focusRow, focusRow);
		this.alignmentOptionsTable.requestFocusInWindow();
		this.alignmentOptionsTable.scrollRowToVisible(focusRow);
	}

	public void onSelectTierOption(Integer tierNum) {
		final int selectedWord = this.wordTable.getSelectedRow();
		if(selectedWord < 0 || selectedWord >= this.currentState.getLeafCount()) return;

		final int selectedOption = this.alignmentOptionsTable.getSelectedRow();
		if(selectedOption < 0 || selectedOption >= this.alignmentOptionsTableModel.alignmentRows.length) return;

		final String[] types = this.alignmentOptionsTableModel.alignmentRows[selectedOption];
		if(tierNum > 0 && tierNum < getVisibleAlignmentTiers().size()) {
			final String tierName = getVisibleAlignmentTiers().get(tierNum);
			final String selectedType = types[tierNum];

			updateTier(selectedWord, tierName, selectedType);
		}
	}

	public void showAlignmentOptionsMenu(PhonActionEvent<Void> pae) {
		final JPopupMenu popupMenu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(popupMenu);

		if(this.currentState == null) return;

		final String keyTier = keyTier();
		if(keyTier == null) return;

		final int selectedWord = this.wordTable.getSelectedRow();
		if(selectedWord < 0) return;

		final int selectedRow = this.alignmentOptionsTable.getSelectedRow();
		if(selectedRow >= 0) {
			setupAlignmentOptionsMenu(builder, selectedWord, selectedRow, this.alignmentOptionsTableModel.alignmentRows);

			int ypos = 0;
			for (int i = 0; i <= selectedRow; i++) {
				ypos += alignmentOptionsTable.getRowHeight(i);
			}
			popupMenu.show(this.alignmentOptionsTable, 0, ypos);
		}
	}

	private void setupAlignmentOptionsMenu(MenuBuilder builder, int wordIndex,
	                                       int selectedSet, String[][] options) {
		String[] optionSet = (selectedSet < options.length ? options[selectedSet] : new String[0]);
		List<String> visibleTiers = getVisibleAlignmentTiers();

		if(wordIndex < 0) return;

		final List<TypeMapNode> leafNodes = this.currentState.getLeaves();
		final TypeMapNode leafNode =
				(wordIndex > 0 && wordIndex < leafNodes.size() ? leafNodes.get(wordIndex) : null);

		for(int i = 1; i < visibleTiers.size(); i++) {
			final String tierName = visibleTiers.get(i);
			final String option = optionSet[i];

			if(option.length() == 0) continue;

			final String optionTxt = String.format("%s: %s \u2192 %s", tierName, (leafNode == null ? "" : leafNode.getType(tierName)), option);
			final String descTxt = String.format("Insert/Replace word for tier %s", tierName);
			final InsertWordForTierData eventData = new InsertWordForTierData();
			eventData.wordIndex = wordIndex;
			eventData.tierName = tierName;
			eventData.word = option;
			final PhonUIAction<InsertWordForTierData> insertWordAct = PhonUIAction.eventConsumer(this::insertWordForTier, eventData);
			insertWordAct.putValue(PhonUIAction.NAME, optionTxt);
			insertWordAct.putValue(PhonUIAction.SHORT_DESCRIPTION, descTxt);
			if(i < 9)
				insertWordAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0));
			builder.addItem(".", insertWordAct);
		}

		builder.addSeparator(".", "insert_all");
		final String headerTxt = wordSetMenuItemText(getVisibleAlignmentTiers().toArray(new String[0]));
		builder.addItem(".", headerTxt).setEnabled(false);

		final InsertAlignedWordsData data = new InsertAlignedWordsData();
		data.wordIndex = wordIndex;
		data.options = optionSet;
		final PhonUIAction<InsertAlignedWordsData> insertAlignedWordsAct = PhonUIAction.eventConsumer(this::insertAlignedWords, data);
		insertAlignedWordsAct.putValue(PhonUIAction.NAME, wordSetMenuItemText(optionSet));
		insertAlignedWordsAct.putValue(PhonUIAction.SHORT_DESCRIPTION,
				"Insert aligned values into record, replacing current words");
		builder.addItem(".", insertAlignedWordsAct);

		builder.addSeparator(".", "delete");
		final PhonUIAction<Void> deleteAlignedTypesAct = PhonUIAction.eventConsumer(this::onDeleteAlignedTypes);
		deleteAlignedTypesAct.putValue(PhonUIAction.NAME, "Remove alignment from database");
		deleteAlignedTypesAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove aligned types from database");
		deleteAlignedTypesAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		builder.addItem(".", deleteAlignedTypesAct);
	}

	private String[][] alignmentOptionsForWord(int wordIndex) {
		final TypeMapNode leafNode = this.currentState.getLeaves().get(wordIndex);

		final String keyTier = keyTier();
		final String morpheme = leafNode.getType(keyTier);
		return alignmentOptionsForType(morpheme, leafNode.getAlignedTypeOptions());
	}

	private String[][] alignmentOptionsForType(String type, Map<String, String[]> alignedTypes) {
		if(type.length() == 0)
			return new String[0][];

		final List<String> visibleTiers = getVisibleOptionsTiers();
		final String[][] arrays = new String[visibleTiers.size()][];

		int idx = 0;
		for(String tierName:visibleTiers) {
			if(keyTier().equals(tierName))
				arrays[idx++] = new String[]{type};
			else
				arrays[idx++] = (alignedTypes.containsKey(tierName) ? alignedTypes.get(tierName) : new String[0]);
		}

		final String[][] product = CartesianProduct.stringArrayProduct(arrays,
				(set) -> getUserDb().hasAlignedTypes(visibleTiers.toArray(new String[0]), set));
		return product;
	}

	private class InsertWordForTierData {
		int wordIndex = 0;
		String tierName;
		String word;
	}

	public void insertWordForTier(PhonActionEvent<InsertWordForTierData> pae) {
		final InsertWordForTierData data = pae.getData();
		updateTier(data.wordIndex, data.tierName, data.word);
	}

	private class InsertAlignedWordsData {
		int wordIndex = 0;
		String[] options = new String[0];
	}

	public void insertAlignedWords(PhonActionEvent<InsertAlignedWordsData> pae) {
		final InsertAlignedWordsData data = pae.getData();
		updateRecord(data.wordIndex, getVisibleAlignmentTiers().toArray(new String[0]), data.options);
	}

	/**
	 * Set of all aligned tiers in both the record and the database
	 *
	 * @return list of all tiers
	 */
	public List<String> allTiers() {
		Set<String> tierSet = new LinkedHashSet<>();
		tierSet.addAll(sessionGroupedTiers());
		final AlignedTypesDatabase projectDb = getUserDb();
		if(projectDb != null)
			tierSet.addAll(projectDb.tierNames());
		return tierSet.stream().collect(Collectors.toList());
	}

	private boolean sessionHasTier(String tierName) {
		SystemTierType systermTier = SystemTierType.tierFromString(tierName);
		if(systermTier != null) {
			return systermTier.isGrouped();
		} else {
			for (TierDescription td : getEditor().getSession().getUserTiers()) {
				if(td.getName().equals(tierName)) return true;
			}
		}
		return false;
	}

	private boolean sessionTierVisible(String tierName) {
		Optional<TierViewItem> tviOpt =
				getEditor().getSession().getTierView().stream().filter(tvi -> tvi.getTierName().equals(tierName)).findAny();
		if(tviOpt.isPresent()) {
			return tviOpt.get().isVisible();
		} else {
			return false;
		}
	}

	private boolean dbTierVisible(String tierName) {
		return !isTierHidden(getEditor().getProject(),
				new SessionPath(getEditor().getSession().getCorpus(), getEditor().getSession().getName()), tierName);
	}

	private boolean dbOnlyTierVisible(String tierName) {
		return isDbOnlyTierVisible(getEditor().getProject(),
				new SessionPath(getEditor().getSession().getCorpus(), getEditor().getSession().getName()), tierName);
	}

	private boolean isGroupedTier(String tierName) {
		SystemTierType systemTier = SystemTierType.tierFromString(tierName);
		if(systemTier != null) {
			return systemTier.isGrouped();
		} else {
			for(TierDescription td:getEditor().getSession().getUserTiers()) {
				if (td.getName().equals(tierName)) {
					return td.isGrouped();
				}
			}
		}
		return false;

	}

	private boolean isGroupedTier(TierViewItem tvi) {
		return isGroupedTier(tvi.getTierName());
	}

	/**
	 * Set of all aligned tiers in the session
	 *
	 * @return all aligned (grouped) tiers in session
	 */
	public List<String> sessionGroupedTiers() {
		List<String> tierList = getEditor().getSession().getTierView()
				.stream()
				.filter(this::isGroupedTier)
				.map(TierViewItem::getTierName)
				.collect(Collectors.toList());
		return tierList;
	}

	/**
	 * Determine group index from morpheme table row
	 *
	 * @param row
	 *
	 * @return group index for provided row
	 */
	public int tableRowToGroupIndex(int row) {
		if(this.currentState == null) return -1;
		int offset = 0;
		for(int gidx = 0; gidx < this.currentState.childCount(); gidx++) {
			TypeMapNode groupNode = this.currentState.getChild(gidx);
			if(row < offset + groupNode.getLeafCount())
				return gidx;
			offset += groupNode.getLeafCount();
		}
		return -1;
	}

	/**
	 * The list of tiers visible in the morpheme table and key tier selector
	 *
	 * @return list of tiers visible in the morpheme table
	 *
	 */
	public List<String> getVisibleAlignmentTiers() {
		// use ordering and visibility from session
		List<String> tierList = getEditor().getSession().getTierView()
				.stream()
				.filter(TierViewItem::isVisible)
				.map(TierViewItem::getTierName)
				.collect(Collectors.toList());
		for(SystemTierType systemTier:SystemTierType.values()) {
			if(!systemTier.isGrouped()) tierList.remove(systemTier.getName());
		}
		for(TierDescription td:getEditor().getSession().getUserTiers()) {
			if(!td.isGrouped()) tierList.remove(td.getName());
		}
		if(keyTierBox.getSelectedItem() != null) {
			// move key tier to front
			tierList.remove(keyTierBox.getSelectedItem());
			tierList.add(0, keyTierBox.getSelectedItem().toString());
		}
		// filter based on database visibility
		if(getUserDb() != null) {
			return tierList.stream().filter(this::dbTierVisible).collect(Collectors.toList());
		} else {
			return tierList;
		}
	}

	public List<String> getVisibleOptionsTiers() {
		final List<String> retVal = getVisibleAlignmentTiers();

		if(getUserDb() != null) {
			for (String tierName : getUserDb().tierNames()) {
				// ignore hidden/metadata tiers
				if(!retVal.contains(tierName) && !tierName.startsWith("__")) {
					if(dbOnlyTierVisible(tierName)) {
						retVal.add(tierName);
					}
				}
			}
		}

		return retVal;
	}

	private class WordTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			TypeMapNode leafNode = currentState.getLeaves().get(row);

			String tier = getVisibleAlignmentTiers().get(column);
			String key = leafNode.getType(keyTier());
			String morpheme = leafNode.getType(tier);

			if(column == 0) {
				// check that key exists in database
				if(!getUserDb().typeExistsInTier(key, keyTier())) {
					retVal.setFont(retVal.getFont().deriveFont(Font.ITALIC));
				}
			} else {
				// check that link exists to key
				if(!getUserDb().alignmentExists(keyTier(), key, tier, morpheme) && !"*".equals(morpheme)) {
					retVal.setFont(retVal.getFont().deriveFont(Font.ITALIC));
				}
			}

			return retVal;
		}
	}

	private class WordTableModel extends AbstractTableModel {

		public WordTableModel() {
			super();
		}

		@Override
		public int getRowCount() {
			if(currentState == null) return 0;
			return currentState.getLeafCount();
		}

		@Override
		public int getColumnCount() {
			return getVisibleAlignmentTiers().size();
		}

		public String getColumnName(int colIdx) {
			return getVisibleAlignmentTiers().get(colIdx);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(currentState == null) return "";
			List<String> tierNames = getVisibleAlignmentTiers();
			TypeMapNode leafNode = currentState.getLeaves().get(rowIndex);
			String tierName = (columnIndex < tierNames.size() ? tierNames.get(columnIndex) : null);
			return (tierName == null ? "" : leafNode.getType(tierName));
		}

	}

	private class AlignmentOptionsTableModel extends AbstractTableModel {

		private String[][] alignmentRows;

		public AlignmentOptionsTableModel() {
			this.alignmentRows = new String[0][];
		}

		public void setAlignmentRows(String[][] alignmentRows) {
			this.alignmentRows = alignmentRows;
			this.fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return alignmentRows.length;
		}

		@Override
		public int getColumnCount() {
			return getVisibleOptionsTiers().size();
		}

		public String getColumnName(int colIdx) {
			if(colIdx > 0 && colIdx < 9 && colIdx < getVisibleAlignmentTiers().size())
				return getVisibleOptionsTiers().get(colIdx) + (colIdx > 0 ? " (" + colIdx + ")" : "");
			else
				return getVisibleOptionsTiers().get(colIdx);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(rowIndex < alignmentRows.length) {
				String[] rowData = alignmentRows[rowIndex];
				if(columnIndex < rowData.length) {
					return rowData[columnIndex];
				}
			}
			return "";
		}

	}

}
