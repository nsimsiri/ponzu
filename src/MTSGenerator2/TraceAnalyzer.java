package MTSGenerator2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import DataTypes.AnalysisInstance;
import DataTypes.Event2;
import DataTypes.MTS;
import DataTypes.MTS_inv_transition;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;
import DataTypes.Scenario2;
import DataTypes.Scenario2.Invocation;
import daikon.VarInfo;
import javafx.scene.Scene;

public class TraceAnalyzer {
	
	private HashMap<String,ArrayList<Integer>> traversed_states_byID = new HashMap<String, ArrayList<Integer>>();
	private AnalysisInstance instance;
	private MTS invariant_MTS;
	private MTS initialTraceMTS;
	private HashMap<Integer, Integer> init_reference = new HashMap<Integer, Integer>();
	public YicesRunner yicesRun;
	private Converter conv;
	private HashMap<Integer, HashMap<Integer, Set<String>>> stateTails;
	
	private boolean verbose = true;
	private boolean removeNonDet = true;

	public TraceAnalyzer(AnalysisInstance instance, MTS invariant_MTS, YicesRunner yicesRun, Converter conv)
	{	
		this.instance = instance;
		this.invariant_MTS = invariant_MTS;
		this.yicesRun = yicesRun;
		this.conv = conv;

//        checkTraversalOrderAndTerminateSystem();
		determineTraversalOrder();
		createTraceMTS();
	}
	
	public MTS traditionalKTail(int tail)
	{
		HashMap<Integer,Integer> old_references = (HashMap<Integer,Integer>) init_reference.clone();
		for(Integer key : init_reference.keySet())
		{
			init_reference.put(key, 0);
		}
		MTS returnMTS = kTailEnhanced(tail);
		returnMTS.setName("Traditional_" + tail + "_tail");
		init_reference = old_references;
		return returnMTS;
	}

	public MTS kTailEnhanced(int tail)
	{
		stateTails = new HashMap<Integer, HashMap<Integer,Set<String>>>();
		int maxIndex = initialTraceMTS.getStateSize();
		
		if(maxIndex <= 2)
		{
			return initialTraceMTS.cloneMTS("Enhanced_ " + tail + "_tail");			
		}
		else
		{
			// Cloning the trace MTS; we will do state merging on the clone.
			MTS kTailModel = initialTraceMTS.cloneMTS("Enhanced_" + tail + "_tail");
			int stateProcessed = 1;
			int currentIndex = 1;
			
			while(currentIndex < maxIndex - 1)
			{
				while(kTailModel.isUnreachable(currentIndex) && currentIndex < maxIndex - 1)
					currentIndex++;
				MTS_state firstState = kTailModel.getState(currentIndex);
				int nextStateProcessed = 0;
				int nextIndex = currentIndex + 1;
				while(nextIndex < maxIndex)
				{
					while(kTailModel.isUnreachable(nextIndex) && nextIndex < maxIndex)
					{
						nextIndex++;
					}
					
					if(nextIndex == maxIndex) break;
					
					MTS_state secondState = kTailModel.getState(nextIndex);
					int tail_comparison = 
						compareKTails(kTailModel, firstState, secondState, tail);
					if(tail_comparison == 3)
					{
						if (verbose)
							System.out.println("Found a matching tail for states " + firstState + " and " + secondState);
						mergeStates(kTailModel, firstState, secondState);
					}
					else if(tail_comparison == 1)
					{
						if (verbose)
							System.out.println("The first tail subsumes the second.");
					}
					else if(tail_comparison == 2)
					{
						if (verbose)
							System.out.println("The second tail subsumes the first.");
					}
					
					nextIndex++;
					nextStateProcessed++;
				}
				
				currentIndex++;
				stateProcessed ++;
			}
			
			System.out.println("The final model has " + kTailModel.getReachableSize() + " states");
			return kTailModel;
		}
	}
	
