package ca.phon.alignedType;

import ca.phon.session.SystemTierType;
import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.syllabifier.Syllabifier;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.util.*;

@RunWith(JUnit4.class)
public class TestAlignedMorphemesDatabase {

	private final static String CSV_NAME = "Catootje.csv";

	private AlignedTypesDatabase loadDatabase() throws IOException {
		// add database from csv data
		final AlignedTypesDatabase db = new AlignedTypesDatabase();
		db.importFromCSV(new File(CSV_NAME), "UTF-8");
		return db;
	}

	@Test
	public void testDb() throws IOException {
		AlignedTypesDatabase db = loadDatabase();
//		Map<String, String> alignedVals = new LinkedHashMap<>();
//		alignedVals.put("Orthography", "mama");
//		alignedVals.put("IPA Target", "ˈmɑma");
//		alignedVals.put("IPA Actual", "ˈmɑmaː");
//		db.addAlignedTypes(alignedVals);
		System.out.println("Hello world");
	}

//	@Test
//	public void testDatabaseSerialization() throws IOException, ClassNotFoundException {
//		AlignedTypesDatabase db = loadDatabase();
//
//		Assert.assertTrue(db.getTierInfo().stream().filter((info) -> "MorphemeType".equals(info.getTierName())).findAny().isPresent());
//		Assert.assertTrue(db.getTierInfo().stream().filter((info) -> "MorphemeMeaning".equals(info.getTierName())).findAny().isPresent());
//
//		final String[] expectedTypes = {"p,quest","vai.fin","vii.fin","medial","subjunctive","vai+o.fin","spatial","thm(vta)","thm(vti.non3)","2.sg","2.sg>0","OK"};
//		final String[] expectedMeanings = {"p,quest","vai.fin","vii.fin","medial","subjunctive","vai+o.fin","vintr.fin","thm(vta)","thm(vti.non3)","imp","p,aff"};
//		Map<String, String[]> alignedVals = db.alignedTypesForTier("Orthography", "â");
//		Assert.assertArrayEquals(expectedTypes, alignedVals.get("MorphemeType"));
//		Assert.assertArrayEquals(expectedMeanings, alignedVals.get("MorphemeMeaning"));
//
//		ByteArrayOutputStream bout = new ByteArrayOutputStream();
//		ObjectOutputStream oout = new ObjectOutputStream(bout);
//		oout.writeObject(db);
//		oout.flush();
//		oout.close();
//
//		ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
//		AlignedTypesDatabase testDb = (AlignedTypesDatabase) oin.readObject();
//
//		Assert.assertTrue(testDb.getTierInfo().stream().filter((info) -> "MorphemeType".equals(info.getTierName())).findAny().isPresent());
//		Assert.assertTrue(testDb.getTierInfo().stream().filter((info) -> "MorphemeMeaning".equals(info.getTierName())).findAny().isPresent());
//
//		alignedVals = testDb.alignedTypesForTier("Orthography", "â");
//		Assert.assertArrayEquals(expectedTypes, alignedVals.get("MorphemeType"));
//		Assert.assertArrayEquals(expectedMeanings, alignedVals.get("MorphemeMeaning"));
//	}

}
