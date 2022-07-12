package ca.phon.plugin.typeMap;

import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;

@PhonPlugin(name = TypeMapEditorView.NAME)
@EditorViewInfo(category = EditorViewCategory.PLUGINS, icon = TypeMapEditorView.ICON,
		name = TypeMapEditorView.NAME)
public class TypeMapEditorViewExtPt implements IPluginExtensionPoint<EditorView> {
	@Override
	public Class<?> getExtensionType() {
		return EditorView.class;
	}

	@Override
	public IPluginExtensionFactory<EditorView> getFactory() {
		return (args) -> {
			if(args.length != 1) throw new IllegalArgumentException("Invalid number of args");
			if(args[0] instanceof SessionEditor) {
				return new TypeMapEditorView((SessionEditor) args[0]);
			} else {
				throw new IllegalArgumentException("First argument should be the editor");
			}
		};
	}
}