	public MTS refinementStrategy(String name)
	{
		MTS refinedMTS = invariant_MTS.cloneMTS(name);
		for(MTS_transition tran : refinedMTS.getAllTransitions()) tran.changeType("maybe");
		
		HashMap<Integer, HashMap<String,Integer>> refinement_map = new HashMap<Integer, HashMap<String,Integer>>();
		
		for(MTS_transition firstTransition : initialTraceMTS.getAllOutGoing(0))
		{
			MTS_transition currentTransition = firstTransition;
			Integer trueSource = 0;
			
			while(currentTransition != null)
			{
				Integer source = init_reference.get(currentTransition.getStart());
				Integer destination = init_reference.get(currentTransition.getEnd());

				if(source.intValue() == destination.intValue())
				{
					destination = trueSource;
					refinedMTS.requireTransitionInference(trueSource, destination, currentTransition.getEvent());
				}
				else if(!refinement_map.containsKey(destination))
				{
					MTS_state newState = refinedMTS.refineAutomataInference(trueSource, destination, currentTransition.getEvent());
					HashMap<String,Integer> stateCharacterization = new HashMap<String, Integer>();
					stateCharacterization.put(source + "->" + currentTransition.getEvent(), newState.getName());
					refinement_map.put(destination, stateCharacterization);
					
					trueSource = newState.getName();
				}
				else if(refinement_map.containsKey(destination) 
						&& !refinement_map.get(destination).containsKey(source + "->" + currentTransition.getEvent()))
				{
					MTS_state newState = refinedMTS.refineAutomataInference(trueSource, destination, currentTransition.getEvent());
					HashMap<String,Integer> stateCharacterization = refinement_map.get(destination);
					stateCharacterization.put(source + "->" + currentTransition.getEvent(), newState.getName());

					trueSource = newState.getName();
				}
				else
				{
					destination = refinement_map.get(destination).get(source + "->" + currentTransition.getEvent());
					refinedMTS.requireTransitionInference(trueSource, destination, currentTransition.getEvent());

					trueSource = destination;					
				}
				
				ArrayList<MTS_transition> nextTransitions = initialTraceMTS.getAllOutGoing(currentTransition.getEnd());
				
				if(nextTransitions.isEmpty()) currentTransition = null;
				else currentTransition = initialTraceMTS.getAllOutGoing(currentTransition.getEnd()).get(0);
			}
		}
		
/*		for(Long sequenceID : instance.scenario.getSequenceIDs())
		{
			System.out.println(sequenceID + ": " + instance.scenario.getSequenceByID(sequenceID).get(0).post_values);
		}
*/		
		
		// This part merges back states with identical purely outgoing transitions
		for(Integer refinedState : refinement_map.keySet())
		{
			Collection<Integer> refinements = refinement_map.get(refinedState).values();
			
			Collection<Integer> processed = new ArrayList<Integer>();
			
			for(Integer firstState : refinements)
			{
				if(processed.contains(firstState)) continue;
				ArrayList<MTS_transition> firstTransitions = refinedMTS.getAllOutGoing(firstState);
				
				for(Integer secondState : refinements)
				{
					if(firstState.intValue() == secondState.intValue() || processed.contains(secondState)) 
					{
						continue;
					}
					
					// Compare the strictly outgoing transitions
					ArrayList<MTS_transition> secondTransitions = refinedMTS.getAllOutGoing(secondState);
					boolean foundDifference = false;
					
					for(MTS_transition tran1 : firstTransitions)
					{
						if(firstState.intValue() != tran1.getEnd()) {
							boolean foundMatch = false;
							
							for(MTS_transition tran2 : secondTransitions)
							{
								if(secondState.intValue() != tran2.getEnd())
								{
									if(tran1.getEnd() == tran2.getEnd() && tran1.getEvent().equals(tran2.getEvent())) {
										foundMatch = true;
										
										if (!tran1.getType().equals(tran2.getType())) {
											foundDifference = true;
											break;
										}
									}								
								}
							}
							
							if(foundDifference || !foundMatch) {
								foundDifference = true;
								break;
							}
						}
					}
					
					// The states are the same and should be merged back together
					if(!foundDifference)
					{
						ArrayList<MTS_transition> incomingFirst = refinedMTS.getAllIncoming(firstState);
						ArrayList<MTS_transition> incomingSecond = refinedMTS.getAllIncoming(secondState);
						
						for(MTS_transition incoming2 : incomingSecond)
						{
							if(incoming2.getStart() != secondState.intValue())
							{
								refinedMTS.changeMTSDest(incoming2,firstState.intValue());
							}
							else
							{
								for(MTS_transition incoming1 : incomingFirst)
								{
									if(incoming1.getEnd() == firstState.intValue() 
											&& incoming1.getEvent().equals(incoming2.getEvent()))
									{
										incoming1.increaseOccurrencesBy(incoming2.getOccurrences());
										if(incoming2.getType().equals("required") && incoming1.getType().equals("maybe"))
										{
											incoming1.changeType("required");
										}
									}
								}
							}
						}
						
						for(MTS_transition tran1 : firstTransitions)
						{
							for(MTS_transition tran2 : secondTransitions)
							{
								if(firstState.intValue() != tran1.getEnd() && secondState.intValue() != tran2.getEnd())
								{
									if(tran1.getEnd() == tran2.getEnd() && tran1.getEvent().equals(tran2.getEvent()))
									{
										tran1.increaseOccurrencesBy(tran2.getOccurrences());
										if(tran2.getType().equals("required") && tran1.getType().equals("maybe"))
										{
											tran1.changeType("required");
										}
									}
								}
							}
						}
						
						refinedMTS.addToUnreachable(secondState);
			 			processed.add(secondState);
						
						System.out.println("Merged back S" + firstState + " and S" + secondState);
					}
				}
				
				processed.add(firstState);
			}
		}
		
		// TODO:
		// Possibly remove all non-deterministic maybe transitions when there is a purely outgoing required transition 
		// on the same event from a particular state.
		
		// TODO: From the initial state remove all the non-determinism, no matter the exact name because some constructors 
		// may just not be used and we can believe that they would lead to the same states.
		if(removeNonDet)
		{
			Set<Integer> traversedStates = new HashSet<Integer>();
			Set<MTS_transition> toDelete = new HashSet<MTS_transition>();
			Set<Integer> nextStates = new HashSet<Integer>();
			Integer currentState = refinedMTS.getInitialState().getName();
			
			nextStates.add(currentState);
			
			ArrayList<MTS_transition> processTransitions =  refinedMTS.getAllOutGoing(currentState);
			
			while(!nextStates.isEmpty())
			{
				for(MTS_transition currentTransition : processTransitions)
				{
					// TODO:
					// Possibly modify to apply non-determinism removal only in cases when the required transition
					// is outgoing from a state.
					if(currentTransition.getType().equals("required"))
					{
						for(MTS_transition possibleNonDeterminism : processTransitions)
						{
							if(possibleNonDeterminism.getType().equals("maybe") && 
									possibleNonDeterminism.getEvent().equals(currentTransition.getEvent()) &&
									possibleNonDeterminism.getEnd() != currentTransition.getEnd())
							{
								toDelete.add(possibleNonDeterminism);
							}
						}
					}
					
					Integer targetState = new Integer(currentTransition.getEnd());
					
					if(!traversedStates.contains(targetState)
							&& !nextStates.contains(targetState))
					{
						nextStates.add(targetState);
					}
				}
				
				traversedStates.add(currentState);
				nextStates.remove(currentState);
				
				if(!nextStates.isEmpty())
				{
					currentState = nextStates.iterator().next();
					processTransitions = refinedMTS.getAllOutGoing(currentState);
				}
			}
			
			for(MTS_transition deleteTrans : toDelete)
			{
				/*if(deleteTrans.getStart() == 0)*/ refinedMTS.removeTransition(deleteTrans);
			}
		}
		
		return refinedMTS;
	}
	
