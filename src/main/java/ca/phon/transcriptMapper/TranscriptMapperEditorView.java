package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.extensions.UnvalidatedValue;
import ca.phon.ipa.*;
import ca.phon.ipa.alignment.*;
import ca.phon.ipadictionary.IPADictionaryLibrary;
import ca.phon.orthography.Orthography;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.alignedMorphemes.*;
import ca.phon.syllabifier.*;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.*;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aligned morpheme editor view for Phon sessions. This view will display the aligned morpheme lookup data
 * for the current record and provide method for inserting aligned morpheme data and updating the aligned
 * morpheme database(s).
 *
 */
public final class TranscriptMapperEditorView extends EditorView {

	private JToolBar toolbar;

	private JLabel keyLabel;

	private JComboBox<String> keyTierBox;

	private JLabel morphemesLabel;

	private MorphemesTableModel morphemesTableModel;

	private JXTable morphemesTable;

	private JLabel alignmentOptionsLabel;

	private AlignmentOptionsTableModel alignmentOptionsTableModel;

	private JXTable alignmentOptionsTable;

	private JWindow suggestionsWindow;

	private JList<String> suggestionsList;

	private TierDataLayoutPanel morphemeSelectionPanel;

	public final static String NAME = "Transcript Mapper";

	public final static String ICON = "blank";

	private TypeMapNode currentState;

	public TranscriptMapperEditorView(SessionEditor editor) {
		super(editor);

		init();
		loadUserDbAsync(this::updateAfterDbLoad);

		setupEditorEvenListeners();
	}

	private void setupEditorEvenListeners() {
		EditorAction tierChangedEvt = new DelegateEditorAction(this, "onTierChanged");
		getEditor().getEventManager().registerActionForEvent(EditorEventType.TIER_CHANGED_EVT, tierChangedEvt);

		EditorAction recordChangedAct = new DelegateEditorAction(this, "onRecordChanged");
		getEditor().getEventManager().registerActionForEvent(EditorEventType.RECORD_CHANGED_EVT, recordChangedAct);

		EditorAction tierViewChangeAct = new DelegateEditorAction(this, "onTierViewChanged");
		getEditor().getEventManager().registerActionForEvent(EditorEventType.TIER_VIEW_CHANGED_EVT, tierViewChangeAct);
	}

	@RunOnEDT
	public void onRecordChanged(EditorEvent ee) {
		updateStateAsync(this::updateFromCurrentState);
	}

