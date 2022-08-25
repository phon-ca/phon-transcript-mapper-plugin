package ca.phon.transcriptMapper;

import ca.phon.util.alignedTypesDatabase.AlignedTypesDatabase;
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