	private void mergeStates(MTS kTailModel, MTS_state firstState,
			MTS_state secondState) 
	{
		ArrayList<MTS_transition> incomingTransitions = kTailModel.getAllIncoming(secondState.getName());
		for(MTS_transition incoming : incomingTransitions)
		{
			MTS_transition newTransition = new MTS_transition(incoming.getName(), incoming.getStart(), firstState.getName(), "maybe");
			kTailModel.addMTSTransition(newTransition);
			kTailModel.removeTransition(incoming);
		}
		
		ArrayList<MTS_transition> outgoingTransitions = kTailModel.getAllOutGoing(secondState.getName());
		for(MTS_transition outgoing : outgoingTransitions)
		{
			MTS_transition newTransition = new MTS_transition(outgoing.getName(), firstState.getName(), outgoing.getEnd(), "maybe");
			kTailModel.addMTSTransition(newTransition);
			kTailModel.removeTransition(outgoing);
		}
		
		kTailModel.addToUnreachable(secondState.getName());
	}

    private void checkTraversalOrderAndTerminateSystem(){
        for(String currentID : instance.scenario.getSequenceIDs()){
            MTS_state currentState = invariant_MTS.getInitialState();
            ArrayList<MTS_state> invariant_MTS_states = invariant_MTS.getAllStates();
            boolean found_constructor = false;
            boolean found_transition;
            String str;

            ArrayList<Integer> traversed_states = new ArrayList<Integer>();
            traversed_states.add(new Integer(currentState.getName()));

            ArrayList<Scenario2.Invocation> currentScenario = instance.scenario.getSequenceByID(currentID);
            System.out.println("=====================================");

            System.out.println("TRACE ID: " + currentID);
            for(int i = 0; i < currentScenario.size(); i++){
                System.out.printf("\t(%s) %s\n", i+1, currentScenario.get(i).event.getName());
            }

            checkTraceIsEmbeddedInInvaBasedModel(currentScenario, currentID);
        }
        System.exit(-1);
    }

