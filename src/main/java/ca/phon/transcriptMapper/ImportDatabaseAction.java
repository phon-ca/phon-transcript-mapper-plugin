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
import ca.phon.util.alignedTypesDatabase.*;
import ca.phon.worker.PhonWorker;

import java.awt.event.ActionEvent;
import java.io.File;

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
		final ImportDatabaseTask importTask = new ImportDatabaseTask(getView().getUserDb(), new File(filename));
		importTask.setName(DESC);

		getView().getEditor().getStatusBar().watchTask(importTask);
		PhonWorker.invokeOnNewWorker(importTask, getView()::updateAfterDbChange);
	}

}
