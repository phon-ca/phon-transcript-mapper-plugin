package ca.phon.session.alignedType;

import ca.phon.orthography.*;
import ca.phon.util.Tuple;
import ca.phon.visitor.*;
import ca.phon.visitor.annotation.Visits;

import java.util.*;
import java.util.stream.Collectors;

public class OrthographyTypeVisitor extends VisitorAdapter<OrthoElement> {

	private int wrdIdx = 0;

	private List<Tuple<Integer, OrthoElement>> morphemeList = new ArrayList<>();

	public OrthoElement[] getMorphemes() {
		return morphemeList.stream()
				.map(t -> t.getObj2())
				.collect(Collectors.toList())
				.toArray(new OrthoElement[0]);
	}

	public Integer[] getMorphemeIndexes() {
		return morphemeList.stream()
				.map(t -> t.getObj1().intValue())
				.collect(Collectors.toList())
				.toArray(new Integer[0]);
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
		morphemeList.add(new Tuple<>(wrdIdx, word));
		wrdIdx += word.toString().length() + 1;
	}

}
