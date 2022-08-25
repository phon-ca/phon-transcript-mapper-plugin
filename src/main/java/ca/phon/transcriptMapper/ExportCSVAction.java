/*
 * Copyright (C) 2005-2022 Gregory Hedlund & Yvan Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.phon.transcriptMapper;

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
		final ExportCSVTask exportTask = new ExportCSVTask(getView().getUserDb(), new File(filename),
				getView().keyTier(), getView().getVisibleTiers().toArray(new String[0]));
		exportTask.setName(DESC);

		getView().getEditor().getStatusBar().watchTask(exportTask);
		PhonWorker.invokeOnNewWorker(exportTask, getView()::updateAfterDbLoad);
	}

}
