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

import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;

@PhonPlugin(name = TranscriptMapperEditorView.NAME)
@EditorViewInfo(category = EditorViewCategory.PLUGINS, icon = TranscriptMapperEditorView.ICON,
		name = TranscriptMapperEditorView.NAME)
public final class TranscriptMapperEditorViewExtPt implements IPluginExtensionPoint<EditorView> {
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
