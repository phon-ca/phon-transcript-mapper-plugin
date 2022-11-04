/*
 * Copyright (C) 2005-2022 Gregory Hedlund & Yvan Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.phon.transcriptMapper;

import ca.phon.orthography.*;
import ca.phon.util.Tuple;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

import java.util.*;

public class OrthographyWordReplacementVisitor extends VisitorAdapter<OrthoElement> {

	private final int wordIndex;

	private final String word;

	private int currentWordIndex = -1;

	private OrthographyBuilder builder;

	public OrthographyWordReplacementVisitor(int wordIndex, String word, int currentWordIndex) {
		super();
		this.wordIndex = wordIndex;
		this.word = word;
		this.currentWordIndex = currentWordIndex;
		this.builder = new OrthographyBuilder();
	}

//	private List<Tuple<Character, OrthoWord>> flattenWordnet(OrthoWordnet wordnet) {
//		List<Tuple<Character, OrthoWord>> retVal = new ArrayList<>();
//		if(wordnet.getWord1() instanceof OrthoWordnet) {
//			retVal.addAll(flattenWordnet((OrthoWordnet) wordnet.getWord1()));
//		} else {
//			retVal.add(new Tuple<>('\u0000', wordnet.getWord1()));
//		}
//		retVal.add(new Tuple<>(wordnet.getMarker().getMarker(), wordnet.getWord2()));
//		return retVal;
//	}

	@Override
	public void fallbackVisit(OrthoElement orthoElement) {
		if(currentWordIndex == wordIndex) {
			builder.append(word);
		}
		builder.append(orthoElement);
	}

//	@Visits
//	public void visitWordNet(OrthoWordnet wordnet) {
//		var morphemeList = flattenWordnet(wordnet);
//		if(wordIndex >= currentWordIndex && wordIndex < currentWordIndex + morphemeList.size()) {
//			// replace specific morpheme, recreate wordnet
//			OrthoWord lastWord = (currentWordIndex++ == wordIndex ? new OrthoWord(word) : morphemeList.get(0).getObj2());
//			for(int i = 1; i < morphemeList.size(); i++) {
//				OrthoWord currentWord =
//						(currentWordIndex++ == wordIndex ? new OrthoWord(word) : morphemeList.get(i).getObj2());
//				Character marker = morphemeList.get(i).getObj1();
//				OrthoWordnet wn = new OrthoWordnet(lastWord, currentWord, OrthoWordnetMarker.fromMarker(marker));
//				lastWord = wn;
//			}
//			builder.append(lastWord);
//		} else {
//			currentWordIndex += morphemeList.size();
//			builder.append(wordnet);
//		}
//	}

	@Visits
	public void orthoWord(OrthoWord word) {
		if(word.getPrefix() != null && word.getPrefix().getType() == WordPrefixType.OMISSION) {
			// append word without incrementing morpheme index
			builder.append(word);
		} else {
			if (currentWordIndex++ == wordIndex) {
				// replace morpheme
				builder.append(this.word);
			} else {
				builder.append(word);
			}
		}
	}

	public Orthography getOrthography() {
		return builder.toOrthography();
	}

}
