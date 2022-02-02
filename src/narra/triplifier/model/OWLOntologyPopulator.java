package narra.triplifier.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import narra.triplifier.resource.ActorWithRole;
import narra.triplifier.resource.Concept;
import narra.triplifier.resource.Entity;
import narra.triplifier.resource.Event;
import narra.triplifier.resource.Narrative;
import narra.triplifier.resource.PhysicalObject;
import narra.triplifier.resource.Place;
import narra.triplifier.resource.Proposition;
import narra.triplifier.resource.Reference;
import narra.triplifier.resource.Vocabulary;
import narra.triplifier.resource.Work;
import narra.triplifier.util.Log4JClass;

public class OWLOntologyPopulator {
	
	private HashMap<String, String> eventTypes = new HashMap<String, String>();

	// Get logger instance
	private Logger log = null;
	private OWLDataFactory dataFactory;
	
	public OWLOntologyPopulator() {
		dataFactory = OWLManager.getOWLDataFactory();
		log = Log4JClass.getLogger();
	}

	/**
	 * Loads a narrative and triplifies it according to our ontology model
	 * @param model The ontology model expressed through OWL API
	 * @param narrative A Narrative object containing the data to be triplified
	 * @return void
	 */
	public void populateOntology(OWLOntology model, Narrative narrative) {
		
		// CIDOC CRM event types
		eventTypes.put("creation",     Vocabulary.ecrm + Vocabulary.ecrmNames.E65_Creation.toString());
		eventTypes.put("birth",        Vocabulary.ecrm + Vocabulary.ecrmNames.E67_Birth.toString());
		eventTypes.put("death",        Vocabulary.ecrm + Vocabulary.ecrmNames.E69_Death.toString());
		eventTypes.put("joining",      Vocabulary.ecrm + Vocabulary.ecrmNames.E85_Joining.toString());
		eventTypes.put("leaving",      Vocabulary.ecrm + Vocabulary.ecrmNames.E86_Leaving.toString());

		// Wikidata event types
		eventTypes.put("baptism",      Vocabulary.wikidata + "Q35856");
		eventTypes.put("battle",       Vocabulary.wikidata + "Q178561");
		eventTypes.put("crowning",     Vocabulary.wikidata + "Q209715");
		eventTypes.put("divorce",      Vocabulary.wikidata + "Q93190");
		eventTypes.put("education",    Vocabulary.wikidata + "Q8434");
		eventTypes.put("election",     Vocabulary.wikidata + "Q40231");
		eventTypes.put("exile",        Vocabulary.wikidata + "Q188863");
		eventTypes.put("expedition",   Vocabulary.wikidata + "Q2401485");
		eventTypes.put("lawmaking",    Vocabulary.wikidata + "Q1725430");
		eventTypes.put("lecture",      Vocabulary.wikidata + "Q603773");
		eventTypes.put("legation",     Vocabulary.wikidata + "Q2737008");
		eventTypes.put("marriage",     Vocabulary.wikidata + "Q8445");
		eventTypes.put("meeting",      Vocabulary.wikidata + "Q2761147");
		eventTypes.put("residence",    Vocabulary.wikidata + "Q2359691");
		eventTypes.put("sentence",     Vocabulary.wikidata + "Q1763090");
		eventTypes.put("travel",       Vocabulary.wikidata + "Q61509");
		eventTypes.put("war",          Vocabulary.wikidata + "Q198");
		
		// Set ID of first textual fragment
		int fragmentID = 1;
		
		// Event type URI will be set later
		String eventTypeURI = null;
		final String DCTypeText = "http://purl.org/dc/dcmitype/Text";
		try {
			//log.info("ENTER OWLOntologyPopulator");
			
			// Load list of events
			ArrayList<Event> eventList = narrative.getEvents();
			
			// Load list of linked resources
			HashMap<String, Entity> wikiResources = narrative.getEntities();
			
			// Generate random ID for narrator -- FIXME should be set by the tool
			//UUID narratorID = UUID.randomUUID();
			
			// Load ID from narrative object
			String narraID = narrative.getId();
						
			// Create resources for narrator, narrative, and creation event
			String resource_Narrator = Vocabulary.base + "narrator/" + narrative.getAuthor().replace(" ", "_");			
			String resource_Narrative = Vocabulary.base + "narrative/" + narraID;
			String resource_NarrativeType =  Vocabulary.narra + Vocabulary.narraNames.Narrative;
			String resource_NarrativeCreationEvent = Vocabulary.base + "narrative/" + narraID + "/creation";
			
			// Add narrative name and author to the model
			addDataPropertyAssertion(model, resource_Narrative, RDFS.label.toString(), narrative.getName(), null);
			addDataPropertyAssertion(model, resource_Narrator, RDFS.label.toString(), narrative.getAuthor(), null);
						
			// Add relations between creation event, narrator and narrative 
			addObjectPropertyAssertion(model, resource_NarrativeCreationEvent, Vocabulary.ecrm+Vocabulary.ecrmNames.P14_carried_out_by.toString(), resource_Narrator,
					new String[]{Vocabulary.ecrm+Vocabulary.ecrmNames.E5_Event, Vocabulary.ecrm+Vocabulary.ecrmNames.E39_Actor});
			addObjectPropertyAssertion(model, resource_NarrativeCreationEvent, Vocabulary.ecrm+Vocabulary.ecrmNames.P94_has_created.toString(), resource_Narrative, 
					new String[]{null, resource_NarrativeType});

			// For each event in list...
			for (int j = 0; j < eventList.size(); j++) {

				// Load event and URI
				Event event = eventList.get(j);
				String eventURI = Vocabulary.base + "narrative/" + narraID + "/" + event.getUri();
				
				addObjectPropertyAssertion(model, eventURI, 
						Vocabulary.narra + Vocabulary.narraNames.partOfNarrative.toString(), 
						resource_Narrative, new String[]{Vocabulary.ecrmNames.E5_Event.toString(),Vocabulary.narra + Vocabulary.narraNames.Narrative.toString()});
				
				// Log event number and URI
				log.debug("Event " + j + ": " + eventURI);

				// Load event properties
				String eventTitle = event.getEventTitle();
				String eventType = event.getEventType();
				String startDate = event.getStartDate();
				String endDate = event.getEndDate();
				ArrayList<String> digObjsURI = event.getDigitalObjects();
				String eventNotes = event.getEventNotes();
				
				// Fix dates for datatype XSD:date
				if (startDate.replace("-","").length() < 5) startDate += "-01-01";
				if (endDate.replace("-","").length() < 5) endDate += "-12-31";
				
				if (! startDate.startsWith("-")) {
					if (startDate.split("-")[0].length() == 2) {
						startDate = "00" + startDate;
					}
					else if (startDate.split("-")[0].length() == 3) {
						startDate = "0" + startDate;
					}
				} else {
					startDate = startDate.substring(1);
					if (startDate.split("-")[0].length() == 2) {
						startDate = "00" + startDate;
					}
					else if (startDate.split("-")[0].length() == 3) {
						startDate = "0" + startDate;
					}
					startDate = "-" + startDate;
				}
				
				if (! endDate.startsWith("-")) {
					if (endDate.split("-")[0].length() == 2) {
						endDate = "00" + endDate;
					}
					else if (endDate.split("-")[0].length() == 3) {
						endDate = "0" + endDate;
					}
				} else {
					endDate = endDate.substring(1);
					if (endDate.split("-")[0].length() == 2) {
						endDate = "00" + endDate;
					}
					else if (endDate.split("-")[0].length() == 3) {
						endDate = "0" + endDate;
					}
					endDate = "-" + endDate;
				}
				
				// Load causal and mereological relations attached to event
				ArrayList<String> eventCausedBy = event.getCausedBy();
				ArrayList<String> eventPartOf = event.getPartOf();
				
				// Load propositions attached to event
				ArrayList<Proposition> propositionList = event.getPropositionList();

				// If event type is known, load it from event type list
				if (eventTypes.containsKey(eventType)) {
					eventTypeURI = eventTypes.get(eventType);
				} else {
					log.debug("Missing URI for event type: " + eventType);
					eventTypeURI = Vocabulary.base + "event-type/" + eventType.replace(" ", "_");
				}
				
				String eventTitleURI = Vocabulary.base + "appellation/" + eventTitle.replace(" ", "_");
				// Log event title and type
				log.debug("Event title: " + eventTitle);
				log.debug("Event type: " + eventTypeURI);
				
				// Event type must be a subclass of ecrm:E5_Event
				OWLSubClassOfAxiom subClassAxiom = dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLClass(eventTypeURI),
						dataFactory.getOWLClass(Vocabulary.ecrm + Vocabulary.ecrmNames.E5_Event));
				AddAxiom addAxiomChange = new AddAxiom(model, subClassAxiom);
				model.applyChange(addAxiomChange);

				// Add mereological relations to the model (invert part of == consists of) and create resources for event, event title, event type; 
				// and create literal for event title and for start/end dates
				for (int i = 0; i < eventPartOf.size(); i++) {
					addObjectPropertyAssertion(model, Vocabulary.base + "narrative/" + narraID + "/event/" + eventPartOf.get(i), 
							Vocabulary.ecrmNames.P9_consists_of.toString(), 
							eventURI, new String[]{null,eventTypeURI});
				}
				
				// Add causal relations to model
				for (int i = 0; i < eventCausedBy.size(); i++) {
					addObjectPropertyAssertion(model, eventURI, 
							Vocabulary.narra + Vocabulary.narraNames.causallyDependsOn.toString(), 
							Vocabulary.base + "narrative/" + narraID + "/event/" + eventCausedBy.get(i), new String[]{eventTypeURI,null});
				}

				// Add digital object and link it to event
				if (digObjsURI != null && digObjsURI.size() > 0) {
					for (String digObjURI: digObjsURI) {
						log.debug(digObjURI);
						addObjectPropertyAssertion(model, digObjURI, 
								Vocabulary.ecrm+Vocabulary.ecrmNames.P67_refers_to.toString(), 
								eventURI, new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E73_Information_Object,eventTypeURI});	
					}
				}

				// Add event notes and link them to event
				if (eventNotes != null && !eventNotes.equals("")) {
					log.info(eventNotes);
					addDataStringPropertyAssertions(model, eventURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), eventNotes, 
							eventTypeURI);
				}

