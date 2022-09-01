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

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.OrthoElement;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.alignedMorphemes.*;
import ca.phon.util.LanguageEntry;

import java.util.*;

public class AlignedMorphemesScanner {

	private final AlignedTypesDatabase db;

	private final LanguageEntry lang;

	public AlignedMorphemesScanner(AlignedTypesDatabase db, LanguageEntry lang) {
		super();
		this.db = db;
		this.lang = lang;
	}

	public AlignedTypesDatabase getMorphemeTaggerDatabase() {
		return this.db;
	}

	/**
	 * Check to make sure that at least two tiers have been filled in
	 *
	 * @param alignedTypes
	 * @return true if type map alignment is 'empty'
	 */
	private boolean checkForEmptyAlignment(Map<String, String> alignedTypes) {
		int filledTiers = 0;
		for(String tierName:alignedTypes.keySet()) {
			String val = alignedTypes.get(tierName);
			if(val != null && val.trim().length() > 0 && !"*".equals(val)) {
				++filledTiers;
			}
		}
		return (filledTiers < 2);
	}

	public void scanSession(UUID projectId, Session session) {
		for(Record record:session.getRecords()) {
			for(int i = 0; i < record.numberOfGroups(); i++) {
				Group g = record.getGroup(i);
				for(int j = 0; j < g.getAlignedWordCount(); j++) {
					Word w = g.getAlignedWord(j);
					AlignedMorphemes alignedMorphemes = w.getExtension(AlignedMorphemes.class);
					if(alignedMorphemes != null) {
						Map<String, String> alignedTypeMap = new LinkedHashMap<>();
						for(int k = 0; k < alignedMorphemes.getMorphemeCount(); k++) {
							AlignedMorpheme am = alignedMorphemes.getAlignedMorpheme(k);
							for(SystemTierType systemTier:SystemTierType.values()) {
								switch(systemTier) {
								case Orthography:
									final OrthoElement orthoElement = am.getOrthography();
									final String ortho = orthoElement == null ? "" : orthoElement.toString().trim();
									if(!"*".equals(ortho)) {
										alignedTypeMap.put(systemTier.getName(), ortho);
									}
									break;

								case IPATarget:
									final IPATranscript ipaT = am.getIPATarget();
									final String target = ipaT == null ? "" : ipaT.toString();
									if(!"*".equals(target)) {
										alignedTypeMap.put(systemTier.getName(), target);
									}
									break;

								case IPAActual:
									final IPATranscript ipaA = am.getIPAActual();
									final String actual = ipaA == null ? "" : am.getIPAActual().toString();
									if(!"*".equals(actual)) {
										alignedTypeMap.put(systemTier.getName(), actual);
									}
									break;

								default:
									break;
								}
							}

							for(TierDescription td: session.getUserTiers()) {
								if(td.isGrouped()) {
									TierString userTierVal = am.getUserTier(td.getName());
									if (userTierVal == null) userTierVal = new TierString();
									if (!"*".equals(userTierVal.toString())) {
										alignedTypeMap.put(td.getName(), userTierVal.toString());
									}
								}
							}

							if(!checkForEmptyAlignment(alignedTypeMap)) continue;

							if(this.lang != null) {
								alignedTypeMap.put("Language", this.lang.getId());
							}

							if(projectId != null) {
								alignedTypeMap.put(TypeMapMetadataTier.PROJECT_ID.getTierName(), projectId.toString());
							}
						}
						this.db.addAlignedTypes(alignedTypeMap);
					}
				}
			}
		}
	}

}
