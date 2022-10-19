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

import ca.phon.alignedTypesDatabase.*;
import ca.phon.app.log.LogUtil;
import ca.phon.session.SystemTierType;
import ca.phon.util.PrefHelper;
import org.apache.commons.io.FileUtils;

import java.beans.*;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public final class UserATDB {

	private final static String USER_DB_FILENAME = "transcriptMapper/typeMap" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private final static String BACKUP_DB_FILENAME = "transcriptMapper/typeMap-backup" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private final ReentrantLock loadLock = new ReentrantLock();

	private AlignedTypesDatabase atdb = null;

	private volatile boolean modified = false;

	private volatile boolean saving = false;

	private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

	private static UserATDB _instance;

	public static UserATDB getInstance() {
		if(_instance == null) {
			_instance = new UserATDB();
		}
		return _instance;
	}

	private UserATDB() {
	}

	public boolean isATDBLoaded() {
		return this.atdb != null;
	}

	public boolean isModified() {
		return this.modified;
	}

	public boolean isSaving() { return this.saving; }

	/**
	 * Return the user's aligned types database.
	 *
	 * @return ATDB for project, null if project has not been loaded
	 */
	public AlignedTypesDatabase getATDB() {
		if(!isATDBLoaded()) {
			try {
				loadATDB();
			} catch (IOException e) {
				LogUtil.severe(e);
			}
		}
		return atdb;
	}

	private File getDbFile() {
		final File dbFile = new File(PrefHelper.getUserDataFolder(), USER_DB_FILENAME);
		return dbFile;
	}

	private File backupDbFile() {
		final File dbFile = new File(PrefHelper.getUserDataFolder(), BACKUP_DB_FILENAME);
		return dbFile;
	}

	/**
	 * Load the project {@link AlignedTypesDatabase}
	 */
	public void loadATDB() throws IOException {
		if(isATDBLoaded()) return;
		loadLock.lock();
		final File projectDbFile = getDbFile();
		if(projectDbFile.exists()) {
			this.atdb = AlignedTypesDatabaseIO.readFromFile(projectDbFile);
		} else {
			this.atdb = (new AlignedTypesDatabaseFactory()).createDatabase();
			setupTierDescriptionTree(this.atdb);
		}
		this.atdb.addDatabaseListener(listener);
		loadLock.unlock();
		propSupport.firePropertyChange("loaded", false, true);
	}

	private void setupTierDescriptionTree(AlignedTypesDatabase db) {
		db.addUserTier(SystemTierType.Orthography.getName());
		db.addUserTier(SystemTierType.IPATarget.getName());
		db.addUserTier(SystemTierType.IPAActual.getName());
	}

	public void backupDb() throws IOException {
		FileUtils.copyFile(getDbFile(), backupDbFile());
	}

	public void saveDb() throws  IOException {
		this.saving = true;
		final File dbFile = getDbFile();
		final File parentFolder = dbFile.getParentFile();
		if(!parentFolder.exists()) {
			parentFolder.mkdirs();
		}
		if(dbFile.exists())
			backupDb();
		AlignedTypesDatabaseIO.writeToFile(this.atdb, dbFile);

		boolean oldVal = this.modified;
		this.modified = false;
		this.saving = false;
		propSupport.firePropertyChange("modified", oldVal, this.modified);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return propSupport.getPropertyChangeListeners();
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(propertyName, listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		return propSupport.getPropertyChangeListeners(propertyName);
	}

	public boolean hasListeners(String propertyName) {
		return propSupport.hasListeners(propertyName);
	}

	private final AlignedTypesDatabaseListener listener = (evt) -> {
		boolean oldVal = this.modified;
		this.modified = true;
		propSupport.firePropertyChange("modified", oldVal, this.modified);
	};

}
