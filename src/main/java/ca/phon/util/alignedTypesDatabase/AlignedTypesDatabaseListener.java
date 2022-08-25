package ca.phon.util.alignedTypesDatabase;

@FunctionalInterface
public interface AlignedTypesDatabaseListener {

	public void databaseEvent(AlignedTypesDatabaseEvent event);

}