	@RunOnEDT
	public void onTierChanged(EditorEvent ee) {
		final int selectedRow = this.morphemesTable.getSelectedRow();
		updateStateAsync(() -> {
			this.updateFromCurrentState();
			if(selectedRow >= 0 && selectedRow < this.morphemesTableModel.getRowCount()) {
				SwingUtilities.invokeLater(() ->
						this.morphemesTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow));
			}
		});
	}

	@RunOnEDT
	public void onTierViewChanged(EditorEvent ee) { updateAfterDbLoad(); }

	AlignedTypesDatabase getUserDb() {
		final UserATDB userATDB = UserATDB.getInstance();
		return userATDB.getATDB();
	}

	private void loadUserDbAsync(Runnable onFinish) {
		final PhonTask task = PhonWorker.invokeOnNewWorker(this::loadUserDb, onFinish, LogUtil::warning);
		task.setName("Loading aligned types database");
		getEditor().getStatusBar().watchTask(task);
	}

	private void loadUserDb() {
		final UserATDB userATDB = UserATDB.getInstance();
		if(!userATDB.isATDBLoaded()) {
			try {
				userATDB.loadATDB();
			} catch (IOException e) {
				LogUtil.severe(e);
			}
		}
	}

	void saveUserDbAsync(Runnable onFinish) {
		final PhonTask task = PhonWorker.invokeOnNewWorker(this::saveUserDb, onFinish, LogUtil::warning);
		task.setName("Saving aligned morpheme database");
		getEditor().getStatusBar().watchTask(task);
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
		final MenuBuilder dbMenuBuilder = new MenuBuilder(dbMenu);
		setupDatabaseMenu(dbMenuBuilder);

		PhonUIAction dbMenuAct = new PhonUIAction(this, "noop");
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

		PhonUIAction tiersMenuAct = new PhonUIAction(this, "noop");
		tiersMenuAct.putValue(PhonUIAction.NAME, "Tiers");
		tiersMenuAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show tiers menu");
		tiersMenuAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("misc/record", IconSize.SMALL));
		tiersMenuAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		tiersMenuAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		tiersMenuAct.putValue(DropDownButton.BUTTON_POPUP, tiersMenu);

		DropDownButton dbBtn = new DropDownButton(dbMenuAct);
		dbBtn.setOnlyPopup(true);

		DropDownButton tiersBtn = new DropDownButton(tiersMenuAct);
		tiersBtn.setOnlyPopup(true);

		toolbar.add(dbBtn);
		toolbar.add(tiersBtn);
	}
	private void setupDatabaseMenu(MenuBuilder builder) {
		builder.addItem(".", new ScanProjectAction(this));
		builder.addSeparator(".", "scan");
		builder.addItem(".", new ImportCSVAction(this));
		builder.addItem(".", new ExportCSVAction(this));
		builder.addSeparator(".", "csv");

		final JMenu importDictMenu = builder.addMenu(".", "Import IPA dictionary");
		final MenuBuilder importBuilder = new MenuBuilder(importDictMenu);
		setupImportDictionaryMenu(importBuilder);

		builder.addSeparator(".", "import_dict");
		builder.addItem(".", new ImportDatabaseAction(this));
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
				final PhonUIAction toggleTierVisibleAct = new PhonUIAction(this, "toggleTier", tierName);
				toggleTierVisibleAct.putValue(PhonUIAction.NAME, tierName);
				toggleTierVisibleAct.putValue(PhonUIAction.SHORT_DESCRIPTION, String.format("Toggle tier %s", tierName));
				toggleTierVisibleAct.putValue(PhonUIAction.SELECTED_KEY, dbTierVisible(tierName));
				final JCheckBoxMenuItem toggleTierItem = new JCheckBoxMenuItem(toggleTierVisibleAct);
				if(!isGroupedTier(tierName)) {
					toggleTierItem.setEnabled(false);
					toggleTierItem.setToolTipText(String.format("%s is not a group tier", tierName));
				} else if(!sessionTierVisible(tierName)) {
					toggleTierItem.setEnabled(false);
					toggleTierItem.setToolTipText(String.format("%s is not visible in record data", tierName));
				}
				builder.addItem(".", toggleTierItem);
			} else {
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
				final PhonUIAction createTierAct = new PhonUIAction(this, "createTiers", List.of(tierName));
				createTierAct.putValue(PhonUIAction.NAME, "Add tier: " + tierName);
				createTierAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Add tier " + tierName + " to session");
				builder.addItem(".", createTierAct);
			}

			if(dbOnlyTiers.size() > 1) {
				final PhonUIAction createAllTiersAct = new PhonUIAction(this, "createTiers", dbOnlyTiers);
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
			if(this.morphemesTableModel != null)
				this.morphemesTableModel.fireTableStructureChanged();
			if(this.alignmentOptionsTableModel != null)
				this.alignmentOptionsTableModel.fireTableStructureChanged();
		});

		morphemeSelectionPanel = new TierDataLayoutPanel();

		int row = 0;
		keyLabel = new JLabel("Key tier");
		keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		keyLabel.setFont(FontPreferences.getTitleFont());
		morphemeSelectionPanel.add(keyLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));
		morphemeSelectionPanel.add(keyTierBox, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		++row;
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		morphemeSelectionPanel.add(sep, new TierDataConstraint(TierDataConstraint.FULL_TIER_COLUMN, row));

		++row;
		morphemesLabel = new JLabel("Words/Morphemes");
		morphemesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		morphemesLabel.setFont(FontPreferences.getTitleFont());
		morphemeSelectionPanel.add(morphemesLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));

		setupMorphemesTable();
		morphemeSelectionPanel.add(new JScrollPane(morphemesTable), new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		++row;
		alignmentOptionsLabel = new JLabel("Alignment Options");
		alignmentOptionsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		alignmentOptionsLabel.setFont(FontPreferences.getTitleFont());
		morphemeSelectionPanel.add(alignmentOptionsLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));

		setupAlignmentOptionsTable();
		morphemeSelectionPanel.add(new JScrollPane(alignmentOptionsTable), new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		add(new JScrollPane(morphemeSelectionPanel), BorderLayout.CENTER);
	}

	private void setupMorphemesTable() {
		morphemesTableModel = new MorphemesTableModel();
		morphemesTable = new JXTable(morphemesTableModel) {
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				// alternate row color based on group index
				if (!isRowSelected(row))
					c.setBackground(tableRowToGroupIndex(row) % 2 == 0 ? getBackground() : PhonGuiConstants.PHON_UI_STRIP_COLOR);
				return c;
			}
		};
		morphemesTable.setDefaultRenderer(Object.class, new MorphemeTableCellRenderer());
		morphemesTable.setSortable(false);
		morphemesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		morphemesTable.setVisibleRowCount(8);

		morphemesTable.getSelectionModel().addListSelectionListener(e -> {
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
				final int row = morphemesTable.rowAtPoint(e.getPoint());
				if(row >= 0 && row < morphemesTable.getRowCount()) {
					morphemesTable.getSelectionModel().setSelectionInterval(row, row);

					final String[][] optionsForMorpheme = alignmentOptionsForMorpheme(row);
					final JPopupMenu menu = new JPopupMenu();
					setupMorphemeMenu(new MenuBuilder(menu), row, optionsForMorpheme);
					menu.show(morphemesTable, e.getX(), e.getY());
				}
			}

		};
		morphemesTable.addMouseListener(ctxHandler);

		final InputMap inputMap = morphemesTable.getInputMap(JComponent.WHEN_FOCUSED);
		final ActionMap actionMap = morphemesTable.getActionMap();

		final PhonUIAction showMorphemeMenuAction = new PhonUIAction(this, "showMorphemeMenu");
		showMorphemeMenuAction.putValue(PhonUIAction.NAME, "Show menu for selected word/morpheme");
		final String morphemeMenuActId = "show_morpheme_menu";
		actionMap.put(morphemeMenuActId, showMorphemeMenuAction);
		final KeyStroke showMorphemeMenuKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(showMorphemeMenuKs, morphemeMenuActId);

		final PhonUIAction focusAct = new PhonUIAction(this, "onFocusAlignmentOptions", 0);
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
		alignmentOptionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		alignmentOptionsTable.setVisibleRowCount(8);
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
					setupAlignmentOptionsMenu(new MenuBuilder(menu), morphemesTable.getSelectedRow(), row, alignmentOptionsTableModel.alignmentRows);
					menu.show(alignmentOptionsTable, e.getX(), e.getY());
				}
			}

		};
		alignmentOptionsTable.addMouseListener(ctxHandler);

		final InputMap inputMap = alignmentOptionsTable.getInputMap(JComponent.WHEN_FOCUSED);
		final ActionMap actionMap = alignmentOptionsTable.getActionMap();

		final PhonUIAction showAlignmentOptionsMenuAct = new PhonUIAction(this, "showAlignmentOptionsMenu");
		final String alignmentOptionsMenuActId = "show_alignment_options_menu";
		actionMap.put(alignmentOptionsMenuActId, showAlignmentOptionsMenuAct);
		final KeyStroke showAlignmentOptionsMenuKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(showAlignmentOptionsMenuKs, alignmentOptionsMenuActId);

		final PhonUIAction focusAct = new PhonUIAction(this, "onFocusMorphemeTable", 0);
		final String focusActId = "focus_morpheme_table";
		final KeyStroke focusKs = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		actionMap.put(focusActId, focusAct);
		inputMap.put(focusKs, focusActId);

		final PhonUIAction deleteAlignedTypesAct = new PhonUIAction(this, "onDeleteAlignedTypes");
		final String deleteAlignedTypesId = "delete_aligned_types";
		actionMap.put(deleteAlignedTypesId, deleteAlignedTypesAct);
		final KeyStroke deleteAlignedTypesKs = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		final KeyStroke deleteAlignedTypesKs2 = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		inputMap.put(deleteAlignedTypesKs, deleteAlignedTypesId);
		inputMap.put(deleteAlignedTypesKs2, deleteAlignedTypesId);

		for(int i = 1; i < 10; i++) {
			final PhonUIAction selectTypeAction = new PhonUIAction(this, "onSelectTierOption", Integer.valueOf(i));
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

		if(morphemesTableModel != null)
			morphemesTableModel.fireTableDataChanged();
		updateAlignmentOptions();
	}

	private void updateAlignmentOptions() {
		if(alignmentOptionsTableModel != null) {
			final int selectedMorpheme = morphemesTable.getSelectedRow();
			if(selectedMorpheme >= 0) {
				final String[][] alignmentOptions = alignmentOptionsForMorpheme(selectedMorpheme);
				alignmentOptionsTableModel.setAlignmentRows(alignmentOptions);
			} else {
				alignmentOptionsTableModel.setAlignmentRows(new String[0][]);
			}
		}
	}

	void updateAfterDbLoad() {
		if(getUserDb() == null) return;

		this.keyTierBox.setModel(new DefaultComboBoxModel<>(getVisibleTiers().toArray(new String[0])));
		this.keyTierBox.setSelectedItem(SystemTierType.Orthography.getName());

		if(getEditor().currentRecord() != null) {
			updateStateAsync(() -> {
				if(this.alignmentOptionsTableModel != null)
					this.alignmentOptionsTableModel.fireTableStructureChanged();
				if(this.morphemesTableModel != null)
					this.morphemesTableModel.fireTableStructureChanged();
				this.updateFromCurrentState();
			});
		}
	}

	/**
	 * Called after adding or removing aligned types to the database retains table selection
	 *
	 */
	void updateAfterDbChange() {
		final int selectedMorpheme = this.morphemesTable.getSelectedRow();
		final int selectedOption = this.alignmentOptionsTable.getSelectedRow();

		updateStateAsync(() -> {
			updateFromCurrentState();
			SwingUtilities.invokeLater(() -> {
				if(selectedMorpheme >= 0 && selectedMorpheme < this.morphemesTableModel.getRowCount()) {
					this.morphemesTable.getSelectionModel().setSelectionInterval(selectedMorpheme, selectedMorpheme);
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
		if(needsRefresh)
			getEditor().getEventManager().queueEvent(new EditorEvent(EditorEventType.RECORD_REFRESH_EVT));
	}

	private boolean updateTier(int morphemeIdx, String tier, String selectedMorpheme) {
		if(selectedMorpheme.length() == 0) return false;

		boolean needsRefresh = false;
		int gIdx = tableRowToGroupIndex(morphemeIdx);
		int mIdx = 0;
		for(int i = 0; i < gIdx; i++)
			mIdx += currentState.getChild(i).getLeafCount();
		final TypeMapNode groupNode = this.currentState.getChild(gIdx);
		final StringBuilder builder = new StringBuilder();
		for(int wIdx = 0; wIdx < groupNode.childCount(); wIdx++) {
			if(wIdx > 0) builder.append(' ');
			TypeMapNode wordNode = groupNode.getChild(wIdx);

			for(int wmIdx = 0; wmIdx < wordNode.childCount(); wmIdx++) {
				TypeMapNode morphemeNode = wordNode.getChild(wmIdx);
				// append morpheme marker
				if(wmIdx > 0) builder.append(wordNode.getChildren().get(wmIdx).getObj1());
				if(mIdx + wmIdx == morphemeIdx) {
					builder.append(selectedMorpheme);
				} else {
					final String cval = morphemeNode.getMorpheme(tier);
					if(cval.length() > 0)
						builder.append(morphemeNode.getMorpheme(tier));
					else
						builder.append('*');
				}
			}
			mIdx += wordNode.childCount();
		}


		final Record currentRecord = getEditor().currentRecord();
		final SystemTierType systemTier = SystemTierType.tierFromString(tier);
		if(systemTier != null) {
			switch(systemTier) {
				case Orthography -> {
					final Orthography currentOrtho = currentRecord.getOrthography().getGroup(gIdx);
					int currentMorphemeIdx = 0;
					for(int i = 0; i < gIdx; i++) {
						Group group = currentRecord.getGroup(i);
						for(int j = 0; j < group.getAlignedWordCount(); j++) {
							Word word = group.getAlignedWord(j);
							AlignedMorphemes alignedMorphemes = word.getExtension(AlignedMorphemes.class);
							if(alignedMorphemes != null) {
								currentMorphemeIdx += alignedMorphemes.getMorphemeCount();
							} else {
								++currentMorphemeIdx;
							}
						}
					}
					final OrthographyMorphemeReplacementVisitor visitor = new OrthographyMorphemeReplacementVisitor(morphemeIdx, selectedMorpheme, currentMorphemeIdx);
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

		final AlignedTypesDatabase projectDb = getUserDb();
		if(projectDb == null) return root;

		final String keyTier = keyTier();
		if(keyTier == null) return root;

		List<String> tierList = getVisibleTiers();
		for(int gidx = 0; gidx < record.numberOfGroups(); gidx++) {
			Group grp = record.getGroup(gidx);
			TypeMapNode grpNode = new TypeMapNode(gidx);
			root.addChild(grpNode);
			for(int widx = 0; widx < grp.getAlignedWordCount(); widx++) {
				Word wrd = grp.getAlignedWord(widx);
				TypeMapNode wrdNode = new TypeMapNode(widx);
				grpNode.addChild(' ', wrdNode);

				AlignedMorphemes morphemes = wrd.getExtension(AlignedMorphemes.class);
				if(morphemes != null) {
					for(int midx = 0; midx < morphemes.getMorphemeCount(); midx++) {
						AlignedMorpheme morpheme = morphemes.getAlignedMorpheme(midx);

						Map<String, String> currentMorphemes = new HashMap<>();
						for(String tierName:tierList) {
							currentMorphemes.put(tierName, morpheme.getMorphemeText(tierName));
						}
						Map<String, String[]> alignedTypes =
								projectDb.alignedTypesForTier(keyTier, currentMorphemes.get(keyTier), tierList);

						TypeMapNode morphemeNode = new TypeMapNode(midx, currentMorphemes, alignedTypes);

						// start of word
						char ch = '\u0000';
						if(midx > 0) {
							int orthoIdx = morpheme.getOrthographyWordLocation();
							int chIdx = orthoIdx - 1;
							ch = (chIdx >= 0 ? wrd.getOrthography().toString().charAt(chIdx) : '\u0000');
						}

						wrdNode.addChild(ch, morphemeNode);
					}
				}
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
		final AlignedTypesDatabase projectDb = getUserDb();
		if(projectDb == null) return;

		final Optional<TierInfo> tierInfoOpt =
				projectDb.getTierInfo().stream().filter(ti -> ti.getTierName().equals(tierName)).findAny();
		if(tierInfoOpt.isPresent()) {
			TierInfo tierInfo = tierInfoOpt.get();
			tierInfo.setVisible(!tierInfo.isVisible());

			saveUserDbAsync(this::updateAfterDbLoad);
		}
	}

	/**
	 * Create the given list of tiers in the session
	 *
	 * @param pae
	 */
	public void createTiers(PhonActionEvent pae) {
		final List<String> tierList = (List<String>) pae.getData();
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

	public void onFocusMorphemeTable(PhonActionEvent pae) {
		if(this.morphemesTable != null)
			this.morphemesTable.requestFocusInWindow();
	}

	public void onAddAlignedTypes(PhonActionEvent pae) {
		int selectedMorpheme = this.morphemesTable.getSelectedRow();
		if(selectedMorpheme >= 0 && selectedMorpheme < this.morphemesTableModel.getRowCount()) {
			final TypeMapNode morphemeNode = this.currentState.getLeaves().get(selectedMorpheme);
			final Map<String, String> alignedTypes = new LinkedHashMap<>();
			for(String tierName:getVisibleTiers()) {
				alignedTypes.put(tierName, morphemeNode.getMorpheme(tierName));
			}

			final Tuple<String[], String[]> alignedTypeArrays = AlignedTypesDatabase.alignedTypesToArrays(alignedTypes);
			final AlignedTypesEdit edit = new AlignedTypesEdit(getEditor(), this,
					AlignedTypesEdit.Operation.ADD, alignedTypeArrays.getObj1(), alignedTypeArrays.getObj2());
			getEditor().getUndoSupport().postEdit(edit);
		}
	}

	public void onDeleteAlignedTypes(PhonActionEvent pae) {
		int selectedMorpheme = this.morphemesTable.getSelectedRow();
		if(selectedMorpheme >= 0 && selectedMorpheme < this.currentState.getLeafCount()) {
			TypeMapNode morphemeNode = this.currentState.getLeaves().get(selectedMorpheme);
			final int selectedAlignment = this.alignmentOptionsTable.getSelectedRow();
			if(selectedAlignment >= 0 && selectedAlignment < this.alignmentOptionsTableModel.alignmentRows.length) {
				final String[] alignedTypes = this.alignmentOptionsTableModel.alignmentRows[selectedAlignment];

				final AlignedTypesEdit edit = new AlignedTypesEdit(getEditor(), this,
						AlignedTypesEdit.Operation.REMOVE, getVisibleTiers().toArray(new String[0]), alignedTypes);
				getEditor().getUndoSupport().postEdit(edit);
			}
		}
	}

	/**
	 * Show morpheme menu for selected word/morpheme in table
	 *
	 * @param pae
	 */
	public void showMorphemeMenu(PhonActionEvent pae) {
		final JPopupMenu popupMenu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(popupMenu);

		if(this.currentState == null) return;

		final String keyTier = keyTier();
		if(keyTier == null) return;

		final int selectedRow = this.morphemesTable.getSelectedRow();

		if(selectedRow >= 0) {
			final String[][] optionsForMorpheme = alignmentOptionsForMorpheme(selectedRow);
			setupMorphemeMenu(builder, selectedRow, optionsForMorpheme);

			int ypos = 0;
			for (int i = 0; i <= selectedRow; i++) {
				ypos += morphemesTable.getRowHeight(i);
			}
			popupMenu.show(this.morphemesTable, 0, ypos);
		}
	}

	private String morphemeSetMenuItemText(String[] optionSet) {
		final List<String> visibleTiers = getVisibleTiers();
		final StringBuilder bulider = new StringBuilder();
		if(visibleTiers.size() != optionSet.length) return "";

		for(int i = 1; i < visibleTiers.size(); i++) {
			final String option = optionSet[i];
			if(i > 1)
				bulider.append(" \u2194 ");
			bulider.append(option);
		}

		return bulider.toString();
	}

	private void setupMorphemeMenu(MenuBuilder builder, int morphemeIdx, String[][] options) {
		final String headerTxt = morphemeSetMenuItemText(getVisibleTiers().toArray(new String[0]));
		builder.addItem(".", headerTxt).setEnabled(false);

		for (int i = 0; i < options.length && i < 10; i++) {
			final String optionTxt = morphemeSetMenuItemText(options[i]);
			final InsertAlignedMorphemesData data = new InsertAlignedMorphemesData();
			data.moprhemeIdx = morphemeIdx;
			data.options = options[i];
			final PhonUIAction insertAlignedMorphemesAct = new PhonUIAction(this,
					"insertAlignedMorphemes", data);
			insertAlignedMorphemesAct.putValue(PhonUIAction.NAME, optionTxt);
			insertAlignedMorphemesAct.putValue(PhonUIAction.SHORT_DESCRIPTION,
					"Insert aligned values into record, replacing current words/morphemes");
			builder.addItem(".", insertAlignedMorphemesAct);
		}

		if(options.length > 10) {
			builder.addSeparator(".", "more_options");
			final PhonUIAction focusOptionsAct = new PhonUIAction(this, "onFocusAlignmentOptions", Integer.valueOf(10));
			focusOptionsAct.putValue(PhonUIAction.NAME, "See more below...");
			focusOptionsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "View more options in the Alignment Options table");
			builder.addItem(".", focusOptionsAct);
		}

		final TypeMapNode morphemeNode = this.currentState.getLeaves().get(morphemeIdx);
		final Map<String, String> alignedTypes = new LinkedHashMap<>();
		for(String tierName:getVisibleTiers()) {
			alignedTypes.put(tierName, morphemeNode.getMorpheme(tierName));
		}
		if(!this.getUserDb().hasAlignedTypes(alignedTypes)) {
			builder.addSeparator(".", "add_alignment");
			final PhonUIAction onAddAlignedTypesAct = new PhonUIAction(this, "onAddAlignedTypes");
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
		final int selectedMorpheme = this.morphemesTable.getSelectedRow();
		if(selectedMorpheme < 0 || selectedMorpheme >= this.currentState.getLeafCount()) return;

		final int selectedOption = this.alignmentOptionsTable.getSelectedRow();
		if(selectedOption < 0 || selectedOption >= this.alignmentOptionsTableModel.alignmentRows.length) return;

		final String[] types = this.alignmentOptionsTableModel.alignmentRows[selectedOption];
		if(tierNum > 0 && tierNum < getVisibleTiers().size()) {
			final String tierName = getVisibleTiers().get(tierNum);
			final String selectedType = types[tierNum];

			updateTier(selectedMorpheme, tierName, selectedType);
		}
	}

	public void showAlignmentOptionsMenu(PhonActionEvent pae) {
		final JPopupMenu popupMenu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(popupMenu);

		if(this.currentState == null) return;

		final String keyTier = keyTier();
		if(keyTier == null) return;

		final int selectedMorpheme = this.morphemesTable.getSelectedRow();
		if(selectedMorpheme < 0) return;

		final int selectedRow = this.alignmentOptionsTable.getSelectedRow();
		if(selectedRow >= 0) {
			final TypeMapNode leafNode = this.currentState.getLeaves().get(selectedMorpheme);
			setupAlignmentOptionsMenu(builder, selectedMorpheme, selectedRow, this.alignmentOptionsTableModel.alignmentRows);

			int ypos = 0;
			for (int i = 0; i <= selectedRow; i++) {
				ypos += alignmentOptionsTable.getRowHeight(i);
			}
			popupMenu.show(this.alignmentOptionsTable, 0, ypos);
		}
	}

	private void setupAlignmentOptionsMenu(MenuBuilder builder, int morphemeIdx,
	                                       int selectedSet, String[][] options) {
		String[] optionSet = (selectedSet < options.length ? options[selectedSet] : new String[0]);
		List<String> visibleTiers = getVisibleTiers();
		if(visibleTiers.size() != optionSet.length) return;

		final List<TypeMapNode> leafNodes = this.currentState.getLeaves();
		if(morphemeIdx >= leafNodes.size()) return;
		final TypeMapNode leafNode = leafNodes.get(morphemeIdx);

		for(int i = 1; i < optionSet.length; i++) {
			final String tierName = visibleTiers.get(i);
			final String option = optionSet[i];

			if(option.length() == 0) continue;

			final String optionTxt = String.format("%s: %s \u2192 %s", tierName, leafNode.getMorpheme(tierName), option);
			final String descTxt = String.format("Insert/Replace morpheme for tier %s", tierName);
			final InsertMorphemeForTierData eventData = new InsertMorphemeForTierData();
			eventData.morphemeIdx = morphemeIdx;
			eventData.tierName = tierName;
			eventData.morpheme = option;
			final PhonUIAction insertMorphemeAct = new PhonUIAction(this, "insertMorphemeForTier", eventData);
			insertMorphemeAct.putValue(PhonUIAction.NAME, optionTxt);
			insertMorphemeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, descTxt);
			insertMorphemeAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0));
			builder.addItem(".", insertMorphemeAct);
		}

		builder.addSeparator(".", "insert_all");
		final String headerTxt = morphemeSetMenuItemText(getVisibleTiers().toArray(new String[0]));
		builder.addItem(".", headerTxt).setEnabled(false);

		final InsertAlignedMorphemesData data = new InsertAlignedMorphemesData();
		data.moprhemeIdx = morphemeIdx;
		data.options = optionSet;
		final PhonUIAction insertAlignedMorphemesAct = new PhonUIAction(this,
				"insertAlignedMorphemes", data);
		insertAlignedMorphemesAct.putValue(PhonUIAction.NAME, morphemeSetMenuItemText(optionSet));
		insertAlignedMorphemesAct.putValue(PhonUIAction.SHORT_DESCRIPTION,
				"Insert aligned values into record, replacing current words/morphemes");
		builder.addItem(".", insertAlignedMorphemesAct);

		builder.addSeparator(".", "delete");
		final PhonUIAction deleteAlignedTypesAct = new PhonUIAction(this, "onDeleteAlignedTypes");
		deleteAlignedTypesAct.putValue(PhonUIAction.NAME, "Remove alignment from database");
		deleteAlignedTypesAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove aligned types from database");
		deleteAlignedTypesAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		builder.addItem(".", deleteAlignedTypesAct);
	}

	private String[][] alignmentOptionsForMorpheme(int morphemeIdx) {
		final TypeMapNode leafNode = this.currentState.getLeaves().get(morphemeIdx);

		final String keyTier = keyTier();
		final String morpheme = leafNode.getMorpheme(keyTier);
		if(morpheme.length() == 0)
			return new String[0][];
		final Map<String, String[]> alignmentOptions = leafNode.getAlignedMorphemeOptions();

		final List<String> visibleTiers = getVisibleTiers();
		final String[][] arrays = new String[visibleTiers.size()][];

		int idx = 0;
		for(String tierName:visibleTiers) {
			if(keyTier.equals(tierName))
				arrays[idx++] = new String[]{morpheme};
			else
				arrays[idx++] = (alignmentOptions.containsKey(tierName) ? alignmentOptions.get(tierName) : new String[0]);
		}

		final String[][] product = CartesianProduct.stringArrayProduct(arrays,
				(set) -> getUserDb().hasAlignedTypes(visibleTiers.toArray(new String[0]), set));
		return product;
	}

	private class InsertMorphemeForTierData {
		int morphemeIdx = 0;
		String tierName;
		String morpheme;
	}

	public void insertMorphemeForTier(PhonActionEvent pae) {
		if(!(pae.getData() instanceof InsertMorphemeForTierData)) {
			throw new IllegalArgumentException();
		}

		final InsertMorphemeForTierData data = (InsertMorphemeForTierData) pae.getData();
		updateTier(data.morphemeIdx, data.tierName, data.morpheme);
	}

	private class InsertAlignedMorphemesData {
		int moprhemeIdx = 0;
		String[] options = new String[0];
	}

	public void insertAlignedMorphemes(PhonActionEvent pae) {
		if(!(pae.getData() instanceof InsertAlignedMorphemesData)) {
			throw new IllegalArgumentException();
		}
		final InsertAlignedMorphemesData data = (InsertAlignedMorphemesData) pae.getData();
		updateRecord(data.moprhemeIdx, getVisibleTiers().toArray(new String[0]), data.options);
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
		final AlignedTypesDatabase projectDb = getUserDb();
		if(projectDb == null) return true;
		final Optional<TierInfo> tierInfoOpt =
				projectDb.getTierInfo().stream().filter(ti -> ti.getTierName().equals(tierName)).findAny();
		if(tierInfoOpt.isPresent()) {
			return tierInfoOpt.get().isVisible();
		} else {
			return false;
		}
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
	public List<String> getVisibleTiers() {
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

	private class MorphemeTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			TypeMapNode leafNode = currentState.getLeaves().get(row);

			String tier = getVisibleTiers().get(column);
			String key = leafNode.getMorpheme(keyTier());
			String morpheme = leafNode.getMorpheme(tier);

			if(column == 0) {
				// check that key exists in database
				if(!getUserDb().typeExistsInTier(key, keyTier())) {
					retVal.setFont(retVal.getFont().deriveFont(Font.ITALIC));
				}
			} else {
				// check that link exists to key
				if(!getUserDb().alignmentExists(keyTier(), key, tier, morpheme)) {
					retVal.setFont(retVal.getFont().deriveFont(Font.ITALIC));
				}
			}

			return retVal;
		}
	}

	private class MorphemesTableModel extends AbstractTableModel {

		public MorphemesTableModel() {
			super();
		}

		@Override
		public int getRowCount() {
			if(currentState == null) return 0;
			return currentState.getLeafCount();
		}

		@Override
		public int getColumnCount() {
			return getVisibleTiers().size();
		}

		public String getColumnName(int colIdx) {
			return getVisibleTiers().get(colIdx);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(currentState == null) return "";
			List<String> tierNames = getVisibleTiers();
			TypeMapNode leafNode = currentState.getLeaves().get(rowIndex);
			String tierName = (columnIndex < tierNames.size() ? tierNames.get(columnIndex) : null);
			return (tierName == null ? "" : leafNode.getMorpheme(tierName));
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
			return getVisibleTiers().size();
		}

		public String getColumnName(int colIdx) {
			return getVisibleTiers().get(colIdx) + (colIdx > 0 ? " (" + colIdx + ")" : "");
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
