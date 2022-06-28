package ca.phon.alignedMorpheme.ui;

import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;

@PhonPlugin(name = AlignedMorphemeSessionEditorView.NAME)
@EditorViewInfo(category = EditorViewCategory.PLUGINS, icon = AlignedMorphemeSessionEditorView.ICON,
		name = AlignedMorphemeSessionEditorView.NAME)
public class AlignedMorphemeEditorViewExtPt implements IPluginExtensionPoint<EditorView> {
	@Override
	public Class<?> getExtensionType() {
		return EditorView.class;
	}

	@Override
	public IPluginExtensionFactory<EditorView> getFactory() {
		return (args) -> {
			if(args.length != 1) throw new IllegalArgumentException("Invalid number of args");
			if(args[0] instanceof SessionEditor) {
				return new AlignedMorphemeSessionEditorView((SessionEditor) args[0]);
			} else {
				throw new IllegalArgumentException("First argument should be the editor");
			}
		};
	}
}