	private void determineTraversalOrder()
	{
		for(String currentID : instance.scenario.getSequenceIDs()) {
			MTS_state currentState = invariant_MTS.getInitialState();
			ArrayList<MTS_state> invariant_MTS_states = invariant_MTS.getAllStates();
			boolean found_constructor = false;
			boolean found_transition;
			String str;
			
			ArrayList<Integer> traversed_states = new ArrayList<Integer>();
			traversed_states.add(new Integer(currentState.getName()));
			
			ArrayList<Scenario2.Invocation> currentScenario = instance.scenario.getSequenceByID(currentID);

            Queue<MTS_state_wrapper> q = new LinkedList<>();
            Queue<MTS_state_wrapper> qTemp = new LinkedList<>();
            q.add(new MTS_state_wrapper(currentState, null)); // first state in queue

			System.out.println("[PONZU] TRACE ID: " + currentID);
			for(int i = 0; i < currentScenario.size(); i++){
				System.out.printf("\t(%s) %s\n", i+1, currentScenario.get(i).event.getName());
			}

			for(int i = 0; i < currentScenario.size(); i++){
                Invocation currentInvocation = currentScenario.get(i);
                System.out.println("INVOC %s " + currentInvocation.event.getName());
				found_transition = false;

				ArrayList<MTS_transition> outgoing_transitions;
				outgoing_transitions = invariant_MTS.getAllOutGoing(currentState.getName());

				ArrayList<MTS_state> candidateNextStates = new ArrayList<MTS_state>();

				// find all vtx in invabased where incoming edge has the same event (method call).
				// at least one state is from actual trace.
				for(MTS_transition trans : outgoing_transitions) {
					if(trans.getEvent().equals(currentInvocation.event.getName())) {
						for(MTS_state st : invariant_MTS_states) {
							if(st.getName() == trans.getEnd()) {
								candidateNextStates.add(st);
							}
						}
					}
				}

				int numFound = 0;
				//yicesRun.reset();

				if(candidateNextStates.size() == 0 && found_constructor) {
					System.out.println("[DetTrav ERR_2]: " + currentID + ": " + "There may be a problem in the trace or the invariant-based automaton because event "
							+ currentInvocation.event.getName() + " does not exist in state " + currentState.toString());
					System.out.printf("[SKIPPING TRACE] INFO: candidateNextStates.size() = %s && found_constructor = %s\n", candidateNextStates.size(), found_constructor);
					break; // quick fix - Natcha
//					System.exit(-1);
				}

				else if(candidateNextStates.size() == 0) {
					continue;
				}
                // Further statements needed for variables that are unmodified.
				else if(candidateNextStates.size() > 0) {
					yicesRun.push();
					for (VarInfo var : instance.variables) {
                        // we check if this variable is recorded in the trace - if not
						if (currentInvocation.has_post_var(var.name())){
                            str = "(= " + conv.toYicesExpr(var, true) + " "
                                    + conv.toYicesExpr(currentInvocation.get_post_value(var.name())) + ")";
                            System.out.println("\t evaluating " + str);
                            yicesRun.assertExpr(str);
                        }

					}

					if(yicesRun.isInconsistent()) {
						yicesRun.pop();

						System.out.println("[DetTrav ERR_2]: " + currentID + ": " + "There may be a problem in the trace or the invariant-based automaton because event "
								+ currentInvocation.event.getName() + " does not exist in state " + currentState.toString());
						continue; // Natcha - ignore all errors.
//						System.exit(-1);
					}

					System.out.println("# Evaluating Candidate states: " + candidateNextStates.size());

					for(MTS_state nextState : candidateNextStates) {
						yicesRun.push();
						yicesRun.assertExpr(nextState.getVariableState());
						boolean yicesResult = yicesRun.isInconsistent();
						System.out.printf("\t %s -> notConsistent: %s (%s, %s)\n", nextState.getVariableState(), yicesResult, currentState.getName(), nextState.getName());

						if(!found_transition && !yicesResult) // if transition not yet found and is consistent
						{
							currentState = nextState;
							found_transition = true;
							found_constructor = true;
						}

						/* Natcha: temporary fix problem (10/29/2016)*/
						else if(!yicesResult) {
							numFound++;
						}

						yicesRun.pop();
						/* [Natcha Simsiri] Line was initially commented out - which makes stackar not run - bringing back statement makes it work*/
//						if(found_transition) break;
					}

					/* Natcha's addition: if no nextState found, always add final candidate state*/
//					if (!found_transition){
//						currentState = candidateNextStates.get(candidateNextStates.size()-1);
//						System.out.println(currentID + ": "
//								+ "[WARNING] NOT FOUND CONSISTENT TRANSITION: adding inconsistent transition"
//								+ currentInvocation.event.getName() + " does not exist in state " + currentState.toString());
//						found_transition = true;
//						found_constructor = true;
//					}

					yicesRun.pop();
				}
				if(!found_transition) { // originally was !found_transition || numFound > 0
	/*broken Here ---> (26/10) */
					System.out.println(currentID + ": "
					        + "EXITING: There may be a problem in the trace or the invariant-based automaton because event "
							+ currentInvocation.event.getName() + " does not exist in state " + currentState.toString());
					System.out.printf("found_trans=%s numFound=%s\n", found_transition, numFound);
					System.out.println("# candidate state: " + candidateNextStates.size());
					System.exit(-1);
				}

				if (verbose)
					System.out.println("Identified transition on " + currentInvocation.event.getName() + " to state " + currentState.toString());
				traversed_states.add(new Integer(currentState.getName()));
			}

			traversed_states_byID.put(currentID, traversed_states);
			System.out.printf("ID: %s = %s", currentID, traversed_states);
		}
	}

