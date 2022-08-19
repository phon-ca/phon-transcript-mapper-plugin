package ca.phon.transcriptMapper;

import ca.phon.app.hooks.PhonShutdownHook;
import ca.phon.app.log.LogUtil;
import ca.phon.plugin.*;

import java.io.IOException;

public class UserATDBShutdownHook implements PhonShutdownHook, IPluginExtensionPoint<PhonShutdownHook> {

	@Override
	public void shutdown() throws PluginException {
		final UserATDB userATDB = UserATDB.getInstance();
		if(userATDB.isATDBLoaded() && userATDB.isModified()) {
			LogUtil.info("[TranscriptMapper] Saving user aligned types database");
			try {
				userATDB.saveDb();
				LogUtil.info("[TranscriptMapper] Finished saving user aligned types database");
			} catch (IOException e) {
				LogUtil.warning(e);
				throw new PluginException(e);
			}
		}
	}

	@Override
	public Class<?> getExtensionType() {
		return PhonShutdownHook.class;
	}

	@Override
	public IPluginExtensionFactory<PhonShutdownHook> getFactory() {
		return (args -> this);
	}
}
