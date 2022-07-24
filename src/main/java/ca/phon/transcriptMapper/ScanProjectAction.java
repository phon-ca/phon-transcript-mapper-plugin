package ca.phon.transcriptMapper;

import ca.phon.project.Project;

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

		final ScanProjectWizard wizard = new ScanProjectWizard(project, "Scan Project");
		wizard.showWizard();
	}

}
