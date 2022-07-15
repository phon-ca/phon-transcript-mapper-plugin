package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.project.Project;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.alignedMorphemes.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
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

	private final static String PROJECT_DB_FILENAME = "__res/pluginData/transcriptMapper/db.bin";

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
	}

	@RunOnEDT
	public void onRecordChanged(EditorEvent ee) {
		updateStateAsync(this::updateFromCurrentState);
	}

	@RunOnEDT
	public void onTierChanged(EditorEvent ee) {
		updateStateAsync(this::updateFromCurrentState);
	}

	private File projectDbFile() {
		final Project project = getEditor().getProject();
		final File dbFile = new File(project.getLocation(), PROJECT_DB_FILENAME);
		return dbFile;
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
			try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(projectDbFile))) {
				this.projectDb = (AlignedTypesDatabase) oin.readObject();
			} catch (IOException | ClassNotFoundException e) {
				LogUtil.warning(e);
			}
		}
	}

	private void saveProjectDbAsync(Runnable onFinish) {
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

		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(projectDbFile))) {
			out.writeObject(this.projectDb);
			out.flush();
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}

	private void init() {
		this.keyTierBox = new JComboBox<>();
		this.keyTierBox.addItemListener(e -> {
			updateFromCurrentState();
			if(morphemesTableModel != null)
				morphemesTableModel.fireTableStructureChanged();
		});

		morphemeSelectionPanel = new TierDataLayoutPanel();

		int row = 0;
		keyLabel = new JLabel("Key tier");
		keyLabel.setFont(FontPreferences.getTitleFont());
		morphemeSelectionPanel.add(keyLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));
		morphemeSelectionPanel.add(keyTierBox, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		++row;
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		morphemeSelectionPanel.add(sep, new TierDataConstraint(TierDataConstraint.FULL_TIER_COLUMN, row));

		++row;
		morphemesLabel = new JLabel("Morphemes");
		morphemesLabel.setFont(FontPreferences.getTitleFont());
		morphemeSelectionPanel.add(morphemesLabel, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, row));

		morphemesTableModel = new MorphemesTableModel();
		morphemesTable = new JXTable(morphemesTableModel);
		morphemesTable.setVisibleRowCount(10);
		morphemeSelectionPanel.add(new JScrollPane(morphemesTable), new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, row));

		setLayout(new BorderLayout());
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

	private void updateAfterDbLoad() {
		if(this.projectDb == null) return;

		this.keyTierBox.setModel(new DefaultComboBoxModel<>(this.projectDb.tierNames().toArray(new String[0])));
		this.keyTierBox.setSelectedItem(SystemTierType.Orthography.getName());

		if(getEditor().currentRecord() != null) {
			updateStateAsync(this::updateFromCurrentState);
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

		List<String> tierList = getTiers();

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
								this.projectDb.alignedTypesForTier(keyTierBox.getSelectedItem().toString(), currentMorphemes.get(keyTierBox.getSelectedItem()));

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
	 * The list of tiers visible in the morpheme table
	 *
	 * @return list of tiers visible in the morpheme table
	 *
	 */
	public List<String> getTiers() {
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
		return tierList;
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
			return getTiers().size();
		}

		public String getColumnName(int colIdx) {
			return getTiers().get(colIdx);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(currentState == null) return "";
			List<String> tierNames = getTiers();
			TypeMapNode leafNode = currentState.getLeaves().get(rowIndex);
			String tierName = tierNames.get(columnIndex);
			return leafNode.getMorpheme(tierName);
		}

	}

}
