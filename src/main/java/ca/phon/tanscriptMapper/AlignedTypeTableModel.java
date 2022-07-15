package ca.phon.tanscriptMapper;

import ca.phon.session.SystemTierType;
import ca.phon.session.alignedMorphemes.*;

import javax.swing.table.AbstractTableModel;

public class AlignedTypeTableModel extends AbstractTableModel {

	private String[] tiers;

	private AlignedMorphemes alignedMorphemes;

	public AlignedTypeTableModel(String[] tiers) {
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
		return alignedMorphemes.getMorphemeCount();
	}

	@Override
	public int getColumnCount() {
		return this.tiers.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		String tierName = this.tiers[columnIndex];
		SystemTierType systemTier = SystemTierType.tierFromString(tierName);

		AlignedMorpheme alignedMorpheme = alignedMorphemes.getAlignedMorpheme(rowIndex);
		if(systemTier != null) {
			switch (systemTier) {
				case Orthography -> {
					return (alignedMorpheme.getOrthography() != null ? alignedMorpheme.getOrthography() : "");
				}

				case IPATarget -> {
					return (alignedMorpheme.getIPATarget() != null ? alignedMorpheme.getIPATarget() : "");
				}

				case IPAActual -> {
					return (alignedMorpheme.getIPAActual() != null ? alignedMorpheme.getIPAActual() : "");
				}

				default -> {
					return "";
				}
			}
		} else {
			return (alignedMorpheme.getUserTier(tierName) != null ? alignedMorpheme.getUserTier(tierName) : "");
		}
	}

}
