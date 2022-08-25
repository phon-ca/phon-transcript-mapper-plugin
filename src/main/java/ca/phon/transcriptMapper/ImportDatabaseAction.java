package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.app.log.LogUtil;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.alignedTypesDatabase.*;
import ca.phon.worker.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Import an existing {@link AlignedTypesDatabase} into
 * this database.
 */
public class ImportDatabaseAction extends TranscriptMapperAction {

	public static final String TXT = "Import database from file...";

	public static final String DESC = "Import database entries from another database file";

	private static final String ICON = "blank";

	public ImportDatabaseAction(TranscriptMapperEditorView view) {
		super(view);

		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent actionEvent) {
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(getView().getEditor());
		props.setRunAsync(true);
		props.setCanChooseFiles(true);
		props.setCanChooseDirectories(true);
		props.setCanCreateDirectories(false);
		props.setAllowMultipleSelection(false);
		String formatDesc = String.format("Transcript mapper database (%s;%s)", AlignedTypesDatabaseIO.DB_EXT,
				AlignedTypesDatabaseIO.DBZ_EXT);
		String extensions = String.format("%s;%s", AlignedTypesDatabaseIO.DB_EXT.substring(1),
				AlignedTypesDatabaseIO.DBZ_EXT.substring(1));
		final FileFilter filter = new FileFilter(formatDesc, extensions);
		props.setFileFilter(filter);
		props.setListener((e) -> {
			if(e.getDialogResult() == NativeDialogEvent.OK_OPTION) {
				final String filename = e.getDialogData().toString();
				importDatabaseFromFile(filename);
			}
		});
		NativeDialogs.showOpenDialog(props);
	}

	private void importDatabaseFromFile(String filename) {
		final PhonTask importTask = new PhonTask() {
			@Override
			public void performTask() {
				super.setStatus(TaskStatus.RUNNING);
				try {
					AlignedTypesDatabase importDb = AlignedTypesDatabaseIO.readFromFile(filename);

					// TODO display interface for modifying imported values

					getView().getUserDb().importDatabase(importDb);
					super.setStatus(TaskStatus.FINISHED);
				} catch (IOException e) {
					Toolkit.getDefaultToolkit().beep();
					LogUtil.severe(e);
					super.err = e;
					super.setStatus(TaskStatus.ERROR);
				}
			}
		};
		importTask.setName(DESC);

		getView().getEditor().getStatusBar().watchTask(importTask);
		PhonWorker.invokeOnNewWorker(importTask, getView()::updateAfterDbChange);
	}

}
