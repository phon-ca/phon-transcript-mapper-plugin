package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
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

		final PhonTask importTask = new PhonTask() {
			@Override
			public void performTask() {
				super.setStatus(TaskStatus.RUNNING);

				final AlignedTypesDatabase db = getView().getProjectDb();
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
								alignedTypes.put("Language", dictLang.toString());

								db.addAlignedTypes(alignedTypes);
							}
						}
					}
				}

				super.setStatus(TaskStatus.FINISHED);
			}
		};
		importTask.setName("Import IPA Dictionary '" + dictLang.toString() + "'");
		getView().getEditor().getStatusBar().watchTask(importTask);

		PhonWorker.invokeOnNewWorker(importTask, getView()::updateAfterDbChange);
	}

}
