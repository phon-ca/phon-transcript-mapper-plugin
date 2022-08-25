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

import ca.phon.app.hooks.PhonStartupHook;
import ca.phon.app.log.LogUtil;
import ca.phon.plugin.*;
import ca.phon.worker.PhonWorker;
import jline.internal.Log;

import java.io.IOException;

public class UserATDBStartupHook implements PhonStartupHook, IPluginExtensionPoint<PhonStartupHook> {

	@Override
	public void startup() throws PluginException {
		final UserATDB userATDB = UserATDB.getInstance();
		LogUtil.info("[TranscriptMapper] Loading user aligned types database");
		PhonWorker.invokeOnNewWorker(() -> {
			try {
				userATDB.loadATDB();
			} catch (IOException e) {
				LogUtil.warning("[TranscriptMapper] " + e.getLocalizedMessage(), e);
			}
		}, () -> {
			LogUtil.info("[TranscriptMapper] Finished loading user aligned types database");
		});
	}

	@Override
	public Class<?> getExtensionType() {
		return PhonStartupHook.class;
	}

	@Override
	public IPluginExtensionFactory<PhonStartupHook> getFactory() {
		return (args -> this);
	}

}
