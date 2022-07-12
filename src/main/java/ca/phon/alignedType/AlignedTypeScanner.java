package ca.phon.alignedType;

import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.OrthoElement;
import ca.phon.session.*;
import ca.phon.session.Record;

import java.util.*;

public class AlignedTypeScanner {

	private final AlignedTypeDatabase db;

	public AlignedTypeScanner(AlignedTypeDatabase db) {
		super();
		this.db = db;
	}

	public AlignedTypeDatabase getMorphemeTaggerDatabase() {
		return this.db;
	}

	public void scanSession(Session session) {
		for(Record record:session.getRecords()) {
			for(int i = 0; i < record.numberOfGroups(); i++) {
				Group g = record.getGroup(i);
				for(int j = 0; j < g.getAlignedWordCount(); j++) {
					Word w = g.getAlignedWord(j);
					AlignedTypes alignedTypes = w.getExtension(AlignedTypes.class);
					if(alignedTypes != null) {
						Map<String, String> alignedMorphemeMap = new LinkedHashMap<>();
						for(int k = 0; k < alignedTypes.getMorphemeCount(); k++) {
							AlignedType am = alignedTypes.getAlignedMorpheme(k);
							for(SystemTierType systemTier:SystemTierType.values()) {
								switch(systemTier) {
								case Orthography:
									final OrthoElement orthoElement = am.getOrthography();
									final String ortho = orthoElement == null ? "" : orthoElement.toString().trim();
									if(ortho.length() > 0 && !"*".equals(ortho)) {
										alignedMorphemeMap.put(systemTier.getName(), ortho);
									}
									break;

								case IPATarget:
									final IPATranscript ipaT = am.getIPATarget();
									final String target = ipaT == null ? "" : ipaT.toString();
									if(target.length() > 0 && !"*".equals(target)) {
										alignedMorphemeMap.put(systemTier.getName(), target);
									}
									break;

								case IPAActual:
									final IPATranscript ipaA = am.getIPAActual();
									final String actual = ipaA == null ? "" : am.getIPAActual().toString();
									if(actual.length() > 0 && !"*".equals(actual)) {
										alignedMorphemeMap.put(systemTier.getName(), actual);
									}
									break;

								default:
									break;
								}
							}

							for(String userTierName: record.getExtraTierNames()) {
								TierString userTierVal = am.getUserTier(userTierName);
								if(userTierVal != null && userTierVal.length() > 0 && !"*".equals(userTierVal.toString())) {
									alignedMorphemeMap.put(userTierName, userTierVal.toString());
								}
							}
						}
						this.db.addAlignedMorphemes(alignedMorphemeMap);
					}
				}
			}
		}
	}

}
