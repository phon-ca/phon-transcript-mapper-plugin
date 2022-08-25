package ca.phon.transcriptMapper;

import ca.phon.util.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.ipadictionary.*;
import ca.phon.ipadictionary.spi.OrthoKeyIterator;
import ca.phon.session.SystemTierType;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.Language;
import ca.phon.worker.*;

import java.awt.event.ActionEvent;
import java.util.*;

public class ImportIPADictionaryAction extends TranscriptMapperAction {

	private final Language dictLang;

	public ImportIPADictionaryAction(TranscriptMapperEditorView view, Language dictLang) {
		super(view);

		this.dictLang = dictLang;

		putValue(NAME, dictLang.toString());
		putValue(PhonUIAction.SHORT_DESCRIPTION, "Import IPA dictionary '" + dictLang.toString() + "'");
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final ImportIPADictionaryTask importTask = new ImportIPADictionaryTask(getView().getUserDb(), dictLang);
		importTask.setName("Import IPA Dictionary '" + dictLang.toString() + "'");
		getView().getEditor().getStatusBar().watchTask(importTask);

		PhonWorker.invokeOnNewWorker(importTask, getView()::updateAfterDbChange);
	}

}
