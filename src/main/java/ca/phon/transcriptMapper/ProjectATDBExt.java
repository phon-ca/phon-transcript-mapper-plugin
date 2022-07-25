package ca.phon.transcriptMapper;

import ca.phon.extensions.*;
import ca.phon.project.Project;

@Extension(Project.class)
public final class ProjectATDBExt implements ExtensionProvider {

	@Override
	public void installExtension(IExtendable iExtendable) {
		if(!(iExtendable instanceof Project))
			throw new IllegalArgumentException();
		final Project project = (Project) iExtendable;
		project.putExtension(ProjectATDB.class, new ProjectATDB(project));
	}

}
