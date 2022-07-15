package ca.phon.alignedTypesDatabase;

import com.jcraft.jsch.IO;
import org.apache.commons.io.Charsets;
import org.apache.tools.ant.taskdefs.Zip;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.*;

/**
 * Serialization methods {@link AlignedTypesDatabase} including compression.
 *
 */
public class AlignedTypesDatabaseIO {

	/** Default extension for uncompressed database files */
	public final static String DB_EXT = ".atdb";

	/** Default extension for compressed database files */
	public final static String DBZ_EXT = ".atdz";

	private final static String INVALID_NAME_MSG = "Invalid file name, extension must be " + DB_EXT + " or " + DBZ_EXT;

	public static AlignedTypesDatabase readFromFile(String filename) throws IOException {
		return readFromFile(new File(filename));
	}

	public static AlignedTypesDatabase readFromFile(String filename, boolean compressed) throws IOException {
		return readFromFile(new File(filename), compressed);
	}

	public static AlignedTypesDatabase readFromFile(File dbFile) throws IOException {
		return readFromFile(dbFile, dbFile.getName().endsWith(DBZ_EXT));
	}

	public static AlignedTypesDatabase readFromFile(File dbFile, boolean compressed) throws IOException {
		checkExtension(dbFile, compressed);
		if(compressed) {
			final String basename = dbFile.getName().substring(0, dbFile.getName().length()-DBZ_EXT.length());
			final ZipFile zipFile = new ZipFile(dbFile);
			final ZipEntry zipEntry = zipFile.getEntry(basename + DB_EXT);
			if(zipEntry == null)
				throw new IOException("No database entry found in file " + dbFile.getAbsolutePath());
			try(final ObjectInputStream oin = new ObjectInputStream(zipFile.getInputStream(zipEntry))) {
				return (AlignedTypesDatabase) oin.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			} finally {
				zipFile.close();
			}
		} else {
			try(final ObjectInputStream oin = new ObjectInputStream(new FileInputStream(dbFile))) {
				return (AlignedTypesDatabase) oin.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			}
		}
	}

	public static void writeToFile(AlignedTypesDatabase db, String filename) throws IOException {
		writeToFile(db, new File(filename));
	}

	public static void writeToFile(AlignedTypesDatabase db, String filename, boolean compressed) throws IOException {
		writeToFile(db, new File(filename), compressed);
	}

	public static void writeToFile(AlignedTypesDatabase db, File dbFile) throws IOException {
		writeToFile(db, dbFile, dbFile.getName().endsWith(DBZ_EXT));
	}

	public static void writeToFile(AlignedTypesDatabase db, File dbFile, boolean compressed) throws IOException {
		checkExtension(dbFile, compressed);
		if(compressed) {
			final String basename = dbFile.getName().substring(0, dbFile.getName().length()-DBZ_EXT.length());
			try(final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(dbFile))) {
				ZipEntry zipEntry = new ZipEntry(basename + DB_EXT);
				zout.putNextEntry(zipEntry);

				try(final ObjectOutputStream out = new ObjectOutputStream(zout)) {
					out.writeObject(db);
					out.flush();
				}
				zout.closeEntry();
			}
		} else {
			try(final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dbFile))) {
				out.writeObject(db);
				out.flush();
			}
		}
	}

	private static void checkExtension(File file, boolean compressed) throws IOException {
		if(compressed) {
			if(!file.getName().endsWith(DBZ_EXT))
				throw new IOException(INVALID_NAME_MSG);
		} else {
			if(!file.getName().endsWith(DB_EXT))
				throw new IOException(INVALID_NAME_MSG);
		}
	}

}
