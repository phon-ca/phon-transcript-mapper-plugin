package ca.phon.alignedMorpheme.ui;

import ca.phon.alignedMorpheme.*;
import ca.phon.alignedMorpheme.db.AlignedMorphemeDatabase;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.orthography.OrthoElement;
import ca.phon.project.Project;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.util.Tuple;
import ca.phon.util.icons.*;
import ca.phon.worker.PhonWorker;
import org.jdesktop.swingx.plaf.AbstractUIChangeHandler;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

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

	private MorphemeTaggerNode currentState;

	public AlignedMorphemeEditorView(SessionEditor editor) {
		super(editor);

		this.tiers = new String[]{SystemTierType.Orthography.getName(), SystemTierType.IPATarget.getName(), SystemTierType.IPAActual.getName()};

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

		if(getEditor().currentRecord() != null)
			this.currentState = stateFromRecord(getEditor().currentRecord());
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

						String morphemeText = morpheme.getMorphemeText(keyTierBox.getSelectedItem().toString());
						Map<String, String[]> alignedMorphemes =
								this.projectDb.alignedMorphemesForTier(keyTierBox.getSelectedItem().toString(), morphemeText);

						MorphemeTaggerNode morphemeNode = new MorphemeTaggerNode(midx, morphemeText, alignedMorphemes);

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

		printTree(root);

		return root;
	}

	private void printTree(MorphemeTaggerNode tree) {
		StringBuffer buffer = new StringBuffer();
		treeToString(buffer, tree);
		System.out.println(buffer.toString());
	}

	private void treeToString(StringBuffer buffer, MorphemeTaggerNode node) {
		if(node.isTerminated()) {
			buffer.append(String.format("%s\n", node.getMorpheme()));
			for(String alignedTier:node.getAlignedMorphemeOptions().keySet()) {
				String[] opts = node.alignedMorphemeOptions.get(alignedTier);
				buffer.append(String.format("\t%s:\n", alignedTier));
				buffer.append(String.format("\t\t%s\n", Arrays.toString(opts)));
			}
		} else {
			for(int cidx = 0; cidx < node.childCount(); cidx++) {
				treeToString(buffer, node.getChild(cidx));
			}
		}
	}

	private class MorphemeTaggerNode {

		// index of group/word/morpheme
		private int index;

		// group/word/morpheme value
		private String morpheme;

		private Map<String, String[]> alignedMorphemeOptions;

		private List<Tuple<Character, MorphemeTaggerNode>> children = new ArrayList<>();

		public MorphemeTaggerNode(int idx) {
			this(idx, null, null);
		}

		public MorphemeTaggerNode(int idx, String token) {
			this(idx, token, null);
		}

		public MorphemeTaggerNode(int idx, String morpheme, Map<String, String[]> alignedMorphemeOptions) {
			super();
			this.index = idx;
			this.morpheme = morpheme;
			this.alignedMorphemeOptions = alignedMorphemeOptions;
		}

		public boolean isRoot() {
			return this.index < 0;
		}

		public List<Tuple<Character, MorphemeTaggerNode>> getChildren() {
			return this.children;
		}

		public boolean isTerminated() {
			return this.morpheme != null;
		}

		public String getMorpheme() {
			return this.morpheme;
		}

		public void setMorpheme(String morpheme) {
			this.morpheme = morpheme;
		}

		public Map<String, String[]> getAlignedMorphemeOptions() {
			return this.alignedMorphemeOptions;
		}

		public void setAlignedMorphemeOptions(Map<String, String[]> alignedMorphemeOptions) {
			this.alignedMorphemeOptions = alignedMorphemeOptions;
		}

		public int childCount() {
			return this.children.size();
		}

		public MorphemeTaggerNode getChild(int cidx) {
			return this.children.get(cidx).getObj2();
		}

		public void addChild(MorphemeTaggerNode cnode) {
			addChild('~', cnode);
		}

		public void addChild(Character ch, MorphemeTaggerNode cnode) {
			this.children.add(new Tuple<>(ch, cnode));
		}

	}

}
