package MTSGenerator.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import DataTypes.Component;
import DataTypes.ComponentScenarios;
import DataTypes.Event;
import DataTypes.Scenario;
import DataTypes.exceptions.AnnotationDiscrepancyException;

public class AnnotationAnalysis {
	
	public AnnotationAnalysis() {}
	
	//The method that compares the annotations between system and component perspectives
	public void Analyze(ArrayList<ComponentScenarios> compPerspectiveScenarios, ComponentScenarios systemScenarioSet, ArrayList<Component> components, HashMap<String,String> initial) throws AnnotationDiscrepancyException
	{
		ArrayList<Scenario> systemScenarios = systemScenarioSet.getScenarios();
		AnnotationDiscrepancyException raiseException = new AnnotationDiscrepancyException();
		
		//We iterate for each scenario and compare system perspective with the perspective of each component
		for(int i = 0; i < systemScenarios.size(); i++)
		{
			Scenario currentSystemScenario = systemScenarios.get(i);
			for(int j = 0; j < compPerspectiveScenarios.size(); j++)
			{
				ComponentScenarios compScenarios = compPerspectiveScenarios.get(j);
				Component currentComponent = components.get(j);
				
				Scenario currentComponentScenario = compScenarios.getScenarioByName(currentSystemScenario.getName());
				if(currentComponentScenario != null)
				{
					ArrayList<ArrayList<String>> componentAnnotations = currentComponentScenario.getAnnotations();
					ArrayList<ArrayList<String>> systemAnnotations = currentSystemScenario.getAnnotationsForComp(compScenarios.getName());
					ArrayList<Event> scenarioEvents = currentSystemScenario.getEventList(compScenarios.getName());
					ArrayList<String> requiredInterface = currentComponent.getRequiredOperationNames();
					
					//We will now compare the system-level and component-level annotations
					Set<String> varNames = initial.keySet();
					Iterator<String> nameIterator = varNames.iterator();
					ArrayList<Integer> variables = new ArrayList<Integer>();
					ArrayList<String> varNames1 = new ArrayList<String>();
					int counter = 0;
					
					//Determine indices of significant component's variables in the overall variable set
					while(nameIterator.hasNext())
					{
						String currentName = nameIterator.next();
						if(currentComponent.getVariables().contains(currentName))
						{
							variables.add(new Integer(counter));
							varNames1.add(currentName);
						}
						else if(currentComponent.getAbsoluteVariables().contains(currentName))
						{
							variables.add(new Integer(counter));
							varNames1.add(currentName);
						}
						counter++;
					}
					
					//Go through all the annotations
					for(int k = 0; k < currentComponentScenario.size(); k++)
					{	
						//For required operations both the annotation before and after the invocation should 
						//be consistent with the system-level annotation
						if(requiredInterface.contains(scenarioEvents.get(k).getName()))
						{
							ArrayList<String> annotationBefore = componentAnnotations.get(k);
							ArrayList<String> annotationAfter = componentAnnotations.get(k + 1);
							ArrayList<String> annotationSysBefore = systemAnnotations.get(2 * k);
							ArrayList<String> annotationSysAfter = systemAnnotations.get(2 * k + 1);
							for(int l = 0; l < variables.size(); l++)
							{
								if(!annotationBefore.get(l).equals(annotationSysBefore.get(variables.get(l).intValue())))
								{
									String message = "SD annotations on variable ";
									message += varNames1.get(l);
									message += " from system's and component " + currentComponent.getName() + "'s perspective differ: ";
									message += "Before invocation of " + scenarioEvents.get(k).getName() + " (positioned as " + (k + 1) + ". in component's lifeline), ";
									message += "the annotation for system's perspective is " + annotationSysBefore.get(variables.get(l).intValue());
									message += ", while it is " + annotationBefore.get(l) + " for the perspective of the component.";								
									raiseException.addMessage(message);
								}
								if(!annotationAfter.get(l).equals(annotationSysAfter.get(variables.get(l).intValue())))
								{
									String message = "SD annotations on variable ";
									message += varNames1.get(l);
									message += " from system's and component " + currentComponent.getName() + "'s perspective differ: ";
									message += "After invocation of " + scenarioEvents.get(k).getName() + " (positioned as " + (k + 1) + ". in component's lifeline), ";
									message += "the annotation for system's perspective is " + annotationSysAfter.get(variables.get(l).intValue());
									message += ", while it is " + annotationAfter.get(l) + " for the perspective of the component.";								
									raiseException.addMessage(message);
								}
							}
						}
						//The other option is when the operation is exclusively a provided operation
						else
						{
							ArrayList<String> annotationAfter = componentAnnotations.get(k + 1);
							ArrayList<String> annotationSysAfter = systemAnnotations.get(2 * k + 1);
							for(int l = 0; l < variables.size(); l++)
							{
								if(!annotationAfter.get(l).equals(annotationSysAfter.get(variables.get(l).intValue())))
								{
									String message = "SD annotations on variable ";
									message += varNames1.get(l);
									message += " from system's and component " + currentComponent.getName() + "'s perspective differ: ";
									message += "After invocation of " + scenarioEvents.get(k).getName() + " (positioned as " + (k + 1) + ". in component's lifeline), ";
									message += "the annotation for system's perspective is " + annotationSysAfter.get(variables.get(l).intValue());
									message += ", while it is " + annotationAfter.get(l) + " for the perspective of the component.";								
									raiseException.addMessage(message);
								}
							}
						}
					}
				}
			}
		}
		if(raiseException.size() != 0) throw raiseException;
	}
}
