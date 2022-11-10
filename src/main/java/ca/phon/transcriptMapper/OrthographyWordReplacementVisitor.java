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

	private int currentEleIndex = 0;

	private int lastWordIndex = 0;

	private OrthographyBuilder builder;

	public OrthographyWordReplacementVisitor(int wordIndex, String word, int currentWordIndex) {
		super();
		this.wordIndex = wordIndex;
		this.word = word;
		this.currentWordIndex = currentWordIndex;
		this.builder = new OrthographyBuilder();
	}

	@Override
	public void fallbackVisit(OrthoElement orthoElement) {
		if(currentWordIndex == wordIndex) {
			builder.append(word);
		}
		builder.append(orthoElement);
		++currentEleIndex;
	}

	public void appendTail() {
		if(currentWordIndex < wordIndex) {
			final Orthography currentOrtho = builder.toOrthography();
			builder.clear();
			for(int i = 0; i <= lastWordIndex; i++) {
				builder.append(currentOrtho.elementAt(i));
			}
			while (currentWordIndex++ < wordIndex) {
				builder.append("xxx");
			}
			builder.append(word);
			for(int i = lastWordIndex+1; i < currentOrtho.length(); i++) {
				builder.append(currentOrtho.elementAt(i));
			}
		}
	}

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
		lastWordIndex = currentEleIndex++;
	}

	public Orthography getOrthography() {
		return builder.toOrthography();
	}

}
