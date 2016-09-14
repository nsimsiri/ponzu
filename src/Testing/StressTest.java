package Testing;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import DataTypes.Component;
import DataTypes.Event;
import DataTypes.Scenario;
import MTSGenerator.algorithm.ScenarioAnnotator;

public class StressTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int componentNumber = 50;
		int variableNumberPerComp = 4;
		int eventsPerComp = 6;
		int scenarioLength = 10;
		int scenarioNumber = 200;
		HashMap<String,String> initialValues = new HashMap<String, String>();
		ArrayList<Scenario> scenarios = new ArrayList<Scenario>();
		ArrayList<Component> components = new ArrayList<Component>();
		ArrayList<Event> events = new ArrayList<Event>();
		
		//Initialization of components, variables and operations (events)
		for(int i = 0; i < componentNumber; i++) {
			Component newComp = new Component("comp" + i);
			
			for(int j = 0; j < variableNumberPerComp; j++) {
				newComp.addAbsoluteVariable("var" + ((i * variableNumberPerComp) + j));
			}
			
			for(int j = 0; j < eventsPerComp; j++) {
				Event newEvent = new Event("op" + ((i * eventsPerComp) + j), "in");
				newComp.addEventProvided(newEvent);
				events.add(newEvent);
			}
			
			components.add(newComp);
		}
		
		Random RNum = new Random();
		
		//Setting initial variable values
		for(int i = 0; i < componentNumber * variableNumberPerComp; i++) {
			int n = RNum.nextInt(2);
			
			if(n == 1) initialValues.put("var" + i, "true");
			else initialValues.put("var" + i, "false");
		}
		
		//Generating random pre- and post-conditions
		for(int i = 0; i < componentNumber; i++) {
			Component currentComp = components.get(i);
			ArrayList<String> firstComp = currentComp.getAbsoluteVariables();
			
			for(int j = 0; j < eventsPerComp; j++) {
				Event currentEvent = currentComp.getEvent("op" + ((i * eventsPerComp) + j));
				int otherCompNumber;	//Stores which component calls the current event				
				int n = RNum.nextInt(10);
				Component currentSendingComp = null;
				ArrayList<String> secondComp = new ArrayList<String>();
				
				//Select "random" component with which we are communicating
				if(n < 4) {
					otherCompNumber = (i + 1 + componentNumber) % componentNumber;
					currentSendingComp = components.get((i + 1 + componentNumber) % componentNumber);
					secondComp = components.get((i + 1 + componentNumber) % componentNumber).getAbsoluteVariables();
				}
				else if(n < 8) {
					otherCompNumber = (i - 1 + componentNumber) % componentNumber;
					currentSendingComp = components.get((i - 1 + componentNumber) % componentNumber);
					secondComp = components.get((i - 1 + componentNumber) % componentNumber).getAbsoluteVariables();
				}
				else if(n < 9) {
					otherCompNumber = (i + 2 + componentNumber) % componentNumber;
					currentSendingComp = components.get((i + 2 + componentNumber) % componentNumber);
					secondComp = components.get((i + 2 + componentNumber) % componentNumber).getAbsoluteVariables();
				}
				else {
					otherCompNumber = (i - 2 + componentNumber) % componentNumber;
					currentSendingComp = components.get((i - 2 + componentNumber) % componentNumber);
					secondComp = components.get((i - 2 + componentNumber) % componentNumber).getAbsoluteVariables();
				}
				//We need to add the event to the calling component's interface
				currentSendingComp.addEventRequired(currentEvent);
				//Storing information about which components are communicating
				currentEvent.addInteractionPair("comp" + otherCompNumber, "comp" + i);
				
				//Randomly adding or not the variables to event's pre/postconditions
				for(int k = 0; k < variableNumberPerComp; k++) {
					n = RNum.nextInt(20);
					
					//Add the variables of the called component less often
					if(n < 1) {
						n = RNum.nextInt(2);
						currentSendingComp.addVariable(firstComp.get(k));
						
						if(n == 0) {
							currentEvent.addPre(firstComp.get(k));
							n = RNum.nextInt(5);
							
							if(n < 2) currentEvent.addPost(firstComp.get(k));
							else if(n < 4) currentEvent.addPost("!" + firstComp.get(k));
						}
						else {
							currentEvent.addPre("!" + firstComp.get(k));
							n = RNum.nextInt(5);
							
							if(n < 2) currentEvent.addPost("!" + firstComp.get(k));
							else if(n < 4) currentEvent.addPost(firstComp.get(k));
						}
					}
					//Adding the postcondition with an empty precondition
					else if(n < 8)
					{
						n = RNum.nextInt(5);
						if(n < 1) {
							n = RNum.nextInt(2);
							
							if(n == 0) {
								currentEvent.addPost(firstComp.get(k));
							}
							else {
								currentEvent.addPost("!" + firstComp.get(k));
							}
						}
					}
					
					n = RNum.nextInt(10);
					
					//Add the variables of the caller component more often
					if(n < 6) {
						n = RNum.nextInt(2);
						
						if(n == 0) {
							currentEvent.addPre(secondComp.get(k));
							n = RNum.nextInt(5);
							
							if(n < 2) currentEvent.addPost(secondComp.get(k));
							else if(n < 4) currentEvent.addPost("!" + secondComp.get(k));
						}
						else {
							currentEvent.addPre("!" + secondComp.get(k));
							n = RNum.nextInt(5);
							
							if(n < 2) currentEvent.addPost("!" + secondComp.get(k));
							else if(n < 4) currentEvent.addPost(secondComp.get(k));
						}
					}
					//Adding the postcondition with an empty precondition
					else if(n == 6)
					{
						n = RNum.nextInt(5);
						if(n < 4) {
							n = RNum.nextInt(2);
							
							if(n == 0) {
								currentEvent.addPost(secondComp.get(k));
							}
							else {
								currentEvent.addPost("!" + secondComp.get(k));
							}
						}
					}
				}
			}
		}
		
		//Creating an appropriate number of scenarios
		for(int i = 0; i < scenarioNumber; i++) {
			//First, we need to get a first caller in the scenario
			int n = RNum.nextInt(componentNumber);
			Component currentCaller = components.get(n);
			Component currentCallee = null;
			int currentScenarioLength = 0;
			Scenario currentScenario = new Scenario("scen" + i);
			int failure = 0;
			
			//Loop while the scenario does not have the necessary length
			while(currentScenarioLength < scenarioLength) {
				//Randomly get an event
				ArrayList<String> candidateEvents = currentCaller.getProvidedOperationNames();
				n = RNum.nextInt(candidateEvents.size());
				Event nextEvent = currentCaller.getEvent(candidateEvents.get(n));
				String caller = nextEvent.getInteractionIn(0);
				String callee = nextEvent.getInteractionOut(0);
				ArrayList<Scenario> newScenario = new ArrayList<Scenario>();
				
				//Just in case something in the above setup is not right, and we need to switch
				//between the caller and the callee
				if(caller.equals(currentCaller.getName())) {
					currentCallee = components.get(Integer.parseInt(callee.substring(4)));
				}
				else {
					currentCallee = currentCaller;
					currentCaller = components.get(Integer.parseInt(callee.substring(4)));
				}
				
				//Try if the created scenario is raising an exception during 
				//the annotation process
				currentScenario.addEvent(nextEvent, 0);	
				newScenario.add(currentScenario);
				try {
					ScenarioAnnotator annotate = new ScenarioAnnotator();
					annotate.annotateScenarios(components, newScenario, events, initialValues);
				}
				//If so, remove the last invocation and try building it again
				catch (Exception e) {
					currentScenario.removeLast();
					failure++;
					//If we unsuccessfully build for too many times, it is useful
					//to go back an extra step
					if(failure == 5) {
						currentScenario.removeLast();
						currentScenarioLength--;
						failure = 0;
					}
					continue;
				}
				
				currentScenarioLength++;
				n = RNum.nextInt(2);
				if(n == 0) currentCaller = currentCallee;
				failure = 0;
			}
			
			scenarios.add(currentScenario);
		}
		
		//Finally write everything to XML files
		String fileName = ".\\src\\Input_files\\StressTest10\\";
		writeComponents(fileName + "components_test.xml", components);
		writeConstraints(fileName + "constraints_test.xml", events, initialValues);
		writeScenarios(fileName + "scenarios_test.xml", scenarios);
	}
	
	public static void writeComponents(String fileName, ArrayList<Component> components) {
		Document compDoc = null;		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {			
			DocumentBuilder db = dbf.newDocumentBuilder();			
			compDoc = db.newDocument();				
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}
				
		Element root = compDoc.createElement("Components");
		for(int i = 0; i < components.size(); i++) {
			Component currentComponent = components.get(i);
			Element child = compDoc.createElement("Component");
			child.setAttribute("name", currentComponent.getName());
			root.appendChild(child);
		}
		compDoc.appendChild(root);
		
		OutputFormat format = new OutputFormat(compDoc);
		XMLSerializer serializer = null;
		try {
			serializer = new XMLSerializer(new FileOutputStream(fileName), format);
			serializer.serialize(compDoc);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeConstraints(String fileName, ArrayList<Event> events, HashMap<String,String> initialValues) {
		Document compDoc = null;		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {			
			DocumentBuilder db = dbf.newDocumentBuilder();			
			compDoc = db.newDocument();				
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}
				
		Element root = compDoc.createElement("Constraints");
		for(int i = 0; i < events.size(); i++) {
			Event currentEvent = events.get(i);
			Element child = compDoc.createElement("Event");
			child.setAttribute("name", currentEvent.getName());
			
			ArrayList<String> preconditions = currentEvent.getPreCond();
			for(int j = 0; j < preconditions.size(); j++) {
				Element subChild = compDoc.createElement("Precondition");
				subChild.setTextContent(preconditions.get(j));
				child.appendChild(subChild);
			}
			
			ArrayList<String> postconditions = currentEvent.getPostCond();
			for(int j = 0; j < postconditions.size(); j++) {
				Element subChild = compDoc.createElement("Postcondition");
				subChild.setTextContent(postconditions.get(j));
				child.appendChild(subChild);
			}
			
			root.appendChild(child);
		}
		
		Element initials = compDoc.createElement("Initial");
		Set<String> keys = initialValues.keySet();
		Iterator<String> keyIter = keys.iterator();
		
		for(int i = 0; i < initialValues.size(); i++) {
			Element var = compDoc.createElement("Variable");
			String currKey = keyIter.next();
			var.setAttribute("name", currKey);
			var.setAttribute("value", initialValues.get(currKey));
			initials.appendChild(var);
		}
		root.appendChild(initials);
		compDoc.appendChild(root);
		
		OutputFormat format = new OutputFormat(compDoc);
		XMLSerializer serializer = null;
		try {
			serializer = new XMLSerializer(new FileOutputStream(fileName), format);
			serializer.serialize(compDoc);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeScenarios(String fileName, ArrayList<Scenario> scenarios) {
		Document compDoc = null;		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {			
			DocumentBuilder db = dbf.newDocumentBuilder();			
			compDoc = db.newDocument();				
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}
				
		Element root = compDoc.createElement("Scenarios");
		for(int i = 0; i < scenarios.size(); i++) {
			Scenario currentScenario = scenarios.get(i);
			Element child = compDoc.createElement("Scenario");
			child.setAttribute("name", currentScenario.getName());
			
			ArrayList<Event> events = currentScenario.getEventList("All");
			for(int j = 0; j < events.size(); j++) {
				Event currEvent = events.get(j);
				Element subChild = compDoc.createElement("Event");
				subChild.setAttribute("name", currEvent.getName());
				
				Element childComp = compDoc.createElement("Component");
				childComp.setAttribute("type", "source");
				childComp.setAttribute("name", currEvent.getInteractionOut(0));
				
				subChild.appendChild(childComp);
				
				childComp = compDoc.createElement("Component");
				childComp.setAttribute("type", "dest");
				childComp.setAttribute("name", currEvent.getInteractionIn(0));
				
				subChild.appendChild(childComp);
				child.appendChild(subChild);
			}
			
			root.appendChild(child);
		}
		compDoc.appendChild(root);
		
		OutputFormat format = new OutputFormat(compDoc);
		XMLSerializer serializer = null;
		try {
			serializer = new XMLSerializer(new FileOutputStream(fileName), format);
			serializer.serialize(compDoc);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
