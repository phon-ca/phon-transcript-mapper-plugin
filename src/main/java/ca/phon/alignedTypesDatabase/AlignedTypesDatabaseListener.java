package ca.phon.alignedTypesDatabase;

@FunctionalInterface
public interface AlignedTypesDatabaseListener {

	public void databaseEvent(AlignedTypesDatabaseEvent event);

}
