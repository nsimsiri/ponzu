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

import DataTypes.Component;
//import DataTypes.Event;

public class Component_Generator {

	public Component_Generator() {}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Component> extractComponents(String inputFile)
	{		
		ArrayList<Component> systemComponents = new ArrayList<Component> ();
		Document dom = null;		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {			
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			dom = db.parse(inputFile);	
			
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		//get the root element
		Element docEle = dom.getDocumentElement();
		
		//Get all the components
		NodeList nl = docEle.getElementsByTagName("Component");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength(); i++) {
				
				Element el = (Element)nl.item(i);
				Component newComponent = new Component(el.getAttribute("name"));
				
				//Only provided interface operations are specified
				//This has been modified, and all the component's provided and required operations 
				//will be extracted from the scenarios
				/*NodeList provided_interface = el.getElementsByTagName("Provided_interface");
				
				if(provided_interface != null)
				{
					Element el_interface = (Element) provided_interface.item(0);
					
					//Extract a list of operations from the XML file
					//In the new version, the operations are going to be extracted from SDs
					NodeList operations = el_interface.getElementsByTagName("Operation");
					
					if(operations != null)
					{
						for(int j = 0; j < operations.getLength(); j++)
						{
							Element provided_op = (Element) operations.item(j);
							
							//Every component contains own special instances of Event objects
							Event newEvent = new Event(provided_op.getAttribute("name"),"in");
							newComponent.addEventProvided(newEvent);
						}
					}
				}*/
				
				systemComponents.add(newComponent);
			}
		}		
		return (ArrayList<Component>) systemComponents.clone();
	}
}
