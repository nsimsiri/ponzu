package MTSGenerator.initialization;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import DataTypes.Event;
import DataTypes.Scenario;

public class Scenario_Extractor {

	public Scenario_Extractor() {}
	
	public ArrayList<Scenario> extractScenarios(String inFile, ArrayList<Event> events)
	{
		ArrayList<Scenario> scenarioSet = new ArrayList<Scenario>();
		
		Document dom = null;		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {			
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			dom = db.parse(inFile);	
			
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		//get the root element
		Element docEle = dom.getDocumentElement();
		
		//Get all the scenarios
		NodeList nl = docEle.getElementsByTagName("Scenario");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength(); i++) {
				
				Element el = (Element)nl.item(i);
				Scenario newScenario = new Scenario(el.getAttribute("name"));
				
				//Get all the operations in the scenario
				NodeList scenario_events = el.getElementsByTagName("Event");
				
				if(scenario_events != null)
				{
					for(int j = 0; j < scenario_events.getLength(); j++)
					{
						Element event_single = (Element) scenario_events.item(j);
						String eventName = event_single.getAttribute("name");
						Event currEvent = null;
						
						//Iterate until that event is found in the list of events
						for(int k = 0; k < events.size(); k++)
						{
							currEvent = events.get(k);
							if(currEvent.getName().equals(eventName)) break;
						}
						
						NodeList actors = event_single.getElementsByTagName("Component");
						
						if(actors != null)
						{
							String source = null;
							String dest = null;
							
							for(int k = 0; k < actors.getLength(); k++)
							{
								Element actor = (Element) actors.item(k);
								
								if(actor.getAttribute("type").equals("source")) source = actor.getAttribute("name");
								else dest = actor.getAttribute("name");
							}
							
							//Add the interaction pair to the event and store it also in the scenario object
							int index = currEvent.addInteractionPair(source, dest);
							newScenario.addEvent(currEvent, index);
						}						
					}
				}				
				scenarioSet.add(newScenario);
			}
		}	
		return scenarioSet;
	}
}
