package ca.phon.transcriptMapper;

import ca.phon.app.hooks.PhonShutdownHook;
import ca.phon.plugin.PluginException;

public class UserATDBShutdownHook implements PhonShutdownHook {

	@Override
	public void shutdown() throws PluginException {
		// TODO save changes if necessary
	}

}
