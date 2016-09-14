package MTSGenerator.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import DataTypes.Component;
import DataTypes.ComponentScenarios;
import DataTypes.Event;
import DataTypes.Scenario;
import DataTypes.exceptions.ScenarioException;

public class ScenarioAnnotator {

	public ScenarioAnnotator() {}
	
	@SuppressWarnings("unchecked")
	public ArrayList<ComponentScenarios> annotateScenarios(ArrayList<Component> components, ArrayList<Scenario> scenarios, ArrayList<Event> events, HashMap<String, String> initial) throws ScenarioException
	{
		ArrayList<ComponentScenarios> allComponentScenarios = new ArrayList<ComponentScenarios>();
		ScenarioException algorithmException = null;
		
		//Annotate scenarios for the perspective of each component
		for(int i = 0; i < components.size(); i++)
		{
			Component currentComponent = components.get(i);
			ComponentScenarios annotatedScenarioSet = new ComponentScenarios(currentComponent.getName());
			
			ArrayList<String> variables = new ArrayList<String>();
			Set<String> varNames = initial.keySet();
			Iterator<String> nameIterator = varNames.iterator();
			
			//Determine significant variables for a component
			while(nameIterator.hasNext())
			{
				String currentName = nameIterator.next();
				if(currentComponent.getVariables().contains(currentName))
				{
					variables.add(currentName.toString());
				}
				else if(currentComponent.getAbsoluteVariables().contains(currentName))
				{
					variables.add(currentName.toString());
				}
			}
			
			//Create a copy of each scenario for perspective of a component
			for(int j = 0; j < scenarios.size(); j++)
			{
				Scenario currentScenario = scenarios.get(j);
				Scenario componentScenario = new Scenario(currentScenario.getName());
				
				//Get the invocation sequence in the scenario
				ArrayList<Event> componentScenarioEvents = currentScenario.getEventList(currentComponent.getName());
				if(componentScenarioEvents.size() == 0 || componentScenarioEvents == null) continue;
				
				componentScenario.addEvents(componentScenarioEvents);
				
				//Annotations for the scenario
				ArrayList<ArrayList<String>> scenarioAnnotations = new ArrayList<ArrayList<String>>();
				
				//The annotations before (after) the invocation
				ArrayList<ArrayList<String>> preEventCondition = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> postEventCondition = new ArrayList<ArrayList<String>>();
				
				//Creating the initial set of annotations
				for(int k = 0; k < componentScenarioEvents.size(); k++)
				{
					Event currentEvent = componentScenarioEvents.get(k);
					Event componentEventInstance = currentComponent.getEvent(currentEvent.getName());
					ArrayList<String> preconditions = componentEventInstance.getPreCond();
					ArrayList<String> postconditions = componentEventInstance.getPostCond();
					ArrayList<String> newPreAnnotation = new ArrayList<String>();
					ArrayList<String> newPostAnnotation = new ArrayList<String>();
					
					for(int l = 0; l < variables.size(); l++)
					{
						String currentVariable = variables.get(l);
						
						//When a variable has to be true
						if(preconditions.contains(currentVariable)) newPreAnnotation.add("true");
						//When a variable has to be false (marked with ! before the variable name)
						else if(preconditions.contains("!" + currentVariable)) newPreAnnotation.add("false");
						//If the operation can modify the variable then use special symbol '!'
						else if(postconditions.contains(currentVariable) || postconditions.contains("!" + currentVariable)) newPreAnnotation.add("!");
						//And use special symbol '?' otherwise
						else newPreAnnotation.add("?");
						
						//Perform similar actions for postcondition
						if(postconditions.contains(currentVariable)) newPostAnnotation.add("true");
						else if(postconditions.contains("!" + currentVariable)) newPostAnnotation.add("false");
						else if(preconditions.contains(currentVariable) || preconditions.contains("!" + currentVariable)) newPostAnnotation.add("!");
						else newPostAnnotation.add("?");						
					}
					//Initial annotations are created
					preEventCondition.add(newPreAnnotation);
					postEventCondition.add(newPostAnnotation);
				}
				
				boolean change = true;
				
				//These are the annotations we will use to check whether propagation should occur
				ArrayList<ArrayList<String>> preAnnotationInit = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> postAnnotationInit = new ArrayList<ArrayList<String>>();;
				for(int k = 0; k < preEventCondition.size(); k++)
				{
					preAnnotationInit.add((ArrayList<String>) preEventCondition.get(k).clone());
					postAnnotationInit.add((ArrayList<String>) postEventCondition.get(k).clone());
				}
				
				//While some variable value can be propagated, keep the propagation process going
				while(change)
				{
					change = false;
					
					for(int k = 0; k < postEventCondition.size() - 1; k++)
					{
						ArrayList<String> preEvent = preEventCondition.get(k + 1);
						ArrayList<String> postEvent = postEventCondition.get(k);
						
						//Unify the annotations after one and before the next invocation
						for(int l = 0; l < postEvent.size(); l++)
						{
							String unifier = postEvent.get(l);
							
							//If the annotation after invocation is undefined
							if(unifier.equals("?") || unifier.equals("!"))
							{
								if(preEvent.get(l).equals("!") || preEvent.get(l).equals("?")) continue;
								//And the one before the next invocation is defined -- propagate the value
								else
								{
									change = true;
									postEvent.set(l, preEvent.get(l));
								}
							}
							else
							{
								//If the annotation before the next invocation is undefined
								//and after the first event is defined -- propagate value
								if(preEvent.get(l).equals("?") || preEvent.get(l).equals("!"))
								{
									change = true;
									preEvent.set(l, unifier);
								}
								//If the two annotations differ -- that is an error
								else if(!preEvent.get(l).equals(unifier) && algorithmException == null) 
								{
									algorithmException = new ScenarioException("Scenario violation because the postcondition of " + componentScenarioEvents.get(k).getName() + " and the precondition of " + componentScenarioEvents.get(k + 1).getName() + " cannot be unified!", "postpre");
									algorithmException.putComponentName(currentComponent.getName());
									algorithmException.setIndex(currentScenario.getName(), k, l, variables.get(l));
									algorithmException.putComponentScenario(componentScenario);
									algorithmException.inputPrePostAnnotations(preEvent, postEvent);
									throw algorithmException;
								}
							}
						}
					}
					
					//If the variable is not modified by an operation, the annotations before and
					//after the invocation should be identical for that variable
					for(int k = 0; k < postEventCondition.size(); k++)
					{
						ArrayList<String> preEvent = preEventCondition.get(k);
						ArrayList<String> postEvent = postEventCondition.get(k);
						
						for(int l = 0; l < preEvent.size(); l++)
						{
							String unifierPre = preEvent.get(l);
							String unifierPost = postEvent.get(l);
							
							if(unifierPre.equals(unifierPost)) continue;
							else if(unifierPre.equals("!") || unifierPost.equals("!")) continue;
							//Propagate the value if the annotation before the invocation is '?'
							else if(unifierPre.equals("?")) 
							{
								change = true;
								preEvent.set(l, unifierPost);
							}
							//Propagate the value if the annotation after the invocation is '?'
							else if(unifierPost.equals("?")) 
							{
								change = true;
								postEvent.set(l, unifierPre);
							}
						}
					}
				}
				
				//Check whether an exception should be raised in case when a variable value differs
				//before and after an invocation of an operation that does not modify a variable
				for(int k = 0; k < preAnnotationInit.size(); k++)
				{
					ArrayList<String> preEventBefore = preAnnotationInit.get(k);
					ArrayList<String> postEventBefore = postAnnotationInit.get(k);
					ArrayList<String> preEventAfter = preEventCondition.get(k);
					ArrayList<String> postEventAfter = postEventCondition.get(k);
					
					for(int l = 0; l < preEventBefore.size(); l++)
					{
						//If the annotations before and after the invocation were '?',
						//and do not end up identical -- that is an error
						if(preEventBefore.get(l).equals("?") && postEventBefore.get(l).equals("?"))
						{
							if(!preEventAfter.get(l).equals(postEventAfter.get(l)))
							{
								algorithmException = new ScenarioException("Scenario violation because the precondition and the postcondition of " + componentScenarioEvents.get(k).getName() + " cannot be unified and they should be!", "prepost");
								algorithmException.putComponentName(currentComponent.getName());
								algorithmException.setIndex(currentScenario.getName(), k, l, variables.get(l));
								algorithmException.putComponentScenario(componentScenario);
								algorithmException.inputPrePostAnnotations(preEventAfter, postEventAfter);
								throw algorithmException;
							}
						}
					}
				}
				//Unite the annotations before one and after the next event (+preserve the delimiter symbols although that might not be necessary)
				if(preEventCondition.size() > 0)
				{
					scenarioAnnotations.add(preEventCondition.get(0));
					for (int k = 0; k < postEventCondition.size() - 1; k++)
					{
						ArrayList<String> currentPre = preEventCondition.get(k + 1);
						ArrayList<String> currentPost = postEventCondition.get(k);
						ArrayList<String> cummCondition = new ArrayList<String>();
						for(int l = 0; l < currentPre.size(); l++)
						{	
							if(!(currentPre.get(l).equals("?") && currentPost.get(l).equals("!"))) cummCondition.add(currentPost.get(l));
							else if (!(currentPost.get(l).equals("?") && currentPre.get(l).equals("!"))) cummCondition.add(currentPost.get(l));
							else cummCondition.add(currentPost.get(l) + currentPre.get(l));
						}
						scenarioAnnotations.add(cummCondition);
					}
					scenarioAnnotations.add(postEventCondition.get(postEventCondition.size() - 1));
				}
				//Update the components scenario set, and eventually updatye the component itself
				componentScenario.setAnnotations(scenarioAnnotations);
				annotatedScenarioSet.addScenario(componentScenario);
			}
			allComponentScenarios.add(annotatedScenarioSet);
		}
		
		//If there was an error in the annotation process, throw an exception
		if(algorithmException != null) throw algorithmException;
		
		return allComponentScenarios;
	}
	
