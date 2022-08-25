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

import ca.phon.ui.nativedialogs.*;
import ca.phon.worker.PhonWorker;

import java.awt.event.ActionEvent;
import java.io.File;

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
		final ImportCSVTask importTask = new ImportCSVTask(getView().getUserDb(), new File(filename));
		importTask.setName(DESC);

		getView().getEditor().getStatusBar().watchTask(importTask);
		PhonWorker.invokeOnNewWorker(importTask, getView()::updateAfterDbChange);
	}

}
