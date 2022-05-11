package ca.phon.session.alignedMorpheme;

import ca.phon.extensions.*;
import ca.phon.orthography.*;
import ca.phon.session.Word;

import java.lang.ref.WeakReference;

/**
 * Extends the aligned word class with the aligned moprheme concept.
 *
 */
@Extension(Word.class)
public class AlignedMorphemes implements ExtensionProvider {

	private WeakReference<Word> alignedWord;

	public AlignedMorphemes() {
		super();
	}

	/**
	 * @return number of morpheme positions in the aligned word
	 */
	public int getMorphemeCount() {
		int retVal = 0;
		final Word alignedWord = getAlignedWord();
		final MorphemeParser morphemeParser = new MorphemeParser();
		if(alignedWord != null) {
			OrthoElement[] orthoMorphemes = morphemeParser.parseOrthography(alignedWord.getOrthography());

		}
		return retVal;
	}

	public Word getAlignedWord() {
		return this.alignedWord.get();
	}

	public void setAlignedWord(Word alignedWord) {
		this.alignedWord = new WeakReference<>(alignedWord);
	}

	/**
	 * Return aligned morpheme at given index
	 *
	 * @param morphemeIdx
	 * @return aligned morpheme for index
	 *
	 * @throws NullPointerException
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public AlignedMorpheme getAlignedMorpheme(int morphemeIdx) {
		if(morphemeIdx < 0 || morphemeIdx >= getMorphemeCount())
			throw new ArrayIndexOutOfBoundsException("Invalid morpheme index " + morphemeIdx);
		if(getAlignedWord() == null)
			throw new NullPointerException("alignedWord");
		return new AlignedMorpheme(getAlignedWord(), morphemeIdx);
	}

	@Override
	public void installExtension(IExtendable obj) {
		setAlignedWord((Word)obj);
		((Word)obj).putExtension(AlignedMorphemes.class, this);
	}

}
