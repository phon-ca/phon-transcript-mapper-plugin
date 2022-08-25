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

import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.Language;
import ca.phon.worker.PhonWorker;

import java.awt.event.ActionEvent;

public class ImportIPADictionaryAction extends TranscriptMapperAction {

	private final Language dictLang;

	public ImportIPADictionaryAction(TranscriptMapperEditorView view, Language dictLang) {
		super(view);

		this.dictLang = dictLang;

		putValue(NAME, dictLang.toString());
		putValue(PhonUIAction.SHORT_DESCRIPTION, "Import IPA dictionary '" + dictLang.toString() + "'");
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final ImportIPADictionaryTask importTask = new ImportIPADictionaryTask(getView().getUserDb(), dictLang);
		importTask.setName("Import IPA Dictionary '" + dictLang.toString() + "'");
		getView().getEditor().getStatusBar().watchTask(importTask);

		PhonWorker.invokeOnNewWorker(importTask, getView()::updateAfterDbChange);
	}

}
