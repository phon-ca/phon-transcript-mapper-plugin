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

import au.com.bytecode.opencsv.CSVWriter;
import ca.phon.alignedTypesDatabase.*;
import ca.phon.worker.PhonTask;

import java.io.*;
import java.util.*;

public class ExportCSVTask extends PhonTask {

	private final AlignedTypesDatabase db;

	private final File csvFile;

	private final String keyTier;

	private final String[] tierNames;

	public ExportCSVTask(AlignedTypesDatabase db, File csvFile, String keyTier, String[] tierNames) {
		super();

		this.db = db;
		this.csvFile = csvFile;
		this.keyTier = keyTier;
		this.tierNames = tierNames;
	}

	private CSVWriter createWriter() throws IOException {
		return new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"));
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);

		try {
			final CSVWriter writer = createWriter();

			List<String> tiers = new ArrayList<>();
			tiers.addAll(List.of(tierNames));
			tiers.remove(keyTier);
			tiers.add(0, keyTier);

			// write header
			writer.writeNext(tiers.toArray(new String[0]));
			writer.flush();

			final Collection<String> typesForTier = db.typesForTier(keyTier);
			for(String type: typesForTier) {
				if(tiers.size() == 1) {

				} else {
					Map<String, String[]> alignedTypes = db.alignedTypesForTier(keyTier, type, tiers);

					String[][] typeOpts = new String[tiers.size()][];
					typeOpts[0] = new String[]{type};
					for (int i = 1; i < tiers.size(); i++) {
						String tierName = tiers.get(i);
						String[] tierOpts = alignedTypes.get(tierName);
						if (tierOpts == null)
							tierOpts = new String[0];
						typeOpts[i] = tierOpts;
					}

					String[][] filteredCartesianProduct =
							CartesianProduct.stringArrayProduct(typeOpts, (set) -> db.hasAlignedTypes(tiers.toArray(new String[0]), set));
					for (String[] row : filteredCartesianProduct) {
						writer.writeNext(row);
					}
				}
			}

			writer.flush();

			super.setStatus(TaskStatus.FINISHED);
		} catch (IOException e) {
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
		}
	}

}
