package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.util.PrefHelper;
import org.apache.commons.io.FileUtils;

import java.io.*;

public class UserATDB {

	private final static String USER_DB_FILENAME = "transcriptMapper/typeMap" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private final static String BACKUP_DB_FILENAME = "transcriptMapper/typeMap-backup" +
			AlignedTypesDatabaseIO.DBZ_EXT;

	private AlignedTypesDatabase atdb = new AlignedTypesDatabase();

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

	/**
	 * Return the user's aligned types database.
	 *
	 * @return ATDB for project, null if project has not been loaded
	 */
	public synchronized AlignedTypesDatabase getATDB() {
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
	public synchronized void loadATDB() throws IOException {
		final File projectDbFile = getDbFile();
		if(projectDbFile.exists()) {
			this.atdb = AlignedTypesDatabaseIO.readFromFile(projectDbFile);
		}
	}

	public synchronized void backupDb() throws IOException {
		FileUtils.copyFile(getDbFile(), backupDbFile());
	}

	public synchronized void saveDb() throws  IOException {
		final File dbFile = getDbFile();
		final File parentFolder = dbFile.getParentFile();
		if(!parentFolder.exists()) {
			parentFolder.mkdirs();
		}
		if(dbFile.exists())
			backupDb();
		AlignedTypesDatabaseIO.writeToFile(this.atdb, dbFile);
	}

}