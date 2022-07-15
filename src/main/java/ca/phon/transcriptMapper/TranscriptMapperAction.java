package ca.phon.transcriptMapper;

import ca.phon.app.hooks.HookableAction;

public abstract class TranscriptMapperAction extends HookableAction {

	private final TranscriptMapperEditorView view;

	public TranscriptMapperAction(TranscriptMapperEditorView view) {
		super();
		this.view = view;
	}

	public TranscriptMapperEditorView getView() {
		return this.view;
	}

}
