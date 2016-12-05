package MTSGenerator.output;

import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import DataTypes.*;
import daikon.VarInfo;
import daikon.inv.Invariant;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import daikon.PptName;

public class outputResults {

	public AnalysisInstance instance;
	public outputResults() {}
	public outputResults(AnalysisInstance instance) {this.instance = instance;}
	
	//Writes component-level constraints to an XML file
	public void outputComponentConstraints(ArrayList<Component> components, String path)
	{
		try {
		    // first of all we request out DOM-implementation:
		    DocumentBuilderFactory factory = 
		      DocumentBuilderFactory.newInstance();
		    // then we have to create document-loader:
		    DocumentBuilder loader = factory.newDocumentBuilder();

		    // creating a new DOM-document...
		    Document document = loader.newDocument();

		    // create root-element of all the components
		    Element rootElement = document.createElement("Components");
		    
		    //Create an XML element for each component in components list
		    for(int i = 0; i < components.size(); i++)
		    {
		    	Component currentComponent = components.get(i);
		    	Element componentElement = document.createElement("ConstrainedComponent");
		    	componentElement.setAttribute("name", currentComponent.getName());
		    	
		    	ArrayList<Event> componentEventList = currentComponent.getEvents();
		    	
		    	//Add to component's element a child for each event
		    	for(int j = 0; j < componentEventList.size(); j++)
		    	{
		    		Event currentEvent = componentEventList.get(j);
		    		Element eventElement = document.createElement("Event");
		    		eventElement.setAttribute("name", currentEvent.getName());
		    		
		    		//Each event has children corresponding to pre- and postconditions
		    		//for the perspective of the current component
		    		ArrayList<String> preCond = currentEvent.getPreCond();
		    		ArrayList<String> postCond = currentEvent.getPostCond();
		    		
		    		for(int k = 0; k < preCond.size(); k++)
		    		{
		    			Element preCondElement = document.createElement("Precondition");
		    			preCondElement.setTextContent(preCond.get(k));
		    			eventElement.appendChild(preCondElement);
		    		}
		    		
		    		for(int k = 0; k < postCond.size(); k++)
		    		{
		    			Element postCondElement = document.createElement("Postcondition");
		    			postCondElement.setTextContent(postCond.get(k));
		    			eventElement.appendChild(postCondElement);
		    		}
		    		
		    		componentElement.appendChild(eventElement);
		    	}
		    	rootElement.appendChild(componentElement);
		    }
		    document.appendChild(rootElement);
		    
		    // use specific Xerces class to write DOM-data to a file:
		    XMLSerializer serializer = new XMLSerializer();
		    serializer.setOutputCharStream(
		      new java.io.FileWriter(path));
		    serializer.serialize(document);

		    } catch (Exception ex) {
		      ex.printStackTrace();
		    }
	}

