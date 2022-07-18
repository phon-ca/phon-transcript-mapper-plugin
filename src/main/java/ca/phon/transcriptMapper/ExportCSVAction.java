package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.app.log.LogUtil;
import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.worker.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

public class ExportCSVAction extends TranscriptMapperAction {

	public static String TXT = "Export data to csv file...";

	public static String DESC = "Export data for selected key tier to csv file";

	public ExportCSVAction(TranscriptMapperEditorView view) {
		super(view);

		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent actionEvent) {
		final SaveDialogProperties props = new SaveDialogProperties();
		props.setParentWindow(getView().getEditor());
		props.setRunAsync(true);
		props.setCanCreateDirectories(true);
		props.setFileFilter(FileFilter.csvFilter);
		props.setInitialFile("typeMap_" + getView().keyTier() + ".csv");
		props.setListener((e) -> {
			if(e.getDialogResult() == NativeDialogEvent.OK_OPTION) {
				final String filename = e.getDialogData().toString();
				exportDatabaseAsCSV(filename);
			}
		});

		NativeDialogs.showSaveDialog(props);
	}

	private void exportDatabaseAsCSV(String filename) {
		final PhonTask exportTask = new PhonTask() {
			@Override
			public void performTask() {
				super.setStatus(TaskStatus.RUNNING);
				try {
					getView().getProjectDb().exportToCSV(getView().keyTier(), new File(filename), "UTF-8");
					super.setStatus(TaskStatus.FINISHED);
				} catch (IOException e) {
					Toolkit.getDefaultToolkit().beep();
					LogUtil.severe(e);
					super.err = e;
					super.setStatus(TaskStatus.ERROR);
				}
			}
		};

		PhonWorker.invokeOnNewWorker(exportTask, getView()::updateAfterDbLoad);
	}

}
