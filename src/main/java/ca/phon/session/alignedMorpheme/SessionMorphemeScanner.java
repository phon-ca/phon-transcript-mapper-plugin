package ca.phon.session.alignedMorpheme;

import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.OrthoElement;
import ca.phon.session.*;
import ca.phon.session.Record;

import java.util.*;

public class SessionMorphemeScanner {

	private final MorphemeTaggerDatabase db;

	public SessionMorphemeScanner(MorphemeTaggerDatabase db) {
		super();
		this.db = db;
	}

	public MorphemeTaggerDatabase getMorphemeTaggerDatabase() {
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
						Map<String, String> alignedMorphemeMap = new LinkedHashMap<>();
						for(int k = 0; k < alignedMorphemes.getMorphemeCount(); k++) {
							AlignedMorpheme am = alignedMorphemes.getAlignedMorpheme(k);
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
										alignedMorphemeMap.put(systemTier.name(), target);
									}
									break;

								case IPAActual:
									final IPATranscript ipaA = am.getIPAActual();
									final String actual = ipaA == null ? "" : am.getIPAActual().toString();
									if(actual.length() > 0 && !"*".equals(actual)) {
										alignedMorphemeMap.put(systemTier.name(), actual);
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