	//Writes annotated scenarios to an XML file
	public void outputAnnotatedScenarios(ArrayList<ComponentScenarios> componentScenarios, ArrayList<Component> components, HashMap<String,String> initial, String path)
	{
		try {
		    // first of all we request out DOM-implementation:
		    DocumentBuilderFactory factory = 
		      DocumentBuilderFactory.newInstance();
		    // then we have to create document-loader:
		    DocumentBuilder loader = factory.newDocumentBuilder();

		    // creating a new DOM-document...
		    Document document = loader.newDocument();

		    // create root-element of all the components
		    Element rootElement = document.createElement("ComponentScenarios");
		    
		    //Create an XML element with scenarios annotated from each component's perspective
		    for(int i = 0; i < componentScenarios.size(); i++)
		    {
		    	//Determine significant variables for a component
				Set<String> varNames = initial.keySet();
				Iterator<String> nameIterator = varNames.iterator();
				ArrayList<String> variables = new ArrayList<String>();
				
				while(nameIterator.hasNext())
				{
					String currentName = nameIterator.next();
					if(components.get(i).getVariables().contains(currentName))
					{
						variables.add(currentName);
					}
					else if(components.get(i).getAbsoluteVariables().contains(currentName))
					{
						variables.add(currentName);
					}
				}
		    	
				//Create XML elements for each component
		    	ComponentScenarios currentComponentScenarios = componentScenarios.get(i);
		    	Element componentElement = document.createElement("ComponentScenarios");
		    	componentElement.setAttribute("component_name", currentComponentScenarios.getName());
		    	
		    	ArrayList<Scenario> componentScenarioList = currentComponentScenarios.getScenarios();
		    	
		    	//Add to component's element a child for each scenario
		    	for(int j = 0; j < componentScenarioList.size(); j++)
		    	{
		    		Scenario currentScenario = componentScenarioList.get(j);
		    		Element scenarioElement = document.createElement("Scenario");
		    		scenarioElement.setAttribute("name", currentScenario.getName());
		    		
		    		ArrayList<Event> currentScenarioEvents = currentScenario.getEventList("All");
		    		ArrayList<ArrayList<String>> scenarioAnnotations = currentScenario.getAnnotations();
		    		
		    		//Add to scenario element a child for each event in the sequence along with annotations
			    	for(int k = 0; k < currentScenarioEvents.size(); k++)
			    	{
			    		//Adding the annotation before the event
			    		ArrayList<String> annotation = scenarioAnnotations.get(k);
			    		Element annotationElement = document.createElement("Annotation");
			    		
			    		for(int l = 0; l < annotation.size(); l++)
			    		{
			    			annotationElement.setAttribute(variables.get(l), annotation.get(l));
			    		}
			    		
			    		scenarioElement.appendChild(annotationElement);
			    		
			    		//Appending the Event with the name and its type
			    		Event currentEvent = currentScenarioEvents.get(k);
			    		Element eventElement = document.createElement("Event");
			    		eventElement.setAttribute("name", currentEvent.getName());
			    		
			    		scenarioElement.appendChild(eventElement);
			    	}
			    	
			    	//Adding the last annotation
		    		ArrayList<String> annotation = scenarioAnnotations.get(currentScenarioEvents.size());
		    		Element annotationElement = document.createElement("Annotation");
		    		
		    		for(int l = 0; l < annotation.size(); l++)
		    		{
		    			annotationElement.setAttribute(variables.get(l), annotation.get(l));
		    		}
		    		
		    		scenarioElement.appendChild(annotationElement);
		    		
		    		componentElement.appendChild(scenarioElement);
		    	}
		    	rootElement.appendChild(componentElement);
		    }
		    document.appendChild(rootElement);
		    
		    // use specific Xerces class to write DOM-data to a file:
		    XMLSerializer serializer = new XMLSerializer();
		    serializer.setOutputCharStream(
		      new java.io.FileWriter(path));
		    serializer.serialize(document);

		    } catch (Exception ex) {
		      ex.printStackTrace();
		    }
	}
	
	//Writes component MTSs to an XML file
	public void outputMTSs(ArrayList<MTS> componentMTSs, String path)
	{
		try {
		    // first of all we request out DOM-implementation:
		    DocumentBuilderFactory factory = 
		      DocumentBuilderFactory.newInstance();
		    // then we have to create document-loader:
		    DocumentBuilder loader = factory.newDocumentBuilder();

		    // creating a new DOM-document...
		    Document document = loader.newDocument();

		    // create root-element of all the components
		    Element rootElement = document.createElement("ComponentMTSs");
		    
		    //Create an XML element for MTSs of from each component
		    for(int i = 0; i < componentMTSs.size(); i++)
		    {
		    	MTS currentMTS = componentMTSs.get(i);
		    	Element componentElement = document.createElement("MTS");
		    	componentElement.setAttribute("component_name", currentMTS.getName());
		    	
		    	ArrayList<String> variables = currentMTS.getVariableNames();
		    	ArrayList<MTS_state> componentStates = currentMTS.getAllStates();
		    	
		    	//Add to component's element a child for each scenario
		    	for(int j = 0; j < componentStates.size(); j++)
		    	{
		    		MTS_state currentState = componentStates.get(j);
		    		Element stateElement = document.createElement("State");
		    		stateElement.setAttribute("name", new String("S" + currentState.getName()));
		    		
		    		ArrayList<String> currentAssignment = currentState.getVariableState();
		    		
		    		//Add to state element the attributes representing variable assignments
			    	for(int k = 0; k < currentAssignment.size(); k++)
			    	{
			    		stateElement.setAttribute(variables.get(k), currentAssignment.get(k));
			    	}
			    	
			    	componentElement.appendChild(stateElement);
		    	}	
		    	
		    	ArrayList<MTS_transition> componentTransitions = currentMTS.getAllTransitions();
		    	
		    	//Add to component's element a child for each scenario
		    	for(int j = 0; j < componentTransitions.size(); j++)
		    	{
		    		MTS_transition currentTransition = componentTransitions.get(j);
		    		Element transitionElement = document.createElement("Transition");
		    		transitionElement.setAttribute("event", currentTransition.getEvent());
		    		transitionElement.setAttribute("type", currentTransition.getType());
		    		transitionElement.setAttribute("source_state", new String("S" + currentTransition.getStart()));
		    		transitionElement.setAttribute("destination_state", new String("S" + currentTransition.getEnd()));
			    	
			    	componentElement.appendChild(transitionElement);
		    	}	
			    	
		    	rootElement.appendChild(componentElement);
		    }
		    document.appendChild(rootElement);
		    
		    // use specific Xerces class to write DOM-data to a file:
		    XMLSerializer serializer = new XMLSerializer();
		    serializer.setOutputCharStream(
		      new java.io.FileWriter(path));
		    serializer.serialize(document);

		    } catch (Exception ex) {
		      ex.printStackTrace();
		    }
	}
	