    public static class MTS_state_wrapper {
        public MTS_state_wrapper prev;
        public MTS_state state;
        public MTS_transition edge;
        public MTS_state_wrapper(MTS_state state, MTS_state_wrapper prev){
            this.prev = prev;
            this.state = state;
        }
    }

	private void checkTraceIsEmbeddedInInvaBasedModel(List<Scenario2.Invocation> invocations, String scenarioID){
		Queue<MTS_state_wrapper> q = new LinkedList<>();
		int curInvocCount = 0;
		MTS_state initState = invariant_MTS.getInitialState();
        System.out.printf("[TestTrace]: ScenarioID = %s with invocation size = %s\n", scenarioID, invocations.size());
		q.add(new MTS_state_wrapper(initState, null));

        Queue<MTS_state_wrapper> qFinal = new LinkedList();

		for (int i = 0; i < invocations.size(); i++){
			Scenario2.Invocation invocation = invocations.get(i);

            Queue<MTS_state_wrapper> tempQ = new LinkedList<>();

            while(!q.isEmpty()){
                MTS_state_wrapper curWrapperState = q.poll();

                MTS_state curState = curWrapperState.state;
                List<MTS_transition> outgoing_transitions = invariant_MTS.getAllOutGoing(curState.getName());

                for(MTS_transition trans : outgoing_transitions) {
                    if(trans.getEvent().equals(invocation.event.getName())) {
                        for(MTS_state st : invariant_MTS.getAllStates()) {
                            if(st.getName() == trans.getEnd()) {
                                MTS_state_wrapper nextStateWrapper = new MTS_state_wrapper(st, curWrapperState);
                                tempQ.add(nextStateWrapper);
                                if (i==invocations.size()-1){
                                    qFinal.add(nextStateWrapper);
                                }
                            }
                        }
                    }
                }
            }
            if (tempQ.isEmpty() && i < invocations.size()-1) {
                System.out.println("[TEST TRACE ERR] Queue is empty before finishing Trace. Currently at " + invocation.event.getName());
                return;
            }
            while(!tempQ.isEmpty()) q.add(tempQ.poll());
		}

//        for(MTS_state_wrapper msw : qFinal){
//            int c = invocations.size();
//            MTS_state_wrapper curW = msw;
//            System.out.printf("\n\t[%s] ", c--);
//            while(curW != null){
//                System.out.printf("S%s ",curW.state.getName());
//                curW = curW.prev;
//            }
//        }
        System.out.printf("[TEST TRACE] there are %s traces found from InvaBased.\n", qFinal.size());
	}
	
