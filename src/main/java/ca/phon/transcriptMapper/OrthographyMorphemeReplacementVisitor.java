package ca.phon.transcriptMapper;

import ca.phon.orthography.*;
import ca.phon.util.Tuple;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;
import org.apache.tools.ant.taskdefs.condition.Or;

import java.util.*;

public class OrthographyMorphemeReplacementVisitor extends VisitorAdapter<OrthoElement> {

	private final int morphemeIdx;

	private final String morpheme;

	private int currentMorphemeIdx = -1;

	private OrthographyBuilder builder;

	public OrthographyMorphemeReplacementVisitor(int morphemeIdx, String morpheme, int currentMorphemeIdx) {
		super();
		this.morphemeIdx = morphemeIdx;
		this.morpheme = morpheme;
		this.currentMorphemeIdx = currentMorphemeIdx;
		this.builder = new OrthographyBuilder();
	}

	private List<Tuple<Character, OrthoWord>> flattenWordnet(OrthoWordnet wordnet) {
		List<Tuple<Character, OrthoWord>> retVal = new ArrayList<>();
		if(wordnet.getWord1() instanceof OrthoWordnet) {
			retVal.addAll(flattenWordnet((OrthoWordnet) wordnet.getWord1()));
		} else {
			retVal.add(new Tuple<>('\u0000', wordnet.getWord1()));
		}
		retVal.add(new Tuple<>(wordnet.getMarker().getMarker(), wordnet.getWord2()));
		return retVal;
	}

	@Override
	public void fallbackVisit(OrthoElement orthoElement) {
		if(currentMorphemeIdx == morphemeIdx) {
			builder.append(morpheme);
		}
		builder.append(orthoElement);
	}

	@Visits
	public void visitWordNet(OrthoWordnet wordnet) {
		var morphemeList = flattenWordnet(wordnet);
		if(morphemeIdx > currentMorphemeIdx && morphemeIdx <= currentMorphemeIdx + morphemeList.size()) {
			// replace specific morpheme, recreate wordnet
			OrthoWord lastWord = (currentMorphemeIdx++ == morphemeIdx ? new OrthoWord(morpheme) : morphemeList.get(0).getObj2());
			for(int i = 1; i < morphemeList.size(); i++) {
				OrthoWord currentWord =
						(currentMorphemeIdx++ == morphemeIdx ? new OrthoWord(morpheme) : morphemeList.get(i).getObj2());
				Character marker = morphemeList.get(i).getObj1();
				OrthoWordnet wn = new OrthoWordnet(lastWord, currentWord, OrthoWordnetMarker.fromMarker(marker));
				lastWord = wn;
			}
			builder.append(lastWord);
		} else {
			currentMorphemeIdx += morphemeList.size();
			builder.append(wordnet);
		}
	}

	@Visits
	public void orthoWord(OrthoWord word) {
		if(currentMorphemeIdx++ == morphemeIdx) {
			// replace morpheme
			builder.append(morpheme);
		} else {
			builder.append(word);
		}
	}

	public Orthography getOrthography() {
		return builder.toOrthography();
	}

}