	//Outputs the final MTSs to a file appropriate for MTSA tool 
	public void outputToMTSA(ArrayList<MTS> componentMTSs, String path)
	{
		try {
			BufferedWriter outputFile = new BufferedWriter(new FileWriter(path));

			//Go through all the states and transitions and create the .lts file
			for(int i = 0; i < componentMTSs.size(); i++)
			{
				MTS currentMTS = componentMTSs.get(i);
				outputFile.write(currentMTS.getName() + " = S0");

				ArrayList<MTS_state> states = currentMTS.getAllStates();

				for(int j = 0; j < states.size(); j++)
				{
					MTS_state currentState = states.get(j);
					if(currentMTS.isUnreachable(currentState.getName())) continue;

					ArrayList<MTS_transition> transitionsSet = currentMTS.getAllOutGoing(currentState.getName());

					outputFile.write(",");
					outputFile.newLine();
					outputFile.newLine();

					//If there are no outgoing transitions, the state has to be defined as STOP state
					//Otherwise the MTSA considers it an error state
					if(transitionsSet.isEmpty())
					{
						outputFile.write("S" + currentState.getName() + " = STOP");
						continue;
					}

					outputFile.write("S" + currentState.getName() + " = (");

					for(int k = 0; k < transitionsSet.size(); k++)
					{
						MTS_transition currentTransition = transitionsSet.get(k);
						//If this is the first transition, the line starts with (
						if(k == 0)
						{

							outputFile.write(currentTransition.getMTSAName());
						}
						//Otherwise it should be the OR logical symbol
						else
						{
							outputFile.write("		| " + currentTransition.getMTSAName());
						}

						//Maybe transitions are marked with ?
						if(currentTransition.getType().equals("maybe"))
						{
							outputFile.write("? -> S");
						}
						else
						{
							outputFile.write(" -> S");
						}
						outputFile.write((new Integer(currentTransition.getEnd())).toString());

						/*Modified by Natcha Simsiri - Nov 2
						* Only for invariant annotated models.
						* */
						if (currentTransition instanceof MTS_inv_transition){
							Event2 event = ((MTS_inv_transition)currentTransition).getEventObject();
							String invStr = String.format("\n\t\tPreCond:%s\n", event.getPreCond_str());
							outputFile.write(invStr);
                            outputFile.write(String.format("\n\t\tPostCond:%s\n", event.getPostCond_str()));
						}

//						if (this.instance!=null){
//							StringBuffer xx = new StringBuffer("\n\t\t\tPOST-STATE:\n");
//							Event2 event = ((MTS_inv_transition)currentTransition).getEventObject();
//							Scenario2.Invocation currentInvocation = this.instance.scenario.getInvocation(currentTransition.getEnd());
//							for (VarInfo var : instance.variables)
//							{
//								xx.append(String.format("\t\t\t%s = %s\n", var.name(), currentInvocation.get_post_value(var.name())));
//							}
//							outputFile.write(xx.toString());
//						}


						if(k != transitionsSet.size() - 1) outputFile.newLine();
					}

					outputFile.write(")");
				}

				outputFile.write(".");
				outputFile.newLine();
				outputFile.newLine();
				outputFile.write("||MTS_" + currentMTS.getName() + " = (" + currentMTS.getName() + ").");
				outputFile.newLine();
				outputFile.newLine();
			}
			outputFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getShortPptNameFromEventString(String eventName){
		String name = "";
//		System.out.println(eventName);
		String[] pptSplit = eventName.split("\\.");
		for(int i = 2; i < pptSplit.length; i++) name+=pptSplit[i];
		pptSplit=name.split(":::EXIT");
		if (pptSplit.length!=2) return eventName;
		name = pptSplit[0]+pptSplit[1];

		int i_op = name.indexOf("(");
		int i_cp = name.indexOf(")");
		name = name.substring(0,i_op+1).concat(name.substring(i_cp,name.length()));
		pptSplit = name.split("condition");
		name="";
		for(int i = 0; i < pptSplit.length;i++) name+=pptSplit[i];
		return name;
	}

	public static String getShortPptNameFromPptName(PptName pptName){
		return "";
	}


	public void outputToMTSADOT(ArrayList<MTS> componentsMTS, String path){
		try{
			BufferedWriter outputFile = new BufferedWriter(new FileWriter(path));
			for(int i = 0; i < componentsMTS.size(); i++){
				MTS currentMTS = componentsMTS.get(i);
				HashMap<String, String> compMap = new HashMap<String, String>();
				outputFile.write(String.format("digraph %s { ", currentMTS.getName()));
				ArrayList<MTS_state> states = currentMTS.getAllStates();

                for(int j = 0; j < states.size(); j++){
                    MTS_state cur = states.get(j);
                    if (cur.getName() != 0){
						if (!currentMTS.getAllOutGoing(cur.getName()).isEmpty() && !currentMTS.getAllIncoming(cur.getName()).isEmpty()){
							String stateStr = String.format("S%s[label=\"S%s\n%s\"];", cur.getName(), cur.getName(), cur.getVariableState());
							outputFile.newLine();
							outputFile.write(stateStr);
						}
                    }
                }

				for(int j = 0; j < states.size(); j++){
					MTS_state currentState = states.get(j);
					if (currentMTS.isUnreachable(currentState.getName())) continue;
					ArrayList<MTS_transition> transitions = currentMTS.getAllOutGoing(currentState.getName());
					for(int k = 0; k < transitions.size(); k++){
						MTS_transition currentTransition = transitions.get(k);
						String invarString = "\n";
						if (currentTransition instanceof MTS_inv_transition){
							Event2 event = ((MTS_inv_transition) currentTransition).getEventObject();
							List<String> preInvStrs = event.getPreCond_str();
                            for(int ii = 0; ii < preInvStrs.size(); ii++){
                                invarString += preInvStrs.get(ii);
                                if (ii < preInvStrs.size()-1) invarString+=",\n";
                            }
                            System.out.printf("[OUTPUT] %s : %s\n", event.getName(), invarString);
						}

						assert(currentTransition.getName()!=null);
//						System.out.println(currentTransition.getName().getSignature());
						String pptName = currentTransition.getMTSAName();
						String[] pptSplit = pptName.split("EXIT");
						pptName="";
						for(int ii = 0 ; ii < pptSplit.length;ii++) pptName+=pptSplit[ii];

						String edgeStr = String.format("S%s -> S%s",
								currentState.getName(),
								currentTransition.getEnd());
						if (!compMap.containsKey(edgeStr)) compMap.put(edgeStr, pptName+invarString);
						else{
							String prevPptName = compMap.get(edgeStr);
							prevPptName += ("\n" + pptName+invarString);
							compMap.put(edgeStr, prevPptName);
						}
					}

				}
				for (Map.Entry<String, String> entry : compMap.entrySet()){
					String edgeInfo = String.format("%s [label=\"%s\"];", entry.getKey(), entry.getValue());
					outputFile.newLine();
					outputFile.write(edgeInfo);
				}
				outputFile.newLine();
				outputFile.write("}");
				outputFile.close();
//				System.out.println(System.getProperty("user.dir"));
//				System.out.println(System.getProperty("user.dir") + "/"+path);
//				String wholePath = System.getProperty("user.dir") + "/"+path;
//				Process p = Runtime.getRuntime().exec(String.format("/usr/local/Cellar/graphviz/2.38.0/bin/dot %s -Tpng > %s.png", wholePath, wholePath));
//				BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//				String errStr = "";
//				while ((errStr = stdErr.readLine()) != null) {
//					System.out.println("[dot-png err] " + errStr);
//				}

			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
