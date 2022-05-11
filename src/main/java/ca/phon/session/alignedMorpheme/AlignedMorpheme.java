package ca.phon.session.alignedMorpheme;

import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.*;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.util.Range;

/**
 * Represents a set of aligned morphemes across Phon tiers.
 *
 */
public class AlignedMorpheme {

	private final Word alignedWord;

	private final int morphemeIdx;

	AlignedMorpheme(Word word, int morphemeIdx) {
		super();

		this.alignedWord = word;
		this.morphemeIdx = morphemeIdx;
	}

	public Word getAlignedWord() {
		return this.alignedWord;
	}

	public int getMorphemeIdx() {
		return morphemeIdx;
	}

	public OrthoElement getOrthography() {
		MorphemeParser parser = new MorphemeParser();
		OrthoElement[] morphemes = parser.parseOrthography(getAlignedWord().getOrthography());
		return (getMorphemeIdx() >= 0 && getMorphemeIdx() < morphemes.length
				? morphemes[getMorphemeIdx()]
				: null);
	}

	public IPATranscript getIPATarget() {
		MorphemeParser parser = new MorphemeParser();
		IPATranscript[] morphemes = parser.parseIPA(getAlignedWord().getIPATarget());
		return (getMorphemeIdx() >= 0 && getMorphemeIdx() < morphemes.length
				? morphemes[getMorphemeIdx()]
				: null);
	}

	public IPATranscript getIPAActual() {
		MorphemeParser parser = new MorphemeParser();
		IPATranscript[] morphemes = parser.parseIPA(getAlignedWord().getIPAActual());
		return (getMorphemeIdx() >= 0 && getMorphemeIdx() < morphemes.length
				? morphemes[getMorphemeIdx()]
				: null);
	}

	public TierString getNotes() {
		MorphemeParser parser = new MorphemeParser();
		TierString[] morphemes = parser.parseTier(getAlignedWord().getNotes());
		return (getMorphemeIdx() >= 0 && getMorphemeIdx() < morphemes.length
				? morphemes[getMorphemeIdx()]
				: null);
	}

	public TierString getUserTier(String tierName) {
		MorphemeParser parser = new MorphemeParser();
		TierString[] morphemes = parser.parseTier((TierString)getAlignedWord().getTier(tierName));
		return (getMorphemeIdx() >= 0 && getMorphemeIdx() < morphemes.length
				? morphemes[getMorphemeIdx()]
				: null);
	}

}
