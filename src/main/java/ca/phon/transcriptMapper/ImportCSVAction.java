package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.app.log.LogUtil;
import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.worker.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

public class ImportCSVAction extends TranscriptMapperAction {

	public static String TXT = "Import data from csv file...";

	public static String DESC = "Import entries from a csv file";

	public ImportCSVAction(TranscriptMapperEditorView view) {
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
		props.setFileFilter(FileFilter.csvFilter);
		props.setListener((e) -> {
			if(e.getDialogResult() == NativeDialogEvent.OK_OPTION) {
				final String filename = e.getDialogData().toString();
				importDatabaseFromCSV(filename);
			}
		});
		NativeDialogs.showOpenDialog(props);
	}

	private void importDatabaseFromCSV(String filename) {
		final PhonTask importTask = new PhonTask() {
			@Override
			public void performTask() {
				super.setStatus(TaskStatus.RUNNING);
				try {
					getView().getProjectDb().importFromCSV(new File(filename));
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
		PhonWorker.invokeOnNewWorker(importTask, () -> {
			getView().saveProjectDbAsync(getView()::updateAfterDbLoad);
		});
	}

}
