package DataTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class MTS {
	
	//The state set
	private ArrayList<MTS_state> states = new ArrayList<MTS_state>();
	
	private HashMap<Integer,MTS_state> statesByName = new HashMap<Integer, MTS_state>();
	
	//Transition set
	private ArrayList<MTS_transition> transitions = new ArrayList<MTS_transition>();
	
	private HashMap<Integer, ArrayList<MTS_transition>> outgoingByState = new HashMap<Integer, ArrayList<MTS_transition>>();
	private HashMap<Integer, ArrayList<MTS_transition>> incomingByState = new HashMap<Integer, ArrayList<MTS_transition>>();
	
	//Component's significant variables
	private ArrayList<String> varNames = new ArrayList<String>();
	private String name;
	
	//Set of unreachable states; this may occur when states are merged
	Set<Integer> unreachable_states = new LinkedHashSet<Integer>();
	
	//Set of states that have been refined and should not be refined any more
	Set<Integer> refined_states = new LinkedHashSet<Integer>();
	
	@SuppressWarnings("unchecked")
	//Initialize the MTS with the initial state corresponding to values in initial
	public MTS(String name, ArrayList<String> varNames, ArrayList<String> initial) 
	{
		addMTSState(new MTS_state(0, initial));
		if(varNames != null)
		{
			this.varNames = (ArrayList<String>) varNames.clone();	
		}
		this.name = name;
	}
	
	//Return the name
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	//Add a new MTS state
	public boolean addMTSState(MTS_state newState)
	{
		if(states.contains(newState)) return false;
		else {
			states.add(newState);
			statesByName.put(newState.getName(), newState);
		}
		return true;
	}
	
	public boolean unsafeAddMTSState(MTS_state newState)
	{
		states.add(newState);
		statesByName.put(newState.getName(), newState);
		return true;
	}
	
	//Create a new transition
	public boolean addMTSTransition(MTS_transition newTransition)
	{
		if(transitions.contains(newTransition)) return false;
		else {
			transitions.add(newTransition);
			
			if(outgoingByState.containsKey(newTransition.getStart())) {
				(outgoingByState.get(newTransition.getStart())).add(newTransition);
			} else {
				ArrayList<MTS_transition> newOutgoingSet = new ArrayList<MTS_transition>();
				newOutgoingSet.add(newTransition);
				outgoingByState.put(newTransition.getStart(), newOutgoingSet);
			}
			
			if(incomingByState.containsKey(newTransition.getEnd())) {
				(incomingByState.get(newTransition.getEnd())).add(newTransition);
			} else {
				ArrayList<MTS_transition> newIncomingSet = new ArrayList<MTS_transition>();
				newIncomingSet.add(newTransition);
				incomingByState.put(newTransition.getEnd(), newIncomingSet);
			}
		}
		return true;
	}
	
	public boolean unsafeAddMTSTransition(MTS_transition newTransition)
	{
		transitions.add(newTransition);

		if(outgoingByState.containsKey(newTransition.getStart())) {
			(outgoingByState.get(newTransition.getStart())).add(newTransition);
		} else {
			ArrayList<MTS_transition> newOutgoingSet = new ArrayList<MTS_transition>();
			newOutgoingSet.add(newTransition);
			outgoingByState.put(newTransition.getStart(), newOutgoingSet);
		}
		
		if(incomingByState.containsKey(newTransition.getEnd())) {
			(incomingByState.get(newTransition.getEnd())).add(newTransition);
		} else {
			ArrayList<MTS_transition> newIncomingSet = new ArrayList<MTS_transition>();
			newIncomingSet.add(newTransition);
			incomingByState.put(newTransition.getEnd(), newIncomingSet);
		}
		
		if(outgoingByState.size() != incomingByState.size()) {
			int weird = 0;
		}
		
		return true;
	}
	
	public MTS_state getInitialState()
	{
		return states.get(0);
	}
	
	public int getStateSize()
	{
		int max = -1;
		for(MTS_state state : states)
		{
			if(state.getName() > max) max = state.getName();
		}
		return max + 1;
	}
	
	//Get indexes of MTS states that do not conflict with the truth assignments from variable values
	public ArrayList<Integer> getMTS_states(ArrayList<String> values)
	{
		ArrayList<Integer> returnedStates = new ArrayList<Integer>();
		for(int i = 0; i < states.size(); i++)
		{
			ArrayList<String> state_values = states.get(i).getVariableState();
			if(state_values.equals(values)) returnedStates.add(new Integer(states.get(i).getName()));
			else if (state_values == null && values == null) returnedStates.add(new Integer(states.get(i).getName()));
		}
		return returnedStates;
	}
	
	//Returns the index of a particular variable in the vectors corresponding to states
	public int getVariableIndex(String varName)
	{
		for(int i = 0; i < varNames.size(); i++)
		{
			if(varNames.get(i).equals(varName)) return i;
		}
		return -1;
	}
	
	@SuppressWarnings("unchecked")
	//Returns the significant variables
	public ArrayList<String> getVariableNames()
	{
		return (ArrayList<String>) varNames.clone();
	}
	
	//Get all MTS states
	public ArrayList<MTS_state> getAllStates()
	{
		return states;
	}
	
	public ArrayList<MTS_transition> getAllTransitions()
	{
		return transitions;
	}
	
	//Get the actual MTS states that do not conflict with the value assignment
	public ArrayList<MTS_state> getStates(ArrayList<String> assignment)
	{
		ArrayList<MTS_state> returnStates = new ArrayList<MTS_state>();
		for(int i = 0; i < states.size(); i++)
		{
			if(correspondingConditions(states.get(i), assignment)) returnStates.add(states.get(i));
		}		
		return returnStates;
	}
	
	//Check whether the state's vector conflicts the value assignment
	private boolean correspondingConditions(MTS_state state, ArrayList<String> assignment)
	{
		for(int i = 0; i < assignment.size(); i++)
		{
			if(!state.getVariableState().get(i).equals(assignment.get(i))) 
			{
				if(!(assignment.get(i).charAt(0) == '?' || assignment.get(i).charAt(0) == '!')) return false;
			}
		}
		return true;
	}
	
	//Find MTS states which have the variable values consistent with stateCondition
	//and an incoming transition on currentEvent from the outgoingState
	public ArrayList<MTS_state> getStatesWithEvent_Condition(MTS_state source, String eventName, ArrayList<String> stateConditions)
	{
		ArrayList<MTS_state> destStates =  new ArrayList<MTS_state>();
		
		//Check that the state satisfies the conditions stateConditions
		for(int i = 0; i < states.size(); i++)
		{
			if(correspondingConditions(states.get(i), stateConditions)) destStates.add(states.get(i));
		}
		
		int stateSize = destStates.size();
		
		//Check whether there exists an incoming transition into previously selected states
		for(int i = stateSize - 1; i >= 0; i--)
		{
			if(!transitionExists(source.getName(), destStates.get(i).getName(), eventName))
			{
				destStates.remove(i);
			}
		}
		
		return destStates;
	}
	
	//Does there exist a transition source -> destination on eventName
	private boolean transitionExists(int source, int destination, String eventName)
	{
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition currentTransition = transitions.get(i);
			if(currentTransition.getStart() == source && currentTransition.getEnd() == destination && currentTransition.getEvent().equals(eventName))
			{
				return true;
			}
		}
		return false;
	}
	
	//Checks whether the transition source -> dest on eventName is required
	public boolean isRequired(int source, int dest, String eventName)
	{
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition current = transitions.get(i);
			if(current.getStart() == source && current.getEnd() == dest && current.getEvent().equals(eventName) && current.getType().equals("required")) return true;
		}		
		return false;
	}
	
	//Check whether the destination state has all the incoming transitions defined on eventName
	public boolean isExclusiveForEvent(int dest, String eventName)
	{
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition current = transitions.get(i);
			if(current.getEnd() == dest && !current.getEvent().equals(eventName)) return false;
		}		
		return true;
	}
	
	//Get the number of states which have the same variable assignment
	public int getSizeSimilar(ArrayList<String> assignment)
	{
		int number = 0;;
		for(int i = 0; i < states.size(); i++)
		{
			if(correspondingConditions(states.get(i), assignment)) number++;
		}		
		return number;
	}
	
	//Changes the potential transition source -> dest to required
	public void changeToRequired(int source, int dest, String eventName)
	{
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition current = transitions.get(i);
			if(current.getStart() == source && current.getEnd() == dest && current.getEvent().equals(eventName)) 
			{
				current.changeType("required");
				return;
			}
		}	
	}
	
	//Refine the destination state and make the transition source -> dest on eventName required
	public MTS_state refineState(int source, MTS_state dest, String eventName)
	{
		//Create a new state with the key which is set to the next in the ordering
		MTS_state refinedState = null;
		int newStateName = getStateSize();
		int oldStateName = dest.getName();
		
		refinedState = new MTS_state(newStateName, dest.getVariableState());
		
		//Copy all the outgoing transitions in the new state
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition currentTransition = transitions.get(i);
			int sourceState = currentTransition.getStart();
			int endState = currentTransition.getEnd();
			if(sourceState == oldStateName)
			{
				MTS_transition newTransition = new MTS_transition(currentTransition.getName(), newStateName, endState, currentTransition.getType());
				addMTSTransition(newTransition);
			}
		}
			
		//Now redirect all the transitions that go into the state that is being 
		//refined on eventName
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition currentTransition = transitions.get(i);
			int sourceState = currentTransition.getStart();
			int endState = currentTransition.getEnd();
			if(endState == oldStateName)
			{
				//Refine the traversed transition into a required transition
				if(sourceState == source && currentTransition.getEvent().equals(eventName))
				{
					currentTransition.changeType("required");
					changeMTSDest(currentTransition, newStateName);
				}
				else if(currentTransition.getEvent().equals(eventName))
				{
					changeMTSDest(currentTransition, newStateName);
				}
			}
		}
		
		states.add(refinedState);
		statesByName.put(refinedState.getName(), refinedState);
		return refinedState;
	}
	
	//Refine the destination state and make the transition source -> dest on eventName required. Refine defines whether to really split the state
	public MTS_state refineAutomataInference(int source, int dest, String eventName)
	{
		//Create a new state with the key which is set to the next in the ordering
		MTS_state refinedState = null;
		int newStateName = getStateSize();
		
		MTS_state oldState = null;
		int oldStateName = -1;
		for(MTS_state state : states)
		{
			if(state.getName() == dest) 
			{
				oldState = state;
				oldStateName = state.getName();
				break;
			}
		}
		
		refinedState = new MTS_state(newStateName, oldState.getVariableState());
		
		//Copy all the outgoing transitions in the new state
		if(outgoingByState.containsKey(oldStateName)) {
			for(MTS_transition currentTransition : outgoingByState.get(oldStateName)) {
				int sourceState = currentTransition.getStart();
				int endState = currentTransition.getEnd();
				if(sourceState != endState)
				{
					MTS_transition newTransition = new MTS_transition(currentTransition.getName(), newStateName, endState, currentTransition.getType());
					addMTSTransition(newTransition);
				}
				else if(sourceState == endState)
				{
					MTS_transition newTransition = new MTS_transition(currentTransition.getName(), newStateName, newStateName, currentTransition.getType());
					addMTSTransition(newTransition);
				}
			}
		}
			
		//Now redirect all the transitions that go into the state that is being 
		//refined on eventName
		ArrayList<MTS_transition> incomingTemp = (ArrayList<MTS_transition>) (incomingByState.get(oldStateName)).clone();
		
		for(MTS_transition currentTransition : incomingTemp)
		{
			int sourceState = currentTransition.getStart();
			int endState = currentTransition.getEnd();

			//Refine the traversed transition into a required transition
			if(sourceState == source && currentTransition.getEvent().equals(eventName))
			{
				currentTransition.changeType("required");
				changeMTSDest(currentTransition, newStateName);
				currentTransition.increaseOccurrences();			
			}
		}

		ArrayList<MTS_transition> remainingIncoming = incomingByState.get(oldStateName);
		int trueIncoming = 0;
		for(MTS_transition tran : remainingIncoming)
		{
			if(tran.getStart() != tran.getEnd()) trueIncoming++;
		}
		if(trueIncoming == 0)
		{
			unreachable_states.add(oldStateName);
		}
		
		addMTSState(refinedState);
		return refinedState;
	}
	
	public void requireTransitionInference(int source, int dest, String event)
	{
//		changeToRequired(source, dest, event);
		for(MTS_transition tran : outgoingByState.get(source))
		{
			if(tran.getEnd() == dest && tran.getEvent().equals(event))
			{
				if(tran.getType().equals("maybe")) tran.changeType("required");
				tran.increaseOccurrences();
				break;
			}
		}
	}
	
	//Return valid values combinations for variables in varNames
	public ArrayList<ArrayList<String>> getVarValueCombinations(ArrayList<String> varNames)
	{
		ArrayList<ArrayList<String>> comboCollection = new ArrayList<ArrayList<String>>();
		
		for(int i = 0; i < states.size(); i++)
		{
			ArrayList<String> currentStateValues = states.get(i).getVariableState();			
			ArrayList<String> valueCombination = new ArrayList<String>();
			
			for (int j = 0; j < varNames.size(); j++)
			{
				String currentVar = varNames.get(j);
				valueCombination.add(currentStateValues.get(this.varNames.indexOf(currentVar)));
			}
			
			if(!comboCollection.contains(valueCombination))
			{
				comboCollection.add(valueCombination);
			}
		}
		
		return comboCollection;
	}

	public ArrayList<MTS_transition> getAllOutGoing(int state)
	{
		/*ArrayList<MTS_transition> returnTransitions = new ArrayList<MTS_transition>();
		
		for(int i = 0; i < transitions.size(); i++)
		{
			MTS_transition currentTransition = transitions.get(i);
			if(currentTransition.getStart() == state) returnTransitions.add(currentTransition);
		}
		
		return returnTransitions;*/
		
		if(outgoingByState.containsKey(state)) {
			return new ArrayList<MTS_transition>(outgoingByState.get(state));
		} else {
			return new ArrayList<MTS_transition>();
		}
	}
	
	public MTS_state getState(int i)
	{
		if(statesByName.containsKey(i)) {
			return statesByName.get(i);
		} else { 
			for(MTS_state state : states) {
				if(state.getName() == i) return state;
			}
		}
		return null;
	}

	public MTS cloneMTS(String name) {
		MTS clone = new MTS(name, varNames, getInitialState().getVariableState());
		for(MTS_state state : states)
		{
			if(state.getName() != 0)
			{
				clone.unsafeAddMTSState(new MTS_state(state.getName(), state.getVariableState()));
			}
		}
		for(MTS_transition tran : transitions)
		{
			clone.unsafeAddMTSTransition(new MTS_transition(tran.getName(), tran.getStart(), tran.getEnd(), tran.getType()));
		}
		return clone;
	}
	
	public void addToUnreachable(int i)
	{
		unreachable_states.add(i);
	}
	
	public boolean isUnreachable(int i)
	{
		if(unreachable_states.contains(i)) return true;
		
		return false;
	}

	public ArrayList<MTS_transition> getAllIncoming(int state) 
	{
		/*ArrayList<MTS_transition> return_transitions = new ArrayList<MTS_transition>();
		
		for(MTS_transition currentTran : transitions)
		{
			if(currentTran.getEnd() == state)
			{
				return_transitions.add(currentTran);
			}
		}
		
		return return_transitions;*/
		
		if (incomingByState.containsKey(state)) {
			return new ArrayList<MTS_transition>(incomingByState.get(state));
		} else {
			return new ArrayList<MTS_transition>();
		}
	}

	public void removeTransition(MTS_transition incoming) 
	{
		transitions.remove(incoming);
		
		ArrayList<MTS_transition> oldIncoming = incomingByState.get(incoming.getEnd());		
		oldIncoming.remove(incoming);
		
		ArrayList<MTS_transition> oldOutgoing = outgoingByState.get(incoming.getStart());		
		oldOutgoing.remove(incoming);
	}

	public int getReachableSize() 
	{
		return states.size() - unreachable_states.size();
	}
	
	public void changeMTSDest(MTS_transition trans, int newStateName) {
		ArrayList<MTS_transition> oldIncoming = incomingByState.get(trans.getEnd());		
		oldIncoming.remove(trans);
		
		if(incomingByState.containsKey(newStateName)) {
			(incomingByState.get(newStateName)).add(trans);
		} else {
			ArrayList<MTS_transition> newIncomingSet = new ArrayList<MTS_transition>();
			newIncomingSet.add(trans);
			incomingByState.put(newStateName, newIncomingSet);
		}

		trans.changeDest(newStateName);
	}

	public void removeNonRequired() {
		Set<MTS_transition> toRemove = new HashSet<MTS_transition>();		
		for (MTS_transition tran : this.transitions) {
			if (! tran.getType().equals("required")) {
				toRemove.add(tran);
			}
		}
		
		this.transitions.removeAll(toRemove);
		
		for (Integer key : outgoingByState.keySet()) {
			outgoingByState.get(key).removeAll(toRemove);
		}
		
		for (Integer key : incomingByState.keySet()) {
			incomingByState.get(key).removeAll(toRemove);
		}
	}
	
	public void removeOtherEvents(Set<String> eventsToKeep) {
		Set<MTS_transition> toRemove = new HashSet<MTS_transition>();		
		for (MTS_transition tran : this.transitions) {
			if (! eventsToKeep.contains(tran.getEvent())) {
				toRemove.add(tran);
			}
		}
		
		this.transitions.removeAll(toRemove);
		
		for (Integer key : outgoingByState.keySet()) {
			outgoingByState.get(key).removeAll(toRemove);
		}
		
		for (Integer key : incomingByState.keySet()) {
			incomingByState.get(key).removeAll(toRemove);
		}
	}
	
	public Set<String> getEvents () {
		Set<String> toReturn = new HashSet<String>();		
		for(MTS_transition currentTransition : this.transitions) {
			toReturn.add(currentTransition.getEvent().toString());
		}		
		return toReturn;
	}

}