	private void createTraceMTS()
	{
		initialTraceMTS = new MTS("TraceMTS", null, null);		
		int newStateIndex = 1;
		
		for(String current_ID : instance.scenario.getSequenceIDs())
		{
			ArrayList<Invocation> currentScenario = instance.scenario.getSequenceByID(current_ID);
			MTS_state currentState = initialTraceMTS.getInitialState();
			ArrayList<Integer> invariantsMTSTraversal = traversed_states_byID.get(current_ID);
			Integer currentInvariantState = invariantsMTSTraversal.get(0);
			init_reference.put(currentState.getName(), currentInvariantState);
			if (invariantsMTSTraversal.size() > currentScenario.size()){
				for(int i = 0; i < currentScenario.size(); i++)
				{
					Invocation currentInvocation = currentScenario.get(i);
					System.out.println(currentInvocation.event.getPptName());
					MTS_state nextState = new MTS_state(newStateIndex, null);
					newStateIndex++;
					initialTraceMTS.unsafeAddMTSState(nextState);

					MTS_transition newTransition = new MTS_transition(currentInvocation.event.getPptName(), currentState.getName(), nextState.getName(), "maybe");
					initialTraceMTS.unsafeAddMTSTransition(newTransition);

					Integer nextInvariantState = invariantsMTSTraversal.get(i + 1);
					init_reference.put(nextState.getName(), nextInvariantState);

					currentState = nextState;
					currentInvariantState = nextInvariantState;
				}
			}
		}
	}
	
	private int compareKTails(MTS model, MTS_state first, MTS_state second, int k)
	{	
		Set<String> firstTail = getKTail(model, first, k);
		Set<String> secondTail = getKTail(model, second, k);

		if(firstTail.size() != 0 && secondTail.size() != 0 
				&& firstTail.containsAll(secondTail) && !secondTail.containsAll(firstTail))
		{
			return 1;
		}
		else if(firstTail.size() != 0 && secondTail.size() != 0 
				&& !firstTail.containsAll(secondTail) && secondTail.containsAll(firstTail))
		{
			return 2;
		}
		else if(firstTail.size() != 0 && secondTail.size() != 0 
				&& firstTail.containsAll(secondTail) && secondTail.containsAll(firstTail))
		{
			return 3;
		}
		else if(firstTail.size() == 0 && secondTail.size() == 0)
		{
			return 3;
		}
		
		return 0;
	}
	
