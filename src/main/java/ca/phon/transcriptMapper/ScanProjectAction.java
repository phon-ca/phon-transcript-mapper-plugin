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

import ca.phon.project.Project;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ScanProjectAction extends TranscriptMapperAction {

	public final static String TXT = "Scan project...";

	public final static String DESC = "Import aligned type data by scanning sessions";

	public ScanProjectAction(TranscriptMapperEditorView view) {
		super(view);

		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent actionEvent) {
		final Project project = getView().getEditor().getProject();

		final ScanProjectWizard wizard = new ScanProjectWizard(project, getView(),"Scan Project") {
			@Override
			public void onFinishScan() {
				super.onFinishScan();
				SwingUtilities.invokeLater(getView()::updateAfterDbLoad);
			}
		};
		wizard.showWizard();
	}

}