				String timeSpan = Vocabulary.base + "time/" + startDate + "-" + endDate;
				String timeSpanType = Vocabulary.time + Vocabulary.ecrmNames.E52_Time_Span;
				String instantType = Vocabulary.time + Vocabulary.timeNames.Instant;
				
				// Create beginning/end instants for time span, and add relations between time instants and date literals
				addDataPropertyAssertion(model, Vocabulary.base + "time/" + startDate, Vocabulary.time+Vocabulary.timeNames.inXSDDate.toString(), 
						startDate, new String[]{instantType, XSDVocabulary.DATE.getIRI().toString()});
				addDataPropertyAssertion(model, Vocabulary.base + "time/" + endDate, Vocabulary.time+Vocabulary.timeNames.inXSDDate.toString(), 
						endDate, new String[]{instantType, XSDVocabulary.DATE.getIRI().toString()});

				// Add relations between time span and its beginning/end instants
				addObjectPropertyAssertion(model, timeSpan, Vocabulary.narra + Vocabulary.narraNames.timeSpanStartedBy.toString(), 
						Vocabulary.base + "time/" + startDate, new String[]{timeSpanType, instantType});
				addObjectPropertyAssertion(model, timeSpan, Vocabulary.narra + Vocabulary.narraNames.timeSpanFinishedBy.toString(), 
						Vocabulary.base + "time/" + endDate, new String[]{timeSpanType, instantType});
				
