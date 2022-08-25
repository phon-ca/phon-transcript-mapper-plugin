package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.*;
import ca.phon.project.Project;
import ca.phon.util.alignedTypesDatabase.*;
import org.apache.commons.io.*;

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

	private AlignedTypesDatabase atdb = new AlignedTypesDatabase();

	ProjectATDB(Project project) {
		this.project = project;
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
