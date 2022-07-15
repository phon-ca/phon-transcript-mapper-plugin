package ca.phon.tanscriptMapper;

import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;

@PhonPlugin(name = TranscriptMapperEditorView.NAME)
@EditorViewInfo(category = EditorViewCategory.PLUGINS, icon = TranscriptMapperEditorView.ICON,
		name = TranscriptMapperEditorView.NAME)
public class TranscriptMapperEditorViewExtPt implements IPluginExtensionPoint<EditorView> {
	@Override
	public Class<?> getExtensionType() {
		return EditorView.class;
	}

	@Override
	public IPluginExtensionFactory<EditorView> getFactory() {
		return (args) -> {
			if(args.length != 1) throw new IllegalArgumentException("Invalid number of args");
			if(args[0] instanceof SessionEditor) {
				return new TranscriptMapperEditorView((SessionEditor) args[0]);
			} else {
				throw new IllegalArgumentException("First argument should be the editor");
			}
		};
	}
}
