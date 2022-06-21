package ca.phon.alignedMorpheme.db;

import java.io.Serializable;

class TierInfo implements Serializable {

	private final static long serialVersionUID = 1L;

	final String tierName;

	final String tierFont;

	public TierInfo(String tierName) {
		this(tierName, "default");
	}

	public TierInfo(String tierName, String tierFont) {
		this.tierName = tierName;
		this.tierFont = tierFont;
	}

}
