package ca.phon.transcriptMapper;

import ca.phon.app.log.LogUtil;
import ca.phon.project.Project;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

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

		final ScanProjectWizard wizard = new ScanProjectWizard(project, getView().getUserDb(),"Scan Project") {
			@Override
			public void onFinishScan() {
				super.onFinishScan();
				final ProjectATDB projectATDB = project.getExtension(ProjectATDB.class);
				if(projectATDB != null) {
					try {
						projectATDB.saveProjectDb();
					} catch (IOException e) {
						Toolkit.getDefaultToolkit().beep();
						LogUtil.severe(e);
					}
				}
				getView().updateAfterDbLoad();
			}
		};
		wizard.showWizard();
	}

}
