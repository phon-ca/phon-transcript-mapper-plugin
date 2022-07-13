package ca.phon.plugin.typeMap;

import ca.phon.session.SystemTierType;
import ca.phon.session.alignedType.*;

import javax.swing.table.AbstractTableModel;

public class AlignedTypeTableModel extends AbstractTableModel {

	private String[] tiers;

	private AlignedTypes alignedTypes;

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

		AlignedType alignedType = alignedTypes.getAlignedMorpheme(rowIndex);
		if(systemTier != null) {
			switch (systemTier) {
				case Orthography -> {
					return (alignedType.getOrthography() != null ? alignedType.getOrthography() : "");
				}

				case IPATarget -> {
					return (alignedType.getIPATarget() != null ? alignedType.getIPATarget() : "");
				}

				case IPAActual -> {
					return (alignedType.getIPAActual() != null ? alignedType.getIPAActual() : "");
				}

				default -> {
					return "";
				}
			}
		} else {
			return (alignedType.getUserTier(tierName) != null ? alignedType.getUserTier(tierName) : "");
		}
	}

}
