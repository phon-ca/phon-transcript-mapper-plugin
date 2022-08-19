package ca.phon.transcriptMapper;

import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.icons.*;

import java.awt.event.ActionEvent;

public class SaveDatabaseAction extends TranscriptMapperAction {

	public SaveDatabaseAction(TranscriptMapperEditorView view) {
		super(view);

		putValue(PhonUIAction.NAME, "Save database");
		putValue(PhonUIAction.SHORT_DESCRIPTION, "Save changes to user aligned types database");
		putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("actions/document-save", IconSize.SMALL));
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		getView().saveUserDbAsync(() -> {});
	}

}
