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

import ca.phon.ipadictionary.*;
import ca.phon.ipadictionary.spi.OrthoKeyIterator;
import ca.phon.session.SystemTierType;
import ca.phon.util.Language;
import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.worker.PhonTask;

import java.util.*;

public class ImportIPADictionaryTask extends PhonTask {

	private final AlignedTypesDatabase db;

	private final Language dictLang;

	public ImportIPADictionaryTask(AlignedTypesDatabase db, Language dictLang) {
		super();

		this.db = db;
		this.dictLang = dictLang;
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);

		// entries will be given in alphabetical order, we need to collect
		// and randomize them for efficiency
		final List<Map<String, String>> alignedTypesList = new ArrayList<>();

		final List<IPADictionary> dicts = IPADictionaryLibrary.getInstance().dictionariesForLanguage(dictLang);
		for(IPADictionary dict:dicts) {
			OrthoKeyIterator keyItr = dict.getExtension(OrthoKeyIterator.class);
			if(keyItr != null) {
				final Iterator<String> orthoItr = keyItr.iterator();
				while(orthoItr.hasNext()) {
					final String ortho = orthoItr.next();
					final String[] opts = dict.lookup(ortho);

					for(String opt:opts) {
						final Map<String, String> alignedTypes = new LinkedHashMap<>();
						alignedTypes.put(SystemTierType.Orthography.getName(), ortho);
						alignedTypes.put(SystemTierType.IPATarget.getName(), opt);
						alignedTypes.put(SystemTierType.IPAActual.getName(), opt);
						alignedTypes.put(TypeMapMetadataTier.LANGUAGE.getTierName(), dictLang.getPrimaryLanguage().getId());

						alignedTypesList.add(alignedTypes);
					}
				}
			}
		}

		// randomize entries
		Collections.shuffle(alignedTypesList);
		for(Map<String, String> alignedTypes:alignedTypesList) {
			db.addAlignedTypes(alignedTypes);
		}

		super.setStatus(TaskStatus.FINISHED);
	}

}
