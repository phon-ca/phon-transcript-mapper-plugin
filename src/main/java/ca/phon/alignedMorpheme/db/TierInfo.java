package ca.phon.alignedMorpheme.db;

import java.io.Serializable;

public class TierInfo implements Serializable {

	private static final long serialVersionUID = -5835662467026647994L;

	private final String tierName;

	private String tierFont;

	private boolean visible = true;

	private int order = -1;

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

	public void setTierFont(String tierFont) {
		this.tierFont = tierFont;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
