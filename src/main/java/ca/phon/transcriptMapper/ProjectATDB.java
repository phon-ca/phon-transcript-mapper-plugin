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

import ca.phon.project.Project;
import ca.phon.alignedTypesDatabase.*;
import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * Project extension providing a single {@link AlignedTypesDatabase} for
 * a loaded project.
 */
public final class ProjectATDB {

	private final static String PROJECT_DB_FILENAME = "__res/transcriptMapper/typeMap" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private final static String BACKUP_DB_FILENAME = "__res/transcriptMapper/typeMap-backup" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private Project project;

	private AlignedTypesDatabase atdb;

	ProjectATDB(Project project) {
		this.project = project;

		this.atdb = (new AlignedTypesDatabaseFactory()).createDatabase();
	}

	public boolean isATDBLoaded() {
		return this.atdb != null;
	}

	/**
	 * Return the project aligned types database.
	 *
	 * @return ATDB for project, null if project has not been loaded
	 */
	public synchronized AlignedTypesDatabase getATDB() {
		return atdb;
	}

	private File projectDbFile() {
		final File dbFile = new File(project.getLocation(), PROJECT_DB_FILENAME);
		return dbFile;
	}

	private File projectBackupFile() {
		final File dbFile = new File(project.getLocation(), BACKUP_DB_FILENAME);
		return dbFile;
	}

	/**
	 * Load the project {@link AlignedTypesDatabase}
	 */
	public synchronized void loadATDB() throws IOException {
		final File projectDbFile = projectDbFile();
		if(projectDbFile.exists()) {
			this.atdb = AlignedTypesDatabaseIO.readFromFile(projectDbFile);
		}
	}

	public synchronized void backupProjectDb() throws IOException {
		FileUtils.copyFile(projectDbFile(), projectBackupFile());
	}

	public synchronized void saveProjectDb() throws  IOException {
		final File projectDbFile = projectDbFile();
		final File parentFolder = projectDbFile.getParentFile();
		if(!parentFolder.exists()) {
			parentFolder.mkdirs();
		}
		AlignedTypesDatabaseIO.writeToFile(this.atdb, projectDbFile);
	}

}
