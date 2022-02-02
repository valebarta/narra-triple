package narra.triplifier.resource;

import java.util.ArrayList;

public class Event {

	private String uri;
	private String eventType;
	private String startDate;
	private String endDate;
	private String eventTitle;
	private String eventNotes;
	private String desc;

	private ArrayList<Proposition> propList = new ArrayList<Proposition>();
	
	private ArrayList<String> digitalObject = new ArrayList<String>();
	private ArrayList<String> causedBy = new ArrayList<String>();
	private ArrayList<String> partOf = new ArrayList<String>();

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType.trim();
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate.trim();
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate.trim();
	}

	public String getEventTitle() {
		return eventTitle;
	}

	public void setEventTitle(String eventTitle) {
		this.eventTitle = eventTitle.trim();
	}

	public ArrayList<String> getDigitalObjects() {
		return digitalObject;
	}

	public void setDigitalObjects(ArrayList<String> digitalObject) {
		this.digitalObject = digitalObject;
	}

	public String getEventNotes() {
		return eventNotes;
	}

	public void setEventNotes(String eventNotes) {
		this.eventNotes = eventNotes.trim();
	}

	public ArrayList<Proposition> getPropositionList() {
		return propList;
	}

	public void setPropositionList(ArrayList<Proposition> propositionList) {
		this.propList = propositionList;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri.trim();
	}

	public ArrayList<String> getCausedBy() {
		return causedBy;
	}
	
	public void setCausedBy(ArrayList<String> causedBy) {
		this.causedBy = causedBy;
	}
	
	public ArrayList<String> getPartOf() {
		return partOf;
	}
	
	public void setPartOf(ArrayList<String> partOf) {
		this.partOf = partOf;
	}
	
	public void setDescription(String desc) {
		this.desc = desc;
	}
	
	public String getDescription() {
		return desc;
	}
}