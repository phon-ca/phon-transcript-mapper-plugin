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
