package ca.phon.alignedMorpheme.ui;

import ca.phon.alignedMorpheme.*;
import ca.phon.alignedMorpheme.db.AlignedMorphemeDatabase;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.project.Project;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.util.icons.*;
import ca.phon.worker.PhonWorker;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Map;

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

	public final static String NAME = "Aligned Word/Morpheme";

	public final static String ICON = "blank";

	private final static String PROJECT_DB_FILENAME = "__res/morphemeTagger/db.bin";

	private AlignedMorphemeDatabase projectDb;

	private String[] tiers;

	private int groupIdx = 0;

	private int wordIdx = 0;

	private int alignedMorphemeIdx = 0;

	public AlignedMorphemeEditorView(SessionEditor editor) {
		super(editor);

		this.tiers = new String[]{SystemTierType.IPATarget.getName(), SystemTierType.IPAActual.getName()};

		init();
		loadProjectDbAsync(() -> {
			update();
		});
	}

	private File projectDbFile() {
		final Project project = getEditor().getProject();
		final File dbFile = new File(project.getLocation(), PROJECT_DB_FILENAME);
		return dbFile;
	}

	private void loadProjectDbAsync(Runnable onFinish) {
		final PhonWorker worker = PhonWorker.createWorker();
		worker.setFinishWhenQueueEmpty(true);

		worker.invokeLater(this::loadProjectDb);
		worker.setFinalTask(onFinish);

		worker.start();
	}

	private void loadProjectDb() {
		final File projectDbFile = projectDbFile();
		if(projectDbFile.exists()) {
			try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(projectDbFile))) {
				this.projectDb = (AlignedMorphemeDatabase) oin.readObject();
			} catch (IOException | ClassNotFoundException e) {
				LogUtil.severe(e);
				this.projectDb = new AlignedMorphemeDatabase();
			}
		} else {
			this.projectDb = new AlignedMorphemeDatabase();
		}
	}

	private void saveProjectDbAsync(Runnable onFinish) {
		final PhonWorker worker = PhonWorker.createWorker();
		worker.setFinishWhenQueueEmpty(true);

		worker.invokeLater(this::saveProjectDb);
		worker.setFinalTask(onFinish);

		worker.start();
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
		this.currentMorphemeLbl = new JLabel(String.format("%d / %d", alignedMorphemeIdx, 0));
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

		++gbc.gridy;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		topPanel.add(new JLabel("Morpheme:"), gbc);
		++gbc.gridx;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(this.morphemeField, gbc);
		++gbc.gridx;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		topPanel.add(this.prevMorphemeLbl, gbc);
		++gbc.gridx;
		topPanel.add(this.currentMorphemeLbl, gbc);
		++gbc.gridx;
		topPanel.add(this.nextMorphemeLbl, gbc);

		morphemeSelectionPanel = new TierDataLayoutPanel();

		setLayout(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(morphemeSelectionPanel), BorderLayout.CENTER);
	}

	private void update() {
		if(this.projectDb == null) return;

		this.keyTierBox.setModel(new DefaultComboBoxModel<>(tiers));
		this.keyTierBox.setSelectedItem(SystemTierType.Orthography.getName());

		morphemeSelectionPanel.removeAll();
		for(int i = 0; i < tiers.length; i++) {
			final String tierName = tiers[i];
			final JLabel tierLbl = new JLabel(tierName);
			tierLbl.setFont(FontPreferences.getTitleFont());
			morphemeSelectionPanel.add(tierLbl, new TierDataConstraint((i+1), 0));
		}

//		updateCurrentMorpheme();
	}

	private void updateCurrentMorpheme() {
		final Record record = getEditor().currentRecord();
		if(record == null) return;

		final String keyTier = (String) this.keyTierBox.getSelectedItem();
		if(keyTier == null) return;
		SystemTierType systemTier = SystemTierType.tierFromString(keyTier);
		String morpheme = "";

		Group alignedGroup = record.getGroup(this.groupIdx);
		Word alignedWord = (this.wordIdx < alignedGroup.getAlignedWordCount() ? alignedGroup.getAlignedWord(this.wordIdx) : null);
		if(alignedWord == null) return;

		AlignedMorphemes alignedMorphemes = alignedWord.getExtension(AlignedMorphemes.class);
		if(alignedMorphemes == null) return;

		AlignedMorpheme alignedMorpheme = (this.alignedMorphemeIdx < alignedMorphemes.getMorphemeCount()
				? alignedMorphemes.getAlignedMorpheme(this.alignedMorphemeIdx) : null);
		if(alignedMorpheme == null) return;

		if(systemTier != null) {
			switch(systemTier) {
				case Orthography -> {
					morpheme = (alignedMorpheme.getOrthography() != null ? alignedMorpheme.getOrthography().toString() : "");
				}

				case IPATarget -> {
					morpheme = (alignedMorpheme.getIPATarget() != null ? alignedMorpheme.getIPATarget().toString() : "");
				}

				case IPAActual -> {
					morpheme = (alignedMorpheme.getIPAActual() != null ? alignedMorpheme.getIPAActual().toString() : "");
				}

				default -> {

				}
			}
		} else {
			morpheme = (alignedMorpheme.getUserTier(keyTier) != null ? alignedMorpheme.getUserTier(keyTier).toString() : "");
		}

		this.morphemeField.setText(morpheme);
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

}
