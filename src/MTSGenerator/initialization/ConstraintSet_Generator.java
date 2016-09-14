package MTSGenerator.initialization;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import DataTypes.Event;

public class ConstraintSet_Generator {
	
	public ConstraintSet_Generator() {}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Event> extractConstraints(String inputFile, HashMap<String,String> init){
		return extractConstraints(inputFile,init,new ArrayList<String>());
	}
	
	public ArrayList<Event> extractConstraints(String inputFile, HashMap<String,String> init, ArrayList<String> invariants)
	{		
		ArrayList<Event> systemEvents = new ArrayList<Event> ();
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
		
		//get the root elememt
		Element docEle = dom.getDocumentElement();
		
		//Obtain all the event names
		NodeList nl = docEle.getElementsByTagName("Event");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength(); i++) {
				
				Element el = (Element)nl.item(i);
				Event newEvent = new Event(el.getAttribute("name"), "inout");
				
				//Children nodes in the XML structure are pre- and postconditions
				NodeList pre_post = el.getChildNodes();
				
				if(pre_post != null)
				{
					for(int j = 0; j < pre_post.getLength(); j++)
					{
						Node elConstr = (Node) pre_post.item(j);
						String nodeType = elConstr.getNodeName();
						
						if(nodeType.equals("Precondition"))
						{
							newEvent.addPre(elConstr.getTextContent());
						}
						else if(nodeType.equals("Postcondition"))
						{
							newEvent.addPost(elConstr.getTextContent());
						}
					}
				}
				
				systemEvents.add(newEvent);
			}
		}
		
		//Extract initial values of system state variables
		nl = docEle.getElementsByTagName("Initial");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength(); i++) {
				
				Element el = (Element)nl.item(i);
				
				NodeList init_val = el.getElementsByTagName("Variable");
				
				if(init_val != null)
				{
					for(int j = 0; j < init_val.getLength(); j++)
					{
						Element elVar = (Element) init_val.item(j);
						String type = elVar.getAttribute("type");
						String inv = elVar.getAttribute("cond").trim();
						if(inv!=null && inv!=""){
							invariants.add(inv);
						}
						if(type!=null && type.equals("array")){
							//Do some array stuff
							String lengthStr = elVar.getAttribute("length");
							int length=0;
							if(lengthStr==null)
								System.out.println("Need length for array.");
							else
								length = Integer.parseInt(lengthStr);
							//yicesExprs.add("(define f::(-> int int))");
							for(int k=0;k<length;k++){
								String arrayName = "array_"+elVar.getAttribute("name")+"["+k+"]";
								//String arrayName = "( "+elVar.getAttribute("name")+" "+k+" )";
								//yicesExprs.add("(assert(= (f x) 1))");
								init.put(arrayName, "("+arrayName+" = "+elVar.getAttribute("value")+")");
							}
							
						}
						else
							init.put(elVar.getAttribute("name"), elVar.getAttribute("value"));
					}
				}
			}
		}		
		return (ArrayList<Event>) systemEvents.clone();
	}
}
