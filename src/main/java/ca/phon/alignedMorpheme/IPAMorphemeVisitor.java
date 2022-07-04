package ca.phon.alignedMorpheme;

import ca.phon.ipa.*;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

import java.util.*;

public class IPAMorphemeVisitor extends VisitorAdapter<IPAElement> {

	private List<IPATranscript> morphemeList = new ArrayList<>();

	private IPATranscriptBuilder builder = new IPATranscriptBuilder();

	public IPATranscript[] getMorphemes() {
		if(builder.size() > 0) {
			createMorpheme();
		}
		return morphemeList.toArray(new IPATranscript[0]);
	}

	@Override
	public void fallbackVisit(IPAElement obj) {
		this.builder.append(obj);
	}

	private void createMorpheme() {
		if(builder.toIPATranscript().length() > 0) {
			morphemeList.add(this.builder.toIPATranscript());
			this.builder = new IPATranscriptBuilder();
		}
	}

	@Visits
	public void visitCompoundWordMarker(CompoundWordMarker cwm) {
		createMorpheme();
	}

	@Visits
	public void visitIntonationGroup(IntonationGroup ig) {
		createMorpheme();
	}

	@Visits
	public void visitWordBoundary(WordBoundary wb) {
		createMorpheme();
	}

}
