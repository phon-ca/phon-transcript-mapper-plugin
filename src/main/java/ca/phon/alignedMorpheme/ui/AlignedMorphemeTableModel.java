package ca.phon.alignedMorpheme.ui;

import ca.phon.alignedType.*;
import ca.phon.session.SystemTierType;

import javax.swing.table.AbstractTableModel;

public class AlignedMorphemeTableModel extends AbstractTableModel {

	private String[] tiers;

	private AlignedTypes alignedTypes;

	public AlignedMorphemeTableModel(String[] tiers) {
		super();

		this.tiers = tiers;
	}

	public String[] getTiers() {
		return this.tiers;
	}

	public void setTiers(String[] tiers) {
		this.tiers = tiers;
		super.fireTableStructureChanged();
	}

	@Override
	public int getRowCount() {
		return alignedTypes.getMorphemeCount();
	}

	@Override
	public int getColumnCount() {
		return this.tiers.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		String tierName = this.tiers[columnIndex];
		SystemTierType systemTier = SystemTierType.tierFromString(tierName);

		AlignedType morpheme = alignedTypes.getAlignedMorpheme(rowIndex);
		if(systemTier != null) {
			switch (systemTier) {
				case Orthography -> {
					return (morpheme.getOrthography() != null ? morpheme.getOrthography() : "");
				}

				case IPATarget -> {
					return (morpheme.getIPATarget() != null ? morpheme.getIPATarget() : "");
				}

				case IPAActual -> {
					return (morpheme.getIPAActual() != null ? morpheme.getIPAActual() : "");
				}

				default -> {
					return "";
				}
			}
		} else {
			return (morpheme.getUserTier(tierName) != null ? morpheme.getUserTier(tierName) : "");
		}
	}

}
