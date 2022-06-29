package ca.phon.alignedMorpheme.ui;

import ca.phon.alignedMorpheme.db.AlignedMorphemeDatabase;
import ca.phon.session.SystemTierType;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class AlignedMorphemeTableModel extends AbstractTableModel {

	private AlignedMorphemeDatabase db;

	private String keyTier;

	private String[] tiers;

	private String morpheme;

	private Map<String, String[]> alignedMorphemes;

	public AlignedMorphemeTableModel(AlignedMorphemeDatabase db) {
		this(db, db.tierNames().toArray(new String[0]), SystemTierType.Orthography.getName());
	}

	public AlignedMorphemeTableModel(AlignedMorphemeDatabase db, String[] tiers, String keyTier) {
		super();

		this.db = db;
		this.tiers = tiers;
		this.keyTier = keyTier;
		this.morpheme = "";
		this.alignedMorphemes = new HashMap<>();
	}

	public String getKeyTier() {
		return this.keyTier;
	}

	public void setKeyTier(String keyTier) {
		this.keyTier = keyTier;
		super.fireTableDataChanged();
	}

	public String[] getTiers() {
		return this.tiers;
	}

	public void setTiers(String[] tiers) {
		this.tiers = tiers;
		super.fireTableStructureChanged();
	}

	public String getMorpheme() {
		return this.morpheme;
	}

	public void setMorpheme(String morpheme) {
		this.morpheme = morpheme;
		this.alignedMorphemes = db.alignedMorphemesForTier(getKeyTier(), morpheme);
		super.fireTableDataChanged();
	}

	@Override
	public int getRowCount() {

		return 0;
	}

	@Override
	public int getColumnCount() {
		return this.tiers.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return null;
	}

}
