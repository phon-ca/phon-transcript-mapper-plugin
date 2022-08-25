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

import au.com.bytecode.opencsv.CSVReader;
import ca.phon.util.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.worker.PhonTask;

import java.io.*;
import java.util.*;

public class ImportCSVTask extends PhonTask {

	private final AlignedTypesDatabase db;

	private final File csvFile;

	public ImportCSVTask(AlignedTypesDatabase db, File csvFile) {
		super();

		this.db = db;
		this.csvFile = csvFile;
	}

	private CSVReader createReader() throws IOException {
		return new CSVReader(new InputStreamReader(new FileInputStream(csvFile), "UTF-8"));
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);

		try {
			final CSVReader csvReader = createReader();

			final String[] cols = csvReader.readNext();
			for (String tierName : cols) {
				try {
					db.addUserTier(tierName);
				} catch (AlignedTypesDatabase.DuplicateTierEntry e) {
					// tier already exists
				}
			}

			String[] row = null;
			while ((row = csvReader.readNext()) != null) {
				Map<String, String> alignedTypes = new HashMap<>();
				for (int i = 0; i < cols.length; i++) {
					String tierName = cols[i];
					String type = (i < row.length ? row[i] : "");
					alignedTypes.put(tierName, type);
				}
				db.addAlignedTypes(alignedTypes);
			}

			super.setStatus(TaskStatus.FINISHED);
		} catch (IOException e) {
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
		}
	}
}