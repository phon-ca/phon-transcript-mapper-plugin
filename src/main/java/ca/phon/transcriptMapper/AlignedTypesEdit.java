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

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.undo.SessionEditorUndoableEdit;

public class AlignedTypesEdit extends SessionEditorUndoableEdit {

	public static enum Operation {
		ADD,
		REMOVE
	};

	private final TranscriptMapperEditorView view;

	private final String[] tierNames;

	private final String[] types;

	private final Operation operation;

	/**
	 * Constructor
	 *
	 * @param editor
	 */
	public AlignedTypesEdit(SessionEditor editor, TranscriptMapperEditorView view,
	                        Operation operation, String[] tierNames, String[] types) {
		super(editor);

		this.view = view;
		this.tierNames = tierNames;
		this.types = types;
		this.operation = operation;
	}

	@Override
	public void doIt() {
		final AlignedTypesDatabase db = view.getUserDb();
		switch (operation) {
			case ADD -> db.addAlignedTypes(tierNames, types);
			case REMOVE -> db.removeAlignedTypes(tierNames, types);
		}
		view.updateAfterDbChange();
	}

	@Override
	public void undo() {
		final AlignedTypesDatabase db = view.getUserDb();
		switch (operation) {
			case ADD -> db.removeAlignedTypes(tierNames, types);
			case REMOVE -> db.addAlignedTypes(tierNames, types);
		}
		view.updateAfterDbChange();
	}

}
