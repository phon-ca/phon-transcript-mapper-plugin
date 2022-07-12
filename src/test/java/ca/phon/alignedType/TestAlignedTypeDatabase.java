package ca.phon.alignedType;

import ca.phon.session.SystemTierType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.util.*;

@RunWith(JUnit4.class)
public class TestAlignedTypeDatabase {

	private final static String CSV_NAME = "orthoTypeMean.txt";

	private AlignedTypeDatabase loadDatabase() throws IOException {
		// add database from csv data
		final AlignedTypeDatabase db = new AlignedTypeDatabase();

		BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(CSV_NAME),"UTF-16"));
		String headerLine = reader.readLine();

		if(headerLine == null)
			throw new IOException("No data");
		final String[] tierNames = headerLine.split("\\p{Space}");
		for(String tierName:tierNames) {
			tierName = tierName.trim();
			SystemTierType systemTier = SystemTierType.tierFromString(tierName);
			if(systemTier == null) {
				try {
					db.addUserTier(tierName);
				} catch (AlignedTypeDatabase.DuplicateTierEntry e) {
					throw new IOException(e);
				}
			}
		}

		String line = null;
		while((line = reader.readLine()) != null) {
			String[] morphemes = line.split("\\p{Space}");
			Map<String, String> alignedMorphemes = new LinkedHashMap<>();
			for(int i = 0; i < morphemes.length; i++) {
				if(morphemes[i].trim().length() > 0)
					alignedMorphemes.put(tierNames[i].trim(), morphemes[i].trim());
			}
			db.addAlignedMorphemes(alignedMorphemes);
		}

		return db;
	}

	@Test
	public void testDatabaseSerialization() throws IOException, ClassNotFoundException {
		AlignedTypeDatabase db = loadDatabase();

		Assert.assertTrue(db.getTierInfo().stream().filter((info) -> "MorphemeType".equals(info.getTierName())).findAny().isPresent());
		Assert.assertTrue(db.getTierInfo().stream().filter((info) -> "MorphemeMeaning".equals(info.getTierName())).findAny().isPresent());

		final String[] expectedTypes = {"p,quest","vai.fin","vii.fin","medial","subjunctive","vai+o.fin","spatial","thm(vta)","thm(vti.non3)","2.sg","2.sg>0","OK"};
		final String[] expectedMeanings = {"p,quest","vai.fin","vii.fin","medial","subjunctive","vai+o.fin","vintr.fin","thm(vta)","thm(vti.non3)","imp","p,aff"};
		Map<String, String[]> alignedVals = db.alignedMorphemesForTier("Orthography", "â");
		Assert.assertArrayEquals(expectedTypes, alignedVals.get("MorphemeType"));
		Assert.assertArrayEquals(expectedMeanings, alignedVals.get("MorphemeMeaning"));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(bout);
		oout.writeObject(db);
		oout.flush();
		oout.close();

		ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
		AlignedTypeDatabase testDb = (AlignedTypeDatabase) oin.readObject();

		Assert.assertTrue(testDb.getTierInfo().stream().filter((info) -> "MorphemeType".equals(info.getTierName())).findAny().isPresent());
		Assert.assertTrue(testDb.getTierInfo().stream().filter((info) -> "MorphemeMeaning".equals(info.getTierName())).findAny().isPresent());

		alignedVals = testDb.alignedMorphemesForTier("Orthography", "â");
		Assert.assertArrayEquals(expectedTypes, alignedVals.get("MorphemeType"));
		Assert.assertArrayEquals(expectedMeanings, alignedVals.get("MorphemeMeaning"));
	}

}