	@SuppressWarnings("unchecked")
	public ComponentScenarios systemScenario(ArrayList<Scenario> scenarios, ArrayList<Event> events, HashMap<String, String> initial) throws ScenarioException
	{
		ScenarioException algorithmException = null;
		
		//Annotated scenarios from the system's perspective
		ComponentScenarios annotatedSystemScenarios = new ComponentScenarios("System");
			
		ArrayList<String> variables = new ArrayList<String>();
		Set<String> varNames = initial.keySet();
		Iterator<String> nameIterator = varNames.iterator();
			
		//Create an arraylist with variable names
		while(nameIterator.hasNext())
		{
			String currentName = nameIterator.next();
			variables.add(currentName.toString());
		}
		
		//Create a copy of each scenario
		for(int j = 0; j < scenarios.size(); j++)
		{
			Scenario currentScenario = scenarios.get(j);
			Scenario systemScenario = new Scenario(currentScenario.getName());
			
			//Get the invocation sequence in the scenario
			ArrayList<Event> systemScenarioEvents = currentScenario.getEventList("System");
			if(systemScenarioEvents.size() == 0 || systemScenarioEvents == null) continue;
			
			systemScenario.addEvents((ArrayList<Event>) systemScenarioEvents.clone());
			
			//Add indices of interactions pairs which is used in subsequent analysis
			for(int k = 0; k < currentScenario.size(); k++)
			{
				systemScenario.setIndex(currentScenario.getIndex(k));
			}
			
			//Annotations for the scenario
			ArrayList<ArrayList<String>> scenarioAnnotations = new ArrayList<ArrayList<String>>();
			
			//The annotations before (after) the invocation
			ArrayList<ArrayList<String>> preEventCondition = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> postEventCondition = new ArrayList<ArrayList<String>>();
			
			//Creating the initial set of annotations
			for(int k = 0; k < systemScenarioEvents.size(); k++)
			{
				Event currentEvent = systemScenarioEvents.get(k);
				ArrayList<String> preconditions = currentEvent.getPreCond();
				ArrayList<String> postconditions = currentEvent.getPostCond();
				ArrayList<String> newPreAnnotation = new ArrayList<String>();
				ArrayList<String> newPostAnnotation = new ArrayList<String>();
				
				for(int l = 0; l < variables.size(); l++)
				{
					String currentVariable = variables.get(l);
					
					//When a variable has to be true
					if(preconditions.contains(currentVariable)) newPreAnnotation.add("true");
					//When a variable has to be false (marked with ! before the variable name)
					else if(preconditions.contains("!" + currentVariable)) newPreAnnotation.add("false");
					//If the operation can modify the variable then use special symbol '!'
					else if(postconditions.contains(currentVariable) || postconditions.contains("!" + currentVariable)) newPreAnnotation.add("!");
					//And use special symbol '?' otherwise
					else newPreAnnotation.add("?");
					
					//Perform similar actions for postcondition
					if(postconditions.contains(currentVariable)) newPostAnnotation.add("true");
					else if(postconditions.contains("!" + currentVariable)) newPostAnnotation.add("false");
					else if(preconditions.contains(currentVariable) || preconditions.contains("!" + currentVariable)) newPostAnnotation.add("!");
					else newPostAnnotation.add("?");						
				}
				//Initial annotations are created
				preEventCondition.add(newPreAnnotation);
				postEventCondition.add(newPostAnnotation);
			}
			
			boolean change = true;
			
			//These are the annotations we will use to check whether propagation should occur
			ArrayList<ArrayList<String>> preAnnotationInit = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> postAnnotationInit = new ArrayList<ArrayList<String>>();;
			for(int k = 0; k < preEventCondition.size(); k++)
			{
				preAnnotationInit.add((ArrayList<String>) preEventCondition.get(k).clone());
				postAnnotationInit.add((ArrayList<String>) postEventCondition.get(k).clone());
			}
			
			//While some variable value can be propagated, keep the propagation process going
			while(change)
			{
				change = false;
				
				for(int k = 0; k < postEventCondition.size() - 1; k++)
				{
					ArrayList<String> preEvent = preEventCondition.get(k + 1);
					ArrayList<String> postEvent = postEventCondition.get(k);
					
					//Unify the annotations after one and before the next invocation
					for(int l = 0; l < postEvent.size(); l++)
					{
						String unifier = postEvent.get(l);
						
						//If the annotation after invocation is undefined
						if(unifier.equals("?") || unifier.equals("!"))
						{
							if(preEvent.get(l).equals("!") || preEvent.get(l).equals("?")) continue;
							//And the one before the next invocation is defined -- propagate the value
							else
							{
								change = true;
								postEvent.set(l, preEvent.get(l));
							}
						}
						else
						{
							//If the annotation before the next invocation is undefined
							//and after the first event is defined -- propagate value
							if(preEvent.get(l).equals("?") || preEvent.get(l).equals("!"))
							{
								change = true;
								preEvent.set(l, unifier);
							}
							//If the two annotations differ -- that is an error
							else if(!preEvent.get(l).equals(unifier) && algorithmException == null) 
							{
								algorithmException = new ScenarioException("Scenario violation because the postcondition of " + systemScenarioEvents.get(k).getName() + " and the precondition of " + systemScenarioEvents.get(k + 1).getName() + " cannot be unified!", "postpre");
								algorithmException.putComponentName("System");
								algorithmException.setIndex(currentScenario.getName(), k, l, variables.get(l));
								algorithmException.putComponentScenario(systemScenario);
								algorithmException.inputPrePostAnnotations(preEvent, postEvent);
								throw algorithmException;
							}
						}
					}
				}
				
				//If the variable is not modified by an operation, the annotations before and
				//after the invocation should be identical for that variable
				for(int k = 0; k < postEventCondition.size(); k++)
				{
					ArrayList<String> preEvent = preEventCondition.get(k);
					ArrayList<String> postEvent = postEventCondition.get(k);
					
					for(int l = 0; l < preEvent.size(); l++)
					{
						String unifierPre = preEvent.get(l);
						String unifierPost = postEvent.get(l);
						
						if(unifierPre.equals(unifierPost)) continue;
						else if(unifierPre.equals("!") || unifierPost.equals("!")) continue;
						//Propagate the value if the annotation before the invocation is '?'
						else if(unifierPre.equals("?")) 
						{
							change = true;
							preEvent.set(l, unifierPost);
						}
						//Propagate the value if the annotation after the invocation is '?'
						else if(unifierPost.equals("?")) 
						{
							change = true;
							postEvent.set(l, unifierPre);
						}
					}
				}
			}
			
			//Check whether an exception should be raised in case when a variable value differs
			//before and after an invocation of an operation that does not modify a variable
			for(int k = 0; k < preAnnotationInit.size(); k++)
			{
				ArrayList<String> preEventBefore = preAnnotationInit.get(k);
				ArrayList<String> postEventBefore = postAnnotationInit.get(k);
				ArrayList<String> preEventAfter = preEventCondition.get(k);
				ArrayList<String> postEventAfter = postEventCondition.get(k);
				
				for(int l = 0; l < preEventBefore.size(); l++)
				{
					//If the annotations before and after the invocation were '?',
					//and do not end up identical -- that is an error
					if(preEventBefore.get(l).equals("?") && postEventBefore.get(l).equals("?"))
					{
						if(!preEventAfter.get(l).equals(postEventAfter.get(l)))
						{
							algorithmException = new ScenarioException("Scenario violation because the precondition and the postcondition of " + systemScenarioEvents.get(k).getName() + " cannot be unified and they should be!", "prepost");
							algorithmException.putComponentName("System");
							algorithmException.setIndex(currentScenario.getName(), k, l, variables.get(l));
							algorithmException.putComponentScenario(systemScenario);
							algorithmException.inputPrePostAnnotations(preEventAfter, postEventAfter);
							throw algorithmException;
						}
					}
				}
			}
			//Unite the annotations before one and after the next event (+preserve the delimiter symbols although that might not be necessary)
			if(preEventCondition.size() > 0)
			{
				scenarioAnnotations.add(preEventCondition.get(0));
				for (int k = 0; k < postEventCondition.size() - 1; k++)
				{
					ArrayList<String> currentPre = preEventCondition.get(k + 1);
					ArrayList<String> currentPost = postEventCondition.get(k);
					ArrayList<String> cummCondition = new ArrayList<String>();
					for(int l = 0; l < currentPre.size(); l++)
					{	
						if(!(currentPre.get(l).equals("?") && currentPost.get(l).equals("!"))) cummCondition.add(currentPost.get(l));
						else if (!(currentPost.get(l).equals("?") && currentPre.get(l).equals("!"))) cummCondition.add(currentPost.get(l));
						else cummCondition.add(currentPost.get(l) + currentPre.get(l));
					}
					scenarioAnnotations.add(cummCondition);
				}
				scenarioAnnotations.add(postEventCondition.get(postEventCondition.size() - 1));
			}
			//Update the components scenario set, and eventually update the component itself
			systemScenario.setAnnotations(scenarioAnnotations);
			annotatedSystemScenarios.addScenario(systemScenario);
		}
		
		//If there was an error in the annotation process, throw an exception
		if(algorithmException != null) throw algorithmException;
		
		return annotatedSystemScenarios;
	}
}