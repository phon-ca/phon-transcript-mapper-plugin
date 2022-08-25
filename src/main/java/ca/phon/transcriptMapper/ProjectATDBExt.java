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
