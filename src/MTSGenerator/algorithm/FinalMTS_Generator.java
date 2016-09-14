package MTSGenerator.algorithm;

import java.util.ArrayList;

import DataTypes.ComponentScenarios;
import DataTypes.Event;
import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.Scenario;

public class FinalMTS_Generator {
	
	public FinalMTS_Generator() {}
	
	@SuppressWarnings("unchecked")
	public void generateMTS(ArrayList<MTS> componentMTSs, ArrayList<ComponentScenarios> scenarios)
	{
		//Refine the initial MTS for every component
		for(int i = 0; i < componentMTSs.size(); i++)
		{
			MTS currentMTS = componentMTSs.get(i);
			ComponentScenarios currentComponentScenarios = scenarios.get(i);
			
			//Iterate through all scenarios (which are annotated from the current component's perspective)
			for(int j = 0; j < currentComponentScenarios.size(); j++)
			{
				Scenario currentScenario = currentComponentScenarios.getScenario(j);
				ArrayList<Event> scenarioEvents = currentScenario.getEventList("All");
				ArrayList<ArrayList<String>> scenarioAnnotations = currentScenario.getAnnotations();
				
				//Get the first annotation and then iterate through all other annotations
				ArrayList<String> preAnnotation = scenarioAnnotations.get(0);
				//We obtain all the launching states -- the state that satisfy the first annotation
				ArrayList<MTS_state> conditionStates = currentMTS.getStates(preAnnotation);
				
				for(int k = 0; k < scenarioEvents.size(); k++)
				{
					//For the next event in the scenario refine some of the outgoing transitions
					//from the current set of states which satisfy the precondition. The next state has
					//to satisfy the postcondition (captured in postAnnotation). The nextStates set will
					//have the states in which we end up after traversing the new required transitions.
					Event currentEvent = scenarioEvents.get(k);
					ArrayList<String> postAnnotation = scenarioAnnotations.get(k + 1);
					ArrayList<MTS_state> nextStates = new ArrayList<MTS_state>();
					
					//Iterate through all the starting states
					for(int l = 0; l < conditionStates.size(); l++)
					{
						MTS_state outgoingState = conditionStates.get(l);
						ArrayList<String> concretePreAnnotation = outgoingState.getVariableState();
						ArrayList<String> concretePostAnnotation = (ArrayList<String>) postAnnotation.clone();
						
						//For the variables which are not modified by the current operation, their values in 
						//the following state stay the same (?! signals that that variable does not have to 
						//have the same value before and after the next invocation in the SD).
						for(int m = 0; m < concretePreAnnotation.size(); m++)
						{
							if(concretePostAnnotation.get(m).charAt(0) == '?')
							{
								if(preAnnotation.get(m).equals("?") || preAnnotation.get(m).equals("?!"))
								{
									concretePostAnnotation.set(m, concretePreAnnotation.get(m));
								}
							}
						}
						
						//Find MTS states which have the variable values which have to hold, and an incoming transition on currentEvent from the outgoingState 
						ArrayList<MTS_state> incomingStateList = currentMTS.getStatesWithEvent_Condition(outgoingState, currentEvent.getName(), postAnnotation);
						
						//Iterate through all the identified states 
						for(int m = 0; m < incomingStateList.size(); m++)
						{
							MTS_state destState = incomingStateList.get(m);
							
							//If the transitions to destState is required, no refinement need to be performed
							if(currentMTS.isRequired(outgoingState.getName(), destState.getName(), currentEvent.getName()))
							{
								if(!nextStates.contains(destState)) nextStates.add(destState);
								continue;
							}
							else
							{
								//If the currentEvent is the only incoming event into the state, then we can 
								//directly refine the transition to required
								if(currentMTS.isExclusiveForEvent(destState.getName(), currentEvent.getName()))
								{
									//If this is the launching state for SD and there are other states with the same value
									//assignment, do not refine the transition to required because that would introduce an
									//unnecessary self-looping transition
									if(k == 0 && outgoingState.getName() == destState.getName() && currentMTS.getSizeSimilar(destState.getVariableState()) > 1) continue;
									//Otherwise just change the transition to required
									else
									{
										currentMTS.changeToRequired(outgoingState.getName(), destState.getName(), currentEvent.getName());
										if(!nextStates.contains(destState)) nextStates.add(destState);
										continue;
									}
								}
								//Refine the destination state and the transition
								else
								{
									MTS_state refineState = currentMTS.refineState(outgoingState.getName(), destState, currentEvent.getName());
									nextStates.add(refineState);
								}
							}
						}						
					}
					//Prepare for the next invocation in the SD
					conditionStates.clear();
					conditionStates = nextStates;
					preAnnotation = postAnnotation;
				}
			}
		}
		return;
	}
}
