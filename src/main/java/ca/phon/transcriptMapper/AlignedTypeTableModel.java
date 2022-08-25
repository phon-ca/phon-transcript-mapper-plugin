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
