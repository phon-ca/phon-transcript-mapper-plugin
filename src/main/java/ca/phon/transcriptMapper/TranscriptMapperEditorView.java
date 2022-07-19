package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.AddTierEdit;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.project.Project;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.alignedMorphemes.*;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aligned morpheme editor view for Phon sessions. This view will display the aligned morpheme lookup data
 * for the current record and provide method for inserting aligned morpheme data and updating the aligned
 * morpheme database(s).
 *
 */
public class TranscriptMapperEditorView extends EditorView {

	private JToolBar toolbar;

	private JLabel keyLabel;

	private JComboBox<String> keyTierBox;

	private JLabel morphemesLabel;

	private MorphemesTableModel morphemesTableModel;

	private JXTable morphemesTable;

	private JWindow suggestionsWindow;

	private JList<String> suggestionsList;

	private TierDataLayoutPanel morphemeSelectionPanel;

	public final static String NAME = "Transcript Mapper";

	public final static String ICON = "blank";

	private final static String PROJECT_DB_FILENAME = "__res/pluginData/transcriptMapper/typeMap" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private AlignedTypesDatabase projectDb;

	private TypeMapNode currentState;

	public TranscriptMapperEditorView(SessionEditor editor) {
		super(editor);

		init();
		loadProjectDbAsync(this::updateAfterDbLoad);

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
		updateStateAsync(this::updateFromCurrentState);
	}

	@RunOnEDT
	public void onTierViewChanged(EditorEvent ee) { updateAfterDbLoad(); }

	private File projectDbFile() {
		final Project project = getEditor().getProject();
		final File dbFile = new File(project.getLocation(), PROJECT_DB_FILENAME);
		return dbFile;
	}

	AlignedTypesDatabase getProjectDb() {
		return this.projectDb;
	}

	private void loadProjectDbAsync(Runnable onFinish) {
		final PhonTask task = PhonWorker.invokeOnNewWorker(this::loadProjectDb, onFinish, LogUtil::warning);
		task.setName("Loading aligned morpheme database");
		getEditor().getStatusBar().watchTask(task);
	}

	private void loadProjectDb() {
		this.projectDb = new AlignedTypesDatabase();
		final File projectDbFile = projectDbFile();
		if(projectDbFile.exists()) {
			try {
				this.projectDb = AlignedTypesDatabaseIO.readFromFile(projectDbFile);
			} catch (IOException e) {
				LogUtil.warning(e);
			}
		}
	}

	void saveProjectDbAsync(Runnable onFinish) {
		final PhonTask task = PhonWorker.invokeOnNewWorker(this::saveProjectDb, onFinish, LogUtil::warning);
		task.setName("Saving aligned morpheme database");
		getEditor().getStatusBar().watchTask(task);
	}

	private void saveProjectDb() {
		final File projectDbFile = projectDbFile();
		final File parentFolder = projectDbFile.getParentFile();
		if(!parentFolder.exists()) {
			parentFolder.mkdirs();
		}

		try {
			AlignedTypesDatabaseIO.writeToFile(this.projectDb, projectDbFile);
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}

	private void setupToolbar() {
		toolbar = new JToolBar();
		add(toolbar, BorderLayout.NORTH);

		final JPopupMenu dbMenu = new JPopupMenu("Database");
		final MenuBuilder dbMenuBuilder = new MenuBuilder(dbMenu);
		dbMenuBuilder.addItem(".", new ImportDatabaseAction(this));
		dbMenuBuilder.addSeparator(".", "csv");
		dbMenuBuilder.addItem(".", new ImportCSVAction(this));
		dbMenuBuilder.addItem(".", new ExportCSVAction(this));

		PhonUIAction dbMenuAct = new PhonUIAction(this, "noop");
		dbMenuAct.putValue(PhonUIAction.NAME, "Database");
		dbMenuAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show database menu");
		dbMenuAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon(ICON, IconSize.SMALL));
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
		tiersMenuAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon(ICON, IconSize.SMALL));
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
				toggleTierItem.setEnabled(sessionTierVisible(tierName));
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
			updateFromCurrentState();
			if(morphemesTableModel != null)
				morphemesTableModel.fireTableStructureChanged();
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
		morphemesTable.setSortable(false);
		morphemesTable.setVisibleRowCount(10);
		morphemeSelectionPanel.add(new JScrollPane(morphemesTable), new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		add(new JScrollPane(morphemeSelectionPanel), BorderLayout.CENTER);
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
	}

	void updateAfterDbLoad() {
		if(this.projectDb == null) return;

		this.keyTierBox.setModel(new DefaultComboBoxModel<>(getVisibleTiers().toArray(new String[0])));
		this.keyTierBox.setSelectedItem(SystemTierType.Orthography.getName());

		if(getEditor().currentRecord() != null) {
			updateStateAsync(() -> {
				if(this.morphemesTableModel != null)
					this.morphemesTableModel.fireTableStructureChanged();
				this.updateFromCurrentState();
			});
		}
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
		return new JMenu();
	}

	private TypeMapNode stateFromRecord(Record record) {
		TypeMapNode root = new TypeMapNode(-1);

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
								this.projectDb.alignedTypesForTier(keyTier(), currentMorphemes.get(keyTierBox.getSelectedItem()));

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
		Optional<TierInfo> tierInfoOpt =
				this.projectDb.getTierInfo().stream().filter(ti -> ti.getTierName().equals(tierName)).findAny();
		if(tierInfoOpt.isPresent()) {
			TierInfo tierInfo = tierInfoOpt.get();
			tierInfo.setVisible(!tierInfo.isVisible());

			updateAfterDbLoad();
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

	/**
	 * Set of all aligned tiers in both the record and the database
	 *
	 * @return list of all tiers
	 */
	public List<String> allTiers() {
		Set<String> tierSet = new LinkedHashSet<>();
		tierSet.addAll(sessionGroupedTiers());
		tierSet.addAll(this.projectDb.tierNames());
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
		Optional<TierInfo> tierInfoOpt =
				this.projectDb.getTierInfo().stream().filter(ti -> ti.getTierName().equals(tierName)).findAny();
		if(tierInfoOpt.isPresent()) {
			return tierInfoOpt.get().isVisible();
		} else {
			return false;
		}
	}

	private boolean isGroupedTier(TierViewItem tvi) {
		SystemTierType systemTier = SystemTierType.tierFromString(tvi.getTierName());
		if(systemTier != null) {
			return systemTier.isGrouped();
		} else {
			for(TierDescription td:getEditor().getSession().getUserTiers()) {
				if (td.getName().equals(tvi.getTierName())) {
					return td.isGrouped();
				}
			}
		}
		return false;
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
			if(row < offset + groupNode.childCount())
				return gidx;
			offset += groupNode.childCount();
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
		if(this.projectDb != null) {
			return tierList.stream().filter(this::dbTierVisible).collect(Collectors.toList());
		} else {
			return tierList;
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

}
