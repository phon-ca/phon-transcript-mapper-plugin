package ca.phon.util.alignedTypesDatabase;

public class AlignedTypesDatabaseEvent {

	public static enum EventType {
		TierAdded,
		TierRemoved,
		TypeInserted,
		AlignmentAdded,
		AlignmentIncremented,
		AlignmentDecremented,
		AlignmentRemoved
	};

	private final EventType eventType;

	private final Object eventData;

	public AlignedTypesDatabaseEvent(EventType eventType, Object eventData) {
		super();

		this.eventType = eventType;
		this.eventData = eventData;
	}

	public EventType getEventType() {
		return eventType;
	}

	public Object getEventData() {
		return eventData;
	}
}