	private Set<String> getKTail(MTS model, MTS_state currentState, int k)
	{
		Set<String> returnTail = new LinkedHashSet<String>();
		if(!stateTails.containsKey(currentState.getName()))
		{
			stateTails.put(currentState.getName(), new HashMap<Integer, Set<String>>());
		}

		HashMap<Integer, Set<String>> current_tail = stateTails.get(currentState.getName());
		if(current_tail.containsKey(k))
		{
			return current_tail.get(k);
		}
		else if (k > 1)
		{
			ArrayList<MTS_transition> outgoing = model.getAllOutGoing(currentState.getName());
			
			for(MTS_transition currentTrans : outgoing)
			{
				Set<String> lowerTail = getKTail(model, model.getState(currentTrans.getEnd()), k-1);
				if(lowerTail.size() == 0)
				{
					String new_tail = init_reference.get(currentState.getName()) + "->"
										+ currentTrans.getEvent() + "->" 
										+ init_reference.get(currentTrans.getEnd());
					returnTail.add(new_tail);
				}
				else
				{
					for(String tail : lowerTail)
					{
						String new_tail = init_reference.get(currentState.getName()) + "->"
											+ currentTrans.getEvent() + "->" + tail;
						returnTail.add(new_tail);
					}
				}
			}
			
			current_tail.put(k, returnTail);
			return returnTail;
		}
		else if (k == 1)
		{
			for(MTS_transition currentTrans : model.getAllOutGoing(currentState.getName()))
			{
				String new_tail = init_reference.get(currentState.getName()) + "->"
									+ currentTrans.getEvent() + "->" + 
									init_reference.get(currentTrans.getEnd());
				returnTail.add(new_tail);
			}
			
			current_tail.put(k, returnTail);
			return returnTail;
		}
		
		return null;
	}

	public MTS getInitialTraceModel(){
		return this.initialTraceMTS;
	}

	/**
	 * @author Natcha Simsiri
	 * For each of the MTS's outgoing edges, a set of pre/post invariants will be extracted
	 * from the AnalysisInstance object and attached to the edge. Annotation is done through deep copying the
	 * MTS_transition to a subclass, MTS_inv_transition, where invariants are dealt with.
	 *
	 * @param mts (required) input FSM in the form of an MTS.
	 * @param instance (required) the AnalysisInstance obtained from reading the Daikon dtrace/invariant files.
	 */
	public static void annotateTransitionWithInvariants(MTS mts, AnalysisInstance instance){
		ArrayList<MTS_transition> old_transition = new ArrayList<MTS_transition>();
		for(MTS_transition transition : mts.getAllTransitions()){
			old_transition.add(transition);
		}
		ArrayList<MTS_inv_transition> new_transition = new ArrayList<MTS_inv_transition>();
		for(Iterator<MTS_transition> itr = old_transition.iterator(); itr.hasNext();){
			MTS_transition transition = itr.next();
			if (!instance.events.containsKey(transition.getEvent())){
				System.out.println("Error annotating. Unable to find event.");
				return;
			}
			Event2 event = instance.events.get(transition.getEvent());
			MTS_inv_transition inv_transition = MTS_inv_transition.MTS_transitionToInvAnnotated(transition, event);
			mts.removeTransition(transition);
			new_transition.add(inv_transition);
		}


		for(int i = 0; i < new_transition.size(); i++){
			MTS_transition new_trans = (MTS_transition) new_transition.get(i);
			boolean replaceable = mts.addMTSTransition(new_trans);
			assert(replaceable);
		}

		/*
		for(MTS_transition transition: mts.getAllTransitions()){
			MTS_inv_transition hi = (MTS_inv_transition) transition;
			assert(transition instanceof MTS_inv_transition);
		}

		for(MTS_state state : mts.getAllStates()){
			for(MTS_transition transition : mts.getAllIncoming(state.getName())){
//				System.out.println("[out-deg]: " + transition.toString());
				assert(transition instanceof MTS_inv_transition);
			}
			for(MTS_transition transition : mts.getAllOutGoing(state.getName())){
//				System.out.println("[in-deg]: " + transition.toString());
				assert(transition instanceof MTS_inv_transition);
			}
		}
		*/

		System.out.println("all assertions are evaluated");
	}
}
