package ca.phon.session.alignedMorpheme;

import ca.phon.orthography.*;
import ca.phon.visitor.*;
import ca.phon.visitor.annotation.Visits;

import java.util.*;

public class OrthoMorphemeVisitor extends VisitorAdapter<OrthoElement> {

	private List<OrthoElement> morphemeList = new ArrayList<>();

	public OrthoElement[] getMorphemes() {
		return morphemeList.toArray(new OrthoElement[0]);
	}

	@Override
	public void fallbackVisit(OrthoElement obj) {

	}

	@Visits
	public void visitWordnet(OrthoWordnet wordnet) {
		visit(wordnet.getWord1());
		visit(wordnet.getWord2());
	}

	@Visits
	public void visitOrthoWord(OrthoWord word) {
		morphemeList.add(word);
	}

}
