package ca.phon.alignedMorpheme.db;

import java.io.Serializable;

public class TierInfo implements Serializable {

	private final static long serialVersionUID = 1L;

	private final String tierName;

	private final String tierFont;

	public TierInfo(String tierName) {
		this(tierName, "default");
	}

	public TierInfo(String tierName, String tierFont) {
		this.tierName = tierName;
		this.tierFont = tierFont;
	}

	public String getTierName() {
		return tierName;
	}

	public String getTierFont() {
		return tierFont;
	}
}
