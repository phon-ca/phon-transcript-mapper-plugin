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

import ca.phon.app.actions.CheckForChangesOnExit;
import ca.phon.plugin.*;
import com.sun.jna.platform.win32.Netapi32Util;

import java.io.IOException;

public class SaveUserATDBOnExit implements CheckForChangesOnExit, IPluginExtensionPoint<CheckForChangesOnExit> {
	@Override
	public String getName() {
		return "Aligned types database";
	}

	@Override
	public boolean hasChanges() {
		return UserATDB.getInstance().isATDBLoaded() && UserATDB.getInstance().isModified();
	}

	@Override
	public void save() throws IOException {
		UserATDB.getInstance().saveDb();
	}

	@Override
	public Class<?> getExtensionType() {
		return CheckForChangesOnExit.class;
	}

	@Override
	public IPluginExtensionFactory<CheckForChangesOnExit> getFactory() {
		return (args -> this);
	}
}
