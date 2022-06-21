package ca.phon.alignedMorpheme.ui;

import ca.phon.app.session.editor.*;
import ca.phon.util.icons.*;

import javax.swing.*;

/**
 * Aligned morpheme editor view for Phon sessions. This view will display the aligned morpheme lookup data
 * for the current record and provide method for inserting aligned morpheme data and updating the aligned
 * morpheme database(s).
 *
 */
public class AlignedMorphemeSessionEditorView extends EditorView {

	public final static String NAME = "Aligned Word/Morpheme";

	private final static String ICON = "blank";

	public AlignedMorphemeSessionEditorView(SessionEditor editor) {
		super(editor);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ImageIcon getIcon() {
		return IconManager.getInstance().getIcon(ICON, IconSize.SMALL);
	}

	@Override
	public JMenu getMenu() {
		return new JMenu();
	}

}
