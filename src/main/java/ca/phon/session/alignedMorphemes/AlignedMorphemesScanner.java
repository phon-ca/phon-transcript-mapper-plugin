package ca.phon.session.alignedMorphemes;

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.OrthoElement;
import ca.phon.session.*;
import ca.phon.session.Record;

import java.util.*;

public class AlignedMorphemesScanner {

	private final AlignedTypesDatabase db;

	public AlignedMorphemesScanner(AlignedTypesDatabase db) {
		super();
		this.db = db;
	}

	public AlignedTypesDatabase getMorphemeTaggerDatabase() {
		return this.db;
	}

	public void scanSession(Session session) {
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
						}
						this.db.addAlignedTypes(alignedTypeMap);
					}
				}
			}
		}
	}

}