				// Add relations between event and time span
				addObjectPropertyAssertion(model, eventURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P4_has_time_span.toString(), 
						timeSpan, new String[]{eventTypeURI, timeSpanType});

				// Add relations between event, title resource and title literal
				addObjectPropertyAssertion(model, eventURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), 
						eventTitleURI, new String[]{eventTypeURI, Vocabulary.ecrm+Vocabulary.ecrmNames.E41_Appellation});
				addDataPropertyAssertion(model, eventTitleURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), 
						eventTitle, new String[]{null, XSDVocabulary.STRING.getIRI().toString()});
				
				// Add type ecrm:E5_Event explicitly
				addObjectPropertyAssertion(model, eventURI, RDF.TYPE.toString(), Vocabulary.ecrm+Vocabulary.ecrmNames.E5_Event.toString(), new String[]{null,null});

				try {
					// For each proposition...
					for (int i = 0; i < propositionList.size(); i++) {

						// Log proposition number
						log.debug("PROPOSIZIONE " + i);

						// Load proposition from list
						Proposition proposition = propositionList.get(i);

						// Set URIs for proposition resources
						String propositionURI = eventURI + "/proposition/" + (i + 1);
						String propositionSetURI = eventURI + "/proposition-set/" + (i + 1);
						String propositionBeliefURI = eventURI + "/belief/" + (i + 1);

						// Log URIs for proposition resources
						log.debug(propositionURI);
						log.debug(propositionSetURI);
						log.debug(propositionBeliefURI);
						log.debug(proposition.getResourceID());

						String resourceID = proposition.getResourceID();
						String propositionNotes = proposition.getNotes();
						Entity wikiResource = wikiResources.get(resourceID);

						String resourceType = wikiResource.getType();

						if (propositionNotes != null) {
							addDataPropertyAssertion(model, propositionURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), 
									propositionNotes, new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, XSDVocabulary.STRING.getIRI().toString()});
						}

						addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propSubject.toString(), eventURI, new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, eventTypeURI});
						addObjectPropertyAssertion(model, propositionSetURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P106_is_composed_of.toString(), propositionURI, 
								new String[]{Vocabulary.crminf+Vocabulary.crminfNames.I4_Proposition_Set, Vocabulary.narra + Vocabulary.narraNames.Proposition});
						addObjectPropertyAssertion(model, propositionBeliefURI, Vocabulary.crminf+Vocabulary.crminfNames.J4_that.toString(), propositionSetURI,
								new String[]{Vocabulary.crminf+Vocabulary.crminfNames.I2_Belief, null});
							
						// The narrator needs to be linked to his/her belief
						addObjectPropertyAssertion(model, resource_Narrator, Vocabulary.narra+Vocabulary.narraNames.holdsBelief.toString(), propositionBeliefURI,
								new String[]{Vocabulary.crminf+Vocabulary.crminfNames.I2_Belief, null});
						
						log.debug(resourceType);

						if (resourceType.equals("person") || resourceType.equals("organization")) {
							log.debug(wikiResource.getName());
							ActorWithRole personWithRole = (ActorWithRole) wikiResource;
							String actorAppellationURI = personWithRole.getPersonURI() + "/appellation";
							
							addDataPropertyAssertion(model, personWithRole.getURI(), RDFS.label.toString(), personWithRole.getName(), new String[]{null, null});
							addDataPropertyAssertion(model, personWithRole.getURI(), RDFS.comment.toString(), personWithRole.getDescription(), new String[]{null, null});
							
							addObjectPropertyAssertion(model, personWithRole.getURI(), Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), actorAppellationURI, new String[]{null, null});
							addDataPropertyAssertion(model, actorAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), personWithRole.getName(), 
									new String[]{Vocabulary.ecrm+Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});

							String role = personWithRole.getRole();
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra + Vocabulary.narraNames.propPredicate.toString(), Vocabulary.narra+Vocabulary.narraNames.hadParticipant.toString(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, null});

							if (resourceType == "person") {
								addObjectPropertyAssertion(model, personWithRole.getURI(), RDF.TYPE.toString(), Vocabulary.ecrm + Vocabulary.ecrmNames.E21_Person, new String[]{null,null});
							}
							
							addObjectPropertyAssertion(model, personWithRole.getURI(), Vocabulary.narra+Vocabulary.narraNames.hasSubject.toString(), personWithRole.getPersonURI(),
									new String[]{Vocabulary.narra + Vocabulary.narraNames.ActorWithRole, Vocabulary.ecrm + Vocabulary.ecrmNames.E39_Actor});
							
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propObject.toString(), personWithRole.getURI(), new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, null});

							if (role != null) {
								addObjectPropertyAssertion(model, personWithRole.getURI(), Vocabulary.narra+Vocabulary.narraNames.hasRole.toString(), Vocabulary.base + "role/" + role.replace(" ", "_"), new String[]{null, null});
							}
						}

						else if (resourceType.equals("object")) {
							PhysicalObject object = (PhysicalObject) wikiResource;
							String objectAppellationURI = object.getURI() + "/appellation";
							
							addDataPropertyAssertion(model, object.getURI(), RDFS.label.toString(), object.getName(), new String[]{null, null});
							addDataPropertyAssertion(model, object.getURI(), RDFS.comment.toString(), object.getDescription(), new String[]{null, null});
							
							addObjectPropertyAssertion(model, object.getURI(), Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), objectAppellationURI, 
									new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E19_Physical_Object, null});
							addDataPropertyAssertion(model, objectAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), object.getName(), 
									new String[]{Vocabulary.ecrm+Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propPredicate.toString(), Vocabulary.ecrm+Vocabulary.ecrmNames.P12_occurred_in_the_presence_of.toString(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, null});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propObject.toString(), object.getURI(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, Vocabulary.ecrm + Vocabulary.ecrmNames.E19_Physical_Object});
						}

						else if (resourceType.equals("concept")) {
							Concept object = (Concept) wikiResource;
							String objectAppellationURI = object.getURI() + "/appellation";
							
							addDataPropertyAssertion(model, object.getURI(), RDFS.label.toString(), object.getName(), new String[]{null, null});
							addDataPropertyAssertion(model, object.getURI(), RDFS.comment.toString(), object.getDescription(), new String[]{null, null});

							addObjectPropertyAssertion(model, object.getURI(), Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), objectAppellationURI, 
									new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E89_Propositional_Object, null});
							addDataPropertyAssertion(model, objectAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), object.getName(), 
									new String[]{Vocabulary.ecrm+Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propPredicate.toString(), Vocabulary.ecrm+Vocabulary.ecrmNames.P12_occurred_in_the_presence_of.toString(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, null});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propObject.toString(), object.getURI(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, Vocabulary.ecrm + Vocabulary.ecrmNames.E89_Propositional_Object});
						}

						else if (resourceType.equals("work")) {
							Work object = (Work) wikiResource;
							String objectAppellationURI = object.getURI() + "/appellation";
							
							addDataPropertyAssertion(model, object.getURI(), RDFS.label.toString(), object.getTitle(), new String[]{null, null});
							addDataPropertyAssertion(model, object.getURI(), RDFS.comment.toString(), object.getDescription(), new String[]{null, null});

							addObjectPropertyAssertion(model, object.getURI(), Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), objectAppellationURI, 
									new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E73_Information_Object, null});
							addDataPropertyAssertion(model, objectAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), object.getTitle(), 
									new String[]{Vocabulary.ecrm+Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propPredicate.toString(), Vocabulary.ecrm+Vocabulary.ecrmNames.P12_occurred_in_the_presence_of.toString(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, null});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propObject.toString(), object.getURI(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, Vocabulary.ecrm + Vocabulary.ecrmNames.E73_Information_Object});
						}

						else if (resourceType.equals("place")) {
							Place place = (Place) wikiResource;
							String objectAppellationURI = place.getURI() + "/appellation";
							
							addDataPropertyAssertion(model, place.getURI(), RDFS.label.toString(), place.getName(), new String[]{null, null});
							addDataPropertyAssertion(model, place.getURI(), RDFS.comment.toString(), place.getDescription(), new String[]{null, null});

							addObjectPropertyAssertion(model, place.getURI(), Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), objectAppellationURI, 
									new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E53_Place, null});
							addDataPropertyAssertion(model, objectAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), place.getName(), 
									new String[]{Vocabulary.ecrm+Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propPredicate.toString(), Vocabulary.ecrm+Vocabulary.ecrmNames.P7_took_place_at.toString(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, null});
							addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.propObject.toString(), place.getURI(), 
									new String[]{Vocabulary.narra + Vocabulary.narraNames.Proposition, Vocabulary.ecrm + Vocabulary.ecrmNames.E53_Place});
						}

						ArrayList<Reference> referenceList = proposition.getReferenceList();

						try {
							for (int k = 0; k < referenceList.size(); k++) {								
								Reference reference = referenceList.get(k);

								Work source = reference.getSource();

								if (source != null) {
									String sourceURI = source.getURI();
									String sourceAuthorAppellationURI = source.getAuthor().getURI() + "/appellation";
									String sourceAppellationURI = sourceURI + "/appellation";
									String sourceCreationEventURI = sourceURI + "/creation";

									addObjectPropertyAssertion(model, source.getAuthor().getURI(), Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), 
											sourceAuthorAppellationURI, new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E39_Actor, Vocabulary.ecrm + Vocabulary.ecrmNames.E41_Appellation});
									
									addDataPropertyAssertion(model, sourceAuthorAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(),
											source.getAuthor().getName(), new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});
									
									addObjectPropertyAssertion(model, sourceURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P1_is_identified_by.toString(), sourceAppellationURI, 
											new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E73_Information_Object, Vocabulary.ecrm + Vocabulary.ecrmNames.E41_Appellation});
									
									addDataPropertyAssertion(model, sourceAppellationURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P3_has_note.toString(), 
											source.getTitle(), new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E41_Appellation, XSDVocabulary.STRING.getIRI().toString()});
									
									addObjectPropertyAssertion(model, sourceCreationEventURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P94_has_created.toString(), sourceURI, 
											new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E5_Event, Vocabulary.ecrm + Vocabulary.ecrmNames.E73_Information_Object});
									
									addObjectPropertyAssertion(model, sourceCreationEventURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P14_carried_out_by.toString(), 
											source.getAuthor().getURI(), new String[]{Vocabulary.ecrm + Vocabulary.ecrmNames.E5_Event, Vocabulary.ecrm + Vocabulary.ecrmNames.E39_Actor});

									// If source is primary...
									if (source.getSourceType() == "primary") {
									log.debug(sourceURI);
									String observationURI = propositionURI + "/observation/" + (k + 1);				
									String observedPropositionSetURI = propositionURI + "/observed-proposition-set/" + (k + 1);
									log.debug(observationURI);
									log.debug(observedPropositionSetURI);
									
									String premiseBeliefURI = eventURI + "/premise-belief/" + (i + 1);
									String inferenceMakingURI = eventURI + "/inference-making/" + (i + 1);
									addObjectPropertyAssertion(model, inferenceMakingURI, Vocabulary.crminf+Vocabulary.crminfNames.J2_concluded_that.toString(), premiseBeliefURI,
											new String[]{Vocabulary.crminf+Vocabulary.crminfNames.I5_Inference_Making, null});
									addObjectPropertyAssertion(model, premiseBeliefURI, Vocabulary.crminf+Vocabulary.crminfNames.J1_was_premise_for.toString(), inferenceMakingURI,
											new String[]{Vocabulary.crminf+Vocabulary.crminfNames.I2_Belief, null});
									addObjectPropertyAssertion(model, resource_Narrator, Vocabulary.narra+Vocabulary.narraNames.holdsBelief.toString(), premiseBeliefURI,
											new String[]{Vocabulary.crminf+Vocabulary.crminfNames.I2_Belief, null});

									addObjectPropertyAssertion(model, premiseBeliefURI, Vocabulary.crminf+Vocabulary.crminfNames.J4_that.toString(), observedPropositionSetURI, 
											new String[]{null, Vocabulary.crminf + Vocabulary.crminfNames.I4_Proposition_Set});
									
									addObjectPropertyAssertion(model, observationURI, Vocabulary.crmsci+Vocabulary.crmsciNames.O16_observed_value.toString(), observedPropositionSetURI, 
											new String[]{Vocabulary.crmsci + Vocabulary.crmsciNames.O8_observed, null});
									
									addObjectPropertyAssertion(model, observationURI, Vocabulary.crmsci+Vocabulary.crmsciNames.O8_observed.toString(), sourceURI, new String[]{null, null});

									addObjectPropertyAssertion(model, observationURI, Vocabulary.crminf+Vocabulary.ecrmNames.P14_carried_out_by.toString(), resource_Narrator, new String[]{null, null});

									addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.hasSource.toString(), sourceURI, new String[]{null, null});

									}
									
									String textFragment = reference.getTextFragment();
									String referenceFragment = reference.getReferenceFragment();
									String textFragmentURI = sourceURI + "/text-fragment/" + fragmentID;
									
									if (textFragment != null) {
										addDataPropertyAssertion(model, textFragmentURI, Vocabulary.cnt+Vocabulary.cntNames.chars.toString(), textFragment, 
												new String[]{Vocabulary.efrbroo + Vocabulary.efrbrooNames.F23_Expression_Fragment, XSDVocabulary.STRING.getIRI().toString()});
										log.debug(sourceURI + "/text/" + fragmentID);

										addObjectPropertyAssertion(model, textFragmentURI, RDF.TYPE.toString(), Vocabulary.cnt+Vocabulary.cntNames.ContentAsText.toString(), 
												new String[]{Vocabulary.efrbroo + Vocabulary.efrbrooNames.F23_Expression_Fragment, null});
										addObjectPropertyAssertion(model, textFragmentURI, RDF.TYPE.toString(), DCTypeText,
												new String[]{Vocabulary.efrbroo + Vocabulary.efrbrooNames.F23_Expression_Fragment, null});
										addDataPropertyAssertion(model, textFragmentURI, DC.FORMAT.toString(), "text/plain",
												new String[]{Vocabulary.efrbroo + Vocabulary.efrbrooNames.F23_Expression_Fragment, XSDVocabulary.STRING.getIRI().toString()});										
										addObjectPropertyAssertion(model, sourceURI, Vocabulary.ecrm+Vocabulary.ecrmNames.P106_is_composed_of.toString(), textFragmentURI, new String[]{null, null});
										
										// If source is secondary...
										if (source.getSourceType() == "secondary") {
											
											// Add relation between text fragment and its corresponding proposition
											addObjectPropertyAssertion(model, textFragmentURI, Vocabulary.ecrm + Vocabulary.ecrmNames.P129_is_about, propositionURI, new String[]{null, null});
										}
										
										// If source is primary...
										else if (source.getSourceType() == "primary") {
											
											// Add relation between primary source and text fragment
											addObjectPropertyAssertion(model, propositionURI, Vocabulary.narra+Vocabulary.narraNames.hasTextFragment.toString(), 
													textFragmentURI, new String[]{null, null});
										}
										// Increment fragment ID
										fragmentID++;
									}

									if (referenceFragment != null) {
										String referenceFragmentURI = sourceURI + "/reference-fragment/" + fragmentID;
										log.debug(sourceURI + "/reference-fragment/" + fragmentID);
										addObjectPropertyAssertion(model, referenceFragmentURI, RDF.TYPE.toString(), Vocabulary.cnt+Vocabulary.cntNames.ContentAsText.toString(), 
												new String[]{Vocabulary.efrbroo + Vocabulary.efrbrooNames.F23_Expression_Fragment, null});
										addObjectPropertyAssertion(model, referenceFragmentURI, RDF.TYPE.toString(), DCTypeText, new String[]{null, null});
										addDataPropertyAssertion(model, referenceFragmentURI, DC.FORMAT.toString(), "text/plain", new String[]{null, XSDVocabulary.STRING.getIRI().toString()});
										if (textFragment != null) {
											addObjectPropertyAssertion(model, textFragmentURI, Vocabulary.narra+Vocabulary.narraNames.hasReference.toString(), referenceFragmentURI, new String[]{null, null});
										}
										addDataPropertyAssertion(model, referenceFragmentURI, Vocabulary.cnt+Vocabulary.cntNames.chars.toString(), referenceFragment, 
												new String[]{null, XSDVocabulary.STRING.getIRI().toString()});
										// Increment fragment ID
										fragmentID++;
									}								
								}

							}
						}
						// Catch exceptions in reference creation cycle
						catch (Exception e) {
							log.error("EXCEPTION - Creating references: " + e);
							log.error(getStackTrace(e));
						}
					}
				}
				// Catch exceptions in proposition creation cycle
				catch (Exception e) {
					log.error("EXCEPTION - Creating propositions: " + e);
					log.error(getStackTrace(e));
				}
			}
			
		}
		// Catch exception in event creation cycle
		catch (Exception e) {
			log.error("EXCEPTION - Creating events: " + e);
			log.error(getStackTrace(e));
		}
	}//end method
	
	public static String getStackTrace(final Throwable throwable) {
	     final StringWriter sw = new StringWriter();
	     final PrintWriter pw = new PrintWriter(sw, true);
	     throwable.printStackTrace(pw);
	     return sw.getBuffer().toString();
	}
	
	/**
	 * @throws Exception
	 */
	public void addObjectPropertyAssertion(OWLOntology model, String subjectIRI, String predicateIRI, String objectIRI, String[] type) throws Exception {
		OWLIndividual subject = dataFactory.getOWLNamedIndividual(IRI.create(subjectIRI));
		OWLIndividual object = dataFactory.getOWLNamedIndividual(IRI.create(objectIRI));
		if(type!=null){
			if(type[0]!=null&&!"".equals(type[0])){
				OWLClass entityType = dataFactory.getOWLClass(IRI.create(type[0]));
				OWLClassAssertionAxiom axiom = dataFactory.getOWLClassAssertionAxiom(entityType, subject);
				model.addAxiom(axiom);			
			}
			if(type[1]!=null&&!"".equals(type[1])){
				OWLClass entityType = dataFactory.getOWLClass(IRI.create(type[1]));
				OWLClassAssertionAxiom axiom = dataFactory.getOWLClassAssertionAxiom(entityType, object);
				model.addAxiom(axiom);			
			}
		}
		OWLObjectProperty predicate = dataFactory.getOWLObjectProperty(IRI.create(predicateIRI));
		// Now create the assertion (triple)
		OWLObjectPropertyAssertionAxiom assertion = dataFactory.getOWLObjectPropertyAssertionAxiom(predicate, subject, object);
		AddAxiom addAxiomChange = new AddAxiom(model, assertion);
		model.applyChange(addAxiomChange);
	}//end method
	/**
	 * @throws Exception
	 */
	public void addDataPropertyAssertion(OWLOntology model, String subjectIRI, String predicateIRI, String dataLiteral, String[] type) throws Exception {
		OWLIndividual subject = dataFactory.getOWLNamedIndividual(IRI.create(subjectIRI));
		OWLLiteral object = dataFactory.getOWLLiteral(dataLiteral);
		if(type!=null){
			if(type[0]!=null&&!"".equals(type[0])){
				OWLClass entityType = dataFactory.getOWLClass(IRI.create(type[0]));
				OWLClassAssertionAxiom axiom = dataFactory.getOWLClassAssertionAxiom(entityType, subject);
				model.addAxiom(axiom);			
			}
			if(type[1]!=null&&!"".equals(type[1])){
				OWLDatatype dataType = dataFactory.getOWLDatatype(IRI.create(type[1]));
				object = dataFactory.getOWLLiteral(dataLiteral, dataType);
			}
		}
		OWLDataProperty predicate = dataFactory.getOWLDataProperty(IRI.create(predicateIRI));
		// Now create the assertion (triple)
		OWLDataPropertyAssertionAxiom assertion = dataFactory.getOWLDataPropertyAssertionAxiom(predicate, subject, object);
		AddAxiom addAxiomChange = new AddAxiom(model, assertion);
		model.applyChange(addAxiomChange);
	}//end method
	/**
	 * @throws Exception
	 */
	public void addDataStringPropertyAssertions(OWLOntology model, String subjectIRI, String predicateIRI, String dataLiteral, String type) throws Exception {
		OWLIndividual subject = dataFactory.getOWLNamedIndividual(IRI.create(subjectIRI));
		if(type!=null){
			if(type!=null&&!"".equals(type)){
				OWLClass entityType = dataFactory.getOWLClass(IRI.create(type));
				OWLClassAssertionAxiom axiom = dataFactory.getOWLClassAssertionAxiom(entityType, subject);
				model.addAxiom(axiom);			
			}
		}
		OWLDataProperty predicate = dataFactory.getOWLDataProperty(IRI.create(predicateIRI));
		// Now create the assertion (triple)
		OWLDataPropertyAssertionAxiom assertion = dataFactory.getOWLDataPropertyAssertionAxiom(predicate, subject, dataLiteral);
		AddAxiom addAxiomChange = new AddAxiom(model, assertion);
		model.applyChange(addAxiomChange);
	}//end method	
}
