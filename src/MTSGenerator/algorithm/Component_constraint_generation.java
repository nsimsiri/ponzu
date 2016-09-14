package MTSGenerator.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import DataTypes.Component;
import DataTypes.Event;
import DataTypes.Scenario;

public class Component_constraint_generation {
	
	public Component_constraint_generation() {}
	
	public boolean generate_complevel_constraints(ArrayList<Event> events, ArrayList<Scenario> scenarios, ArrayList<Component> components, HashMap<String, String> variable_names)
	{
		String comp_names[] = new String[components.size()];	
		
		//Get component names
		for(int k = 0; k < components.size(); k++)
		{
			comp_names[k] = components.get(k).getName();					
		}
		
		//For all scenarios, add operations with outgoing arrows to component's required interfaces
		for(int i = 0; i < scenarios.size(); i++)
		{
			Scenario sc = scenarios.get(i);
			for(int j = 0; j < sc.size(); j++)
			{
				Event nextEvent = sc.getEvent(j);
				int index = sc.getIndex(j);
							
				String out = null;
				String in = null;
				
				//Get the name of the component with an outgoing event nextEvent
				out = nextEvent.getInteractionOut(index);
				in = nextEvent.getInteractionIn(index);
				
				//Add the event to component's required interface				
				for(int k = 0; k < comp_names.length; k++)
				{
					if(out.equals(comp_names[k])) 
					{
						Component updating = components.get(k);
						Event e_comp = updating.getEvent(nextEvent.getName());
						
						//If the event is already in component's provided interface then this is a self call
						if(e_comp != null && e_comp.getType().equals("in"))
						{
							e_comp.setType("inout");
						}
						
						//Otherwise we have a new member of the required interface
						else if(e_comp == null)
						{
							Event newEvent = new Event(nextEvent.getName(), "out");
							updating.addEventRequired(newEvent);
						}
					}
					if(in.equals(comp_names[k])) 
					{
						Component updating = components.get(k);
						Event e_comp = updating.getEvent(nextEvent.getName());
						
						//If the event is already in component's required interface then this is a self call
						if(e_comp != null && e_comp.getType().equals("out"))
						{
							e_comp.setType("inout");
						}
						
						//Otherwise we have a new member of the provided interface
						else if(e_comp == null)
						{
							Event newEvent = new Event(nextEvent.getName(), "in");
							updating.addEventProvided(newEvent);
						}
					}
				}
			}
		}
		
		//Get significant variables of components from the preconditions of required interface events
		for(int i = 0; i < components.size(); i++)
		{
			Component currComponent = components.get(i);
			for(int j = 0; j < events.size(); j++)
			{
				Event currEvent = events.get(j);
				Event componentEvent = currComponent.getEvent(currEvent.getName());
				
				//Check whether this is an outgoing event for this component
				if(componentEvent != null && componentEvent.getType().equals("in") == false)
				{
					ArrayList<String> variables = currEvent.getPreCond();
					
					//All the variables from the precondition are significant state variables
					for(int k = 0; k < variables.size(); k++)
					{
						String var = variables.get(k);
						
						//The sign ! stands for negation (each constraint in this version is defined on single variables) 
						if(var.charAt(0) == '!')
						{
							currComponent.addVariable(var.substring(1));
						}
						else currComponent.addVariable(var.toString());
					}
				}
			}
		}
		
		Set<String> var_names = variable_names.keySet();
		Iterator<String> iterator_names = var_names.iterator();
		
		//Iterate through all the variables
		for(int i = 0; i < var_names.size(); i++)
		{
			boolean breaked = false;
			
			//If a variable only changes its value in some component's scope then 
			//that component will be stored in these strings 
			String absolute_first = null;
			String absolute_second = null;
			
			String currentVar = iterator_names.next();
			
			//Go through all the events and see if the current variable is modified by them
			for(int j = 0; j < events.size(); j++)
			{
				Event currentEvent = events.get(j);
				ArrayList<String> preCond = currentEvent.getPreCond();
				ArrayList<String> postCond = currentEvent.getPostCond();
				
				for(int k = 0; k < preCond.size(); k++)
				{
					if(preCond.get(k).equals(currentVar) || preCond.get(k).substring(1).equals(currentVar))
					{
						//If there is a precondition on the current variable, and there is no explicit postcondition
						//that means that the variable can be modified (it can be either true or false after the event)
						if(!postCond.contains(preCond.get(k)))
						{
							for(int l = 0; l < currentEvent.getPairNumber(); l++)
							{
								String outComp = currentEvent.getInteractionOut(l);
								String inComp = currentEvent.getInteractionIn(l);
								
								//If this is the first time that this variable is modified after an event
								//set both the components as candidate components for which this variable 
								//is completely inside their scope
								if(absolute_first == null && absolute_second == null)
								{
									absolute_first = outComp;
									absolute_second = inComp;
								}
								//If there is still only one candidate component that has this variable inside its scope
								else if(absolute_second == null)
								{
									//Check if the current event changes things
									if(!outComp.equals(absolute_first) && !inComp.equals(absolute_first))
									{
										absolute_first = null;
										breaked = true;
										break;
									}								
								}
								//If there are two candidate components still, check if the current event changes things
								else
								{
									if(!outComp.equals(absolute_second) && !inComp.equals(absolute_second))
									{
										absolute_second = null;
									}
									if(!outComp.equals(absolute_first) && !inComp.equals(absolute_first))
									{
										absolute_first = null;
									}
									
									//This means that the variable is not completely inside scope of any component
									if(absolute_first == null && absolute_second == null)
									{
										breaked = true;
										break;
									}
									else if(absolute_first == null)
									{
										absolute_first = absolute_second;
										absolute_second = null;
									}
								}
							}
							
							if(breaked) break;
						}
					}
				}
				
				if(breaked) break;
				
				//Here we go through the postconditions
				for(int k = 0; k < postCond.size(); k++)
				{
					if(postCond.get(k).equals(currentVar) || postCond.get(k).substring(1).equals(currentVar))
					{
						//If there isn't an identical precondition, it means that the variable is modified
						if(!preCond.contains(postCond.get(k)))
						{
							for(int l = 0; l < currentEvent.getPairNumber(); l++)
							{
								String outComp = currentEvent.getInteractionOut(l);
								String inComp = currentEvent.getInteractionIn(l);
								if(absolute_first == null && absolute_second == null)
								{
									absolute_first = outComp;
									absolute_second = inComp;
								}
								else if(absolute_second == null)
								{
									if(!outComp.equals(absolute_first) && !inComp.equals(absolute_first))
									{
										absolute_first = null;
										breaked = true;
										break;
									}								
								}
								else
								{
									if(!outComp.equals(absolute_second) && !inComp.equals(absolute_second))
									{
										absolute_second = null;
									}
									if(!outComp.equals(absolute_first) && !inComp.equals(absolute_first))
									{
										absolute_first = null;
									}
									
									if(absolute_first == null && absolute_second == null)
									{
										breaked = true;
										break;
									}
									else if(absolute_first == null)
									{
										absolute_first = absolute_second;
										absolute_second = null;
									}
								}
							}
							
							if(breaked) break;
						}
					}
					
					if(breaked) break;
				}
			}
			
			//At this point we have extracted which components, if any, have the current variable 
			//completely inside their scope
			
			//If there does not exist a component which has the global knowledge of the variable, continue
			if(breaked) continue;
			
			for(int j = 0; j < components.size(); j++)
			{
				Component currentComponent = components.get(j);
				
				//If the current variable is completely inside the scope of current component
				if((absolute_first != null && absolute_first.equals(currentComponent.getName())) || (absolute_second != null && absolute_second.equals(currentComponent.getName())))
				{
					currentComponent.addAbsoluteVariable(currentVar);
				}
			}
		}
		
		for(int i = 0; i < components.size(); i++)
		{
			Component currentComponent = components.get(i);
			
			//Get different types of variables and iterate through all of the component's events
			ArrayList<String> componentVars = currentComponent.getVariables();
			ArrayList<String> componentAbsVars = currentComponent.getAbsoluteVariables();
			ArrayList<Event> component_Events = currentComponent.getEvents();
			for(int k = 0; k < component_Events.size(); k++)
			{
				Event currentEvent = component_Events.get(k);
				for(int l = 0; l < events.size(); l++)
				{
					Event extractingConstr = events.get(l);
					
					//We will extract the constraints that are relevant for currentComponent
					if(currentEvent.getName().equals(extractingConstr.getName()))
					{
						ArrayList<String> preConditions = extractingConstr.getPreCond();
						ArrayList<String> postConditions = extractingConstr.getPostCond();
						for(int m = 0; m < preConditions.size(); m++)
						{
							String constraint = preConditions.get(m);
							if(currentEvent.getType().equals("out") || currentEvent.getType().equals("inout"))
							{
								//For outgoing operations, get all the preconditions defined on significant variables
								for(int n = 0; n < componentVars.size(); n++)
								{
									if(constraint.equals(componentVars.get(n)) || constraint.substring(1).equals(componentVars.get(n)))
									{
										currentEvent.addPre(constraint);
									}
								}
							}
							
							for(int n = 0; n < componentAbsVars.size(); n++)
							{
								//For incoming operations, get all the constraints defined on absolute variables
								if(constraint.equals(componentAbsVars.get(n)) || constraint.substring(1).equals(componentAbsVars.get(n)))
								{
									currentEvent.addPre(constraint);
								}
							}
							
						}
						
						//For the postconditions, get all the constraints defined on relevant variables (significant + absolute)
						for(int m = 0; m < postConditions.size(); m++)
						{
							String constraint = postConditions.get(m);
							for(int n = 0; n < componentVars.size(); n++)
							{
								if(constraint.equals(componentVars.get(n)) || constraint.substring(1).equals(componentVars.get(n)))
								{
									currentEvent.addPost(constraint);
								}
							}
							
							
							for(int n = 0; n < componentAbsVars.size(); n++)
							{
								if(constraint.equals(componentAbsVars.get(n)) || constraint.substring(1).equals(componentAbsVars.get(n)))
								{
									currentEvent.addPost(constraint);
								}
							}
							
						}
					}
				}
			}
			
			//The following block removes all the variables which are not contained in any precondition
			//Although these were thrown out this does not have to be done (and in some situations
			//we will lose some potentially useful information if we remove them). However, if the state 
			//space is too big, then this step is still advisable.
/*			ArrayList<String> overallVars = (ArrayList<String>) currentComponent.getVariables().clone();
			
			for(int k = 0; k < componentAbsVars.size(); k++)
			{
				if(!overallVars.contains(componentAbsVars.get(k)))
				{
					overallVars.add(componentAbsVars.get(k).toString());
				}
			}
			
			for(int k = 0; k < component_Events.size(); k++)
			{
				ArrayList<String> preconstraints = component_Events.get(k).getPreCond();
				
				for(int l = 0; l < preconstraints.size(); l++)
				{
					String preconstr = preconstraints.get(l).toString();
					if(overallVars.contains(preconstr))
					{
						overallVars.remove(preconstr);
					}
					else if(preconstr.charAt(0) == '!' && overallVars.contains(preconstr.substring(1)))
					{
						overallVars.remove(preconstr.substring(1));
					}
					if(overallVars.size() == 0) break;
				}
				if(overallVars.size() == 0) break;
			}
			
			if(overallVars.size() != 0)
			{
				for(int k = 0; k < component_Events.size(); k++)
				{
					Event ev = component_Events.get(k);
					for(int l = 0; l < overallVars.size(); l++)
					{								
						ev.removePost(overallVars.get(l));
						ev.removePost("!" + overallVars.get(l));
						currentComponent.removeAnyVariable(overallVars.get(l));
					}
				}
			}*/
		}
		return true;
	}
}