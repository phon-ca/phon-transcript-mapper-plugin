package ca.phon.alignedMorpheme.ui;

import ca.phon.alignedMorpheme.*;
import ca.phon.alignedMorpheme.db.*;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.project.Project;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.util.icons.*;
import ca.phon.worker.*;

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
public class AlignedMorphemeEditorView extends EditorView {

	private JComboBox<String> keyTierBox;

	private PromptedTextField morphemeField;

	private JWindow suggestionsWindow;

	private JList<String> suggestionsList;

	private JLabel prevMorphemeLbl;

	private JLabel currentMorphemeLbl;

	private JLabel nextMorphemeLbl;

	private TierDataLayoutPanel morphemeSelectionPanel;

	public final static String NAME = "Aligned Word/Morpheme Database";

	public final static String ICON = "blank";

	private final static String PROJECT_DB_FILENAME = "__res/morphemeTagger/db.bin";

	private AlignedMorphemeDatabase projectDb;

	private int groupIdx = 0;

	private int wordIdx = 0;

	private int alignedMorphemeIdx = 0;

	private MorphemeTaggerNode currentState;

	public AlignedMorphemeEditorView(SessionEditor editor) {
		super(editor);

		init();
		loadProjectDbAsync(() -> {
			updateAfterDbLoad();
		});

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
		this.projectDb = new AlignedMorphemeDatabase();
		final File projectDbFile = projectDbFile();
		if(projectDbFile.exists()) {
			try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(projectDbFile))) {
				this.projectDb = (AlignedMorphemeDatabase) oin.readObject();
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

		this.morphemeField = new PromptedTextField("morpheme");
		this.prevMorphemeLbl = new JLabel("<");
		this.currentMorphemeLbl = new JLabel(String.format(" %d / %d ", alignedMorphemeIdx, 0));
		this.nextMorphemeLbl = new JLabel(">");

		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;

		topPanel.add(new JLabel("Key tier:"), gbc);
		++gbc.gridx;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(this.keyTierBox, gbc);

		morphemeSelectionPanel = new TierDataLayoutPanel();

		setLayout(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(morphemeSelectionPanel), BorderLayout.CENTER);
	}

	private void setState(MorphemeTaggerNode state) {
		this.currentState = state;
	}

	private void updateStateAsync(Runnable onFinish) {
		PhonWorker.invokeOnNewWorker(this::updateState, onFinish);
	}

	private void updateState() {
		setState(stateFromRecord(getEditor().currentRecord()));
	}

	private void updateFromCurrentState() {
//		morphemeSelectionPanel.removeAll();
//		for(TierInfo ti:this.projectDb.getTierInfo()) {
//			if(ti.isVisible()) {
//				final JLabel tierLbl = new JLabel(ti.getTierName());
//				tierLbl.setFont(FontPreferences.getTitleFont());
//				morphemeSelectionPanel.add(tierLbl, new TierDataConstraint((i + 1), 0));
//			}
//		}
		morphemeSelectionPanel.removeAll();

		final Record record = getEditor().currentRecord();
		if(record == null || currentState == null) return;

		for(int gidx = 0; gidx < record.numberOfGroups(); gidx++) {
			JLabel glbl = new JLabel(String.format("Group %d", gidx+1));
			glbl.setFont(FontPreferences.getTitleFont());
			morphemeSelectionPanel.add(glbl, new TierDataConstraint(0, gidx));

			JTable morphemeTable = new JTable(new GroupMorphemesTableModel(gidx));
			morphemeTable.setPreferredScrollableViewportSize(new Dimension(morphemeTable.getPreferredScrollableViewportSize().width, 100));
			morphemeSelectionPanel.add(new JScrollPane(morphemeTable), new TierDataConstraint(1, gidx));
		}
		revalidate();
		repaint();
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

	private MorphemeTaggerNode stateFromRecord(Record record) {
		MorphemeTaggerNode root = new MorphemeTaggerNode(-1);

		List<String> tierList = getTiers();

		for(int gidx = 0; gidx < record.numberOfGroups(); gidx++) {
			Group grp = record.getGroup(gidx);
			MorphemeTaggerNode grpNode = new MorphemeTaggerNode(gidx);
			root.addChild(grpNode);
			for(int widx = 0; widx < grp.getAlignedWordCount(); widx++) {
				Word wrd = grp.getAlignedWord(widx);
				MorphemeTaggerNode wrdNode = new MorphemeTaggerNode(widx);
				grpNode.addChild(' ', wrdNode);

				AlignedMorphemes morphemes = wrd.getExtension(AlignedMorphemes.class);
				if(morphemes != null) {
					for(int midx = 0; midx < morphemes.getMorphemeCount(); midx++) {
						AlignedMorpheme morpheme = morphemes.getAlignedMorpheme(midx);

						Map<String, String> currentMorphemes = new HashMap<>();
						for(String tierName:tierList) {
							currentMorphemes.put(tierName, morpheme.getMorphemeText(tierName));
						}
						Map<String, String[]> alignedMorphemes =
								this.projectDb.alignedMorphemesForTier(keyTierBox.getSelectedItem().toString(), currentMorphemes.get(keyTierBox.getSelectedItem()));

						MorphemeTaggerNode morphemeNode = new MorphemeTaggerNode(midx, currentMorphemes, alignedMorphemes);

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

	public List<String> getTiers() {
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
		// move key tier to front
		tierList.remove(keyTierBox.getSelectedItem());
		tierList.add(0, keyTierBox.getSelectedItem().toString());
		return tierList;
	}

	private class GroupMorphemesTableModel extends AbstractTableModel {

		final int groupIdx;

		public GroupMorphemesTableModel(int groupIdx) {
			super();

			this.groupIdx = groupIdx;
		}

		@Override
		public int getRowCount() {
			if(currentState == null) return 0;

			if(this.groupIdx < currentState.childCount()) {
				MorphemeTaggerNode groupNode = currentState.getChild(this.groupIdx);
				return groupNode.getLeafCount();
			} else {
				return 0;
			}
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

			if(this.groupIdx < currentState.childCount()) {
				MorphemeTaggerNode groupNode = currentState.getChild(this.groupIdx);
				MorphemeTaggerNode leafNode = groupNode.getLeaves().get(rowIndex);

				String tierName = tierNames.get(columnIndex);
				return leafNode.getMorpheme(tierName);
			} else {
				return "";
			}
		}

	}

}
