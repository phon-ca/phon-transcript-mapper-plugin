package ca.phon.alignedMorpheme.ui;

import ca.phon.alignedMorpheme.db.AlignedMorphemeDatabase;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.project.Project;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.util.icons.*;
import ca.phon.worker.PhonWorker;

import javax.swing.*;
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

	private JPanel tierValuesPanel;

	private Map<String, JTextField[]> currentValueFieldMap;

	private Map<String, JList<String>> tierOptionListMap;

	public final static String NAME = "Aligned Word/Morpheme";

	public final static String ICON = "blank";

	private final static String PROJECT_DB_FILENAME = "__res/morphemeTagger/db.bin";

	private AlignedMorphemeDatabase projectDb;

	public AlignedMorphemeEditorView(SessionEditor editor) {
		super(editor);

		loadProjectDbAsync();
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
