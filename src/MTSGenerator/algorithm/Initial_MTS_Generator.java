 package MTSGenerator.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import DataTypes.Component;
import DataTypes.Event;
import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;

public class Initial_MTS_Generator {
	
	public Initial_MTS_Generator() {}

	@SuppressWarnings("unchecked")
	public ArrayList<MTS> generateInitialMTS(ArrayList<Event> events, ArrayList<Component> components, HashMap<String,String> initial)
	{
		ArrayList<MTS> initialMTSs = new ArrayList<MTS>();
		
		//Create an initial MTS for each component
		for(int i = 0; i < components.size(); i++)
		{
			Component currentComponent = components.get(i);
			ArrayList<String> variables = new ArrayList<String>();
			Set<String> varNames = initial.keySet();
			Iterator<String> nameIterator = varNames.iterator();
			
			//Obtain the set of all significant variables for a component
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
			
			//Determine the initial values of the significant variables
			ArrayList<String> initial_values = new ArrayList<String>();
			for(int j = 0; j < variables.size(); j++)
			{
				initial_values.add(initial.get(variables.get(j)));
			}
			
			//Create an MTS with the initial state corresponding to the initial values
			//of the component's significant variables
			MTS componentMTS = new MTS(currentComponent.getName(), variables, initial_values);
			MTS_state initialState = componentMTS.getInitialState();
			
			//States for which we have to build outgoing transitions
			ArrayList<MTS_state> processingStates = new ArrayList<MTS_state>();
			processingStates.add(initialState);
			
			//Iterate while there are states that do not have outgoing transitions
			while(processingStates.size() > 0)
			{
				ArrayList<MTS_state> newProcessingStates = new ArrayList<MTS_state>();
				
				for(int j = 0; j < processingStates.size(); j++)
				{
					MTS_state currentState = processingStates.get(j);
					ArrayList<Event> componentEvents = currentComponent.getEvents();
					
					//Check whether an operation invocation can happen and to which states it can lead
					for(int k = 0; k < componentEvents.size(); k++)
					{
						Event currentEvent = componentEvents.get(k);
						ArrayList<String> preconditions = currentEvent.getPreCond();
						ArrayList<String> postconditions = currentEvent.getPostCond();
						ArrayList<String> state_assignment = currentState.getVariableState();
						boolean satisfied_precondition = true;
						
						//Check whether the precondition is violated in the current state
						for(int l = 0; l < preconditions.size(); l++)
						{
							String currentCondition = preconditions.get(l);
							int index;
							
							//Find the index in the state vector for the current precondition expression
							if(currentCondition.charAt(0) == '!')
							{
								index = componentMTS.getVariableIndex(currentCondition.substring(1));
							}
							else index = componentMTS.getVariableIndex(currentCondition);
							
							//Check whether the precondition says false, and the variable in the state is true
							if(currentCondition.charAt(0) == '!')
							{
								if(state_assignment.get(index).equals("true"))
								{
									satisfied_precondition = false;
									break;
								}
							}
							//Check whether the precondition says true, and the variable in the state is false
							else if(state_assignment.get(index).equals("false")) 
							{
								satisfied_precondition = false;
								break;
							}
						}
						
						//Create next states if teh preconditions is satisfied
						if(satisfied_precondition)
						{
							ArrayList<String> nextState = new ArrayList<String>();
							for(int l = 0; l < variables.size(); l++)
							{
								//If the postcondition says that a variables has to be true
								if(postconditions.contains(variables.get(l)))
								{
									nextState.add("true");
								}
								//If the postcondition says that the variable has to be false
								else if(postconditions.contains("!"+variables.get(l)))
								{
									nextState.add("false");
								}
								//If the precondition imposes a variable value and the postconditions says nothing
								//we interpret the case as if it was explicitly stated (true OR false)
								else if(preconditions.contains(variables.get(l)) || preconditions.contains("!"+variables.get(l)))
								{
									nextState.add("?");
								}
								//Finally, if the operation does not modify the variable value, leave it the same
								else nextState.add(state_assignment.get(l));
							}
							
							//Now we need to create all possible value combinations for the variables that are not determined
							int counter_specials = 0;
							for(int l = 0; l < variables.size(); l++)
							{
								if(nextState.get(l).equals("?")) counter_specials++;
							}
							
							for(int l = 0; l < Math.pow(2, counter_specials); l++)
							{								
								ArrayList<String> transitionState = (ArrayList<String>) nextState.clone();
								int counter = 0;
								int currentIndex = 0;
								while(counter < counter_specials)
								{
									if(nextState.get(currentIndex).equals("?"))
									{
										if((l / Math.pow(2, counter)) % 2 == 0) transitionState.set(currentIndex, "true");
										else transitionState.set(currentIndex, "false");
										currentIndex++;
										counter++;
									}
									else currentIndex++;
								}
								
								//If there is no such state in the MTS state set then create it and add it
								//along with the new transition
								ArrayList<Integer> correspondingStates = componentMTS.getMTS_states(transitionState);
								if(correspondingStates == null || correspondingStates.size() == 0)
								{
									int currentMTS_size = componentMTS.getStateSize();
									MTS_state newState = new MTS_state(currentMTS_size, transitionState);
									componentMTS.addMTSState(newState);
									componentMTS.addMTSTransition(new MTS_transition(currentEvent.getName(), currentState.getName(), newState.getName(), "maybe"));
									newProcessingStates.add(newState);
								}
								//Otherwise, just add the new transition
								else
								{
									int destState = correspondingStates.get(0).intValue();
									componentMTS.addMTSTransition(new MTS_transition(currentEvent.getName(), currentState.getName(), destState, "maybe"));
								}
							}
						}
					}
				}				
				//Next, we process the newly created states
				processingStates = newProcessingStates;
			}
			
			initialMTSs.add(componentMTS);
		}		
		return initialMTSs;
	}
}