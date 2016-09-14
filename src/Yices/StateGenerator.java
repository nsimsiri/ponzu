package Yices;

import yices.YicesLite;
import YicesHelpers.Converter;
import YicesHelpers.Generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import DataTypes.Event;
import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;
import MTSGenerator.initialization.ConstraintSet_Generator;
import MTSGenerator.output.outputResults;

public class StateGenerator {

	public StateGenerator(){
		totalPredList = new ArrayList<ArrayList<String>>();
		yL = new YicesLite();
	}
	
	public String[][] vec;
    public ArrayList<ArrayList<String>> totalPredList;
    public YicesLite yL;
	
	public void run(){
		//List of events
		ArrayList<Event> eventList = new ArrayList<Event>();
		
		//List of predicates
		ArrayList<String> predicate = new ArrayList<String>();
		ArrayList<ArrayList<String>> predicateList = new ArrayList<ArrayList<String>>();
		
		//List of states
		ArrayList<ArrayList<String>> stateList = new ArrayList<ArrayList<String>>();
		
		//System state variables and their initial values
		HashMap<String,String> variable_names = new HashMap<String,String>();
		ArrayList<String> invariants = new ArrayList<String>();
		
		//Processing Queue
		Queue<MTS_state> processingQueue = new LinkedList<MTS_state>();
		
		Converter c = new Converter();
		Generator g = new Generator();
		
		//Extension of the input files
		String systemDir = "\\QueueArModifiedTest";
		String extension = "_test";
		
		//Reading constraint set and events from a specially formatted XML file 
		ConstraintSet_Generator constraintReader = new ConstraintSet_Generator();
		eventList = constraintReader.extractConstraints(".\\src\\Input_files" + systemDir + "\\constraints" + extension + ".xml", variable_names, invariants);
		
		ArrayList<String> variables = new ArrayList<String>();
		Set<String> varNames = variable_names.keySet();
		Iterator<String> nameIterator = varNames.iterator();
		
		//Obtain the set of all significant variables for a component
		while(nameIterator.hasNext())
		{
			variables.add(nameIterator.next());
		}
		
		//Determine the initial values of the significant variables
		ArrayList<String> initial_values = new ArrayList<String>();
		for(int j = 0; j < variables.size(); j++)
		{
			initial_values.add(variable_names.get(variables.get(j)));
		}
		
		//Add post invariants
		ArrayList<String> postInvariants = new ArrayList<String>();
		for(String inv : invariants){
			postInvariants.add(this.appendPost(variables, inv));
		}
		invariants.addAll(postInvariants);
	
		//Create an MTS with the initial state corresponding to the initial values
		//of the component's significant variables
		MTS systemMTS = new MTS("EntireSystem", variables, initial_values);
		MTS_state initialState = systemMTS.getInitialState();

		//MTS Transitions
		ArrayList<MTS_transition> transitions = new ArrayList<MTS_transition>();
		
		//MTS States
		ArrayList<MTS_state> allStates = new ArrayList<MTS_state>();
		ArrayList<MTS_state> processingStates = new ArrayList<MTS_state>();
		processingStates.add(initialState);
		
		
		//Define Vars
		for(Event e : eventList){
			System.out.println(e.getPreCond());
			ArrayList<String> atomicPreConds = new ArrayList<String>();
			predicate = new ArrayList<String>();
			for( String pc : e.getPreCond()){
				atomicPreConds.addAll(extractAtomic(pc));
			}
			//System.out.println(atomicPreConds);
			
			String pred;

			for(String p : atomicPreConds){
				predicate = new ArrayList<String>();
				pred = p;
				predicate.add("("+pred+")");
				predicate.add("(not ("+pred+"))");
				boolean notFound = true;
				for(ArrayList<String> definedPred:predicateList){
					if(definedPred.get(0).equals(predicate.get(0))){
						notFound=false;
					}
				}
				if(notFound)
					predicateList.add(predicate);
			}
			//predicateList.add(predicate);
		}
		
		totalPredList = predicateList;
		
		//Building the States
		getMTSStates(0,allStates,new ArrayList<String>());
		/*
		System.out.println("**States**");
		for(MTS_state ms : allStates ){
			System.out.println(ms.getVariableState());
		}
		System.out.println("**End States**");
		*/
		System.out.println("**Events**");
		String postStr;
		for(Event ev : eventList ){
			System.out.println(ev.getPreCond());
		}
		System.out.println("**End Events**");
		
		//Get SMT syntax for Defined Variables
		YicesRunner yicesRun = new YicesRunner();
		yicesRun.defineVars(variables);
		int result=0;
		
		// Get all reachable states
		System.out.println("**Reachable States**");
		int realStates=0;
		for(MTS_state ms : allStates ){
			ms.setName(ms.getName()+1);
			yicesRun.addExpr(invariants);
			yicesRun.addExpr(ms.getVariableState());
			result = yicesRun.runYices();
			if(result==0){
				System.out.println("S" + ms.getName()+ ": " + ms.getVariableState());
				processingStates.add(ms);
				realStates++;
			}
			yicesRun.reset();
		}
		System.out.println("**Reachable States End**");
		allStates.clear();
		
		systemMTS.addMTSState(initialState);
		//use initial state to figure out which state to start in
		for(MTS_state state : processingStates ){
			if(state.equals(initialState))
				continue;
			//if state and initialState
			yicesRun.addExpr(initialState.getVariableState());
			yicesRun.addExpr(state.getVariableState());
			result = yicesRun.runYices();
			yicesRun.reset();
			if(result==0){ //add to queue
				System.out.println("Accepted Start: "+state.getVariableState());
				processingQueue.add(state);
				state.setProcessed();
				systemMTS.addMTSState(state);
				systemMTS.addMTSTransition(new MTS_transition("lambda", initialState.getName(), state.getName(), "true"));
			}
		}
		
		//Iterate through queue of unprocessed states
		while(!processingQueue.isEmpty()){
			MTS_state currState = processingQueue.poll();
			systemMTS.addMTSState(currState);
			
			//Find possible transitions from current state
			for(int i=0;i<eventList.size();i++){
				yicesRun.addExpr(invariants);
				//Add Current state expressions
				yicesRun.addExpr(currState.getVariableState());
				//Add Event expressions
				yicesRun.addExpr(eventList.get(i).getPreCond());
				result = yicesRun.runYices();
				yicesRun.reset();
				//if result == 0 -- no error then make transition between states
				if(result==0){
					//System.out.println("State "+currState.getName()+" -> "+eventList.get(i).getName());
					MTS_state nextState;
					for(int j=1;j<processingStates.size();j++){ //0 is init value not a state
						nextState = processingStates.get(j);
						yicesRun.addExpr(invariants);
						//Current State
						yicesRun.addExpr(currState.getVariableState());
						
						yicesRun.addExpr(eventList.get(i).getPreCond());
						//Next State
						yicesRun.addExpr(appendPostAll(variables, nextState.getVariableState()));
						yicesRun.addExpr(eventList.get(i).getPostCond());
						
						result = yicesRun.runYices();
						
						if(result==0){
							//yicesRun.dumpContext();
							//System.out.println(currState.getVariableState());
							//System.out.println("  "+eventList.get(i).getName()+" -> State "+nextState.getName());
							//Add MTS Transition
							systemMTS.addMTSTransition(new MTS_transition(eventList.get(i).getName(), currState.getName(), nextState.getName(), "true"));
							//Add to queue
							if(!nextState.isProcessed()){
								nextState.setProcessed();
								processingQueue.add(nextState);
							}
						}
						yicesRun.reset();
					} //End state iteration
				} 
			} //End event list iterations
		} //End processing queue

		outputResults results_out = new outputResults();
		ArrayList<MTS> mtsList = new ArrayList<MTS>();
		mtsList.add(systemMTS);
		results_out.outputToMTSA(mtsList, "mtsOUT.lts");
		
	}
	
	public ArrayList<String> appendPostAll(ArrayList<String> vars, ArrayList<String> exprs){
		ArrayList<String> postExpr = new ArrayList<String>();
		
		for(String expr : exprs){
			postExpr.add(appendPost(vars, expr));
		}
		
		return postExpr;
	}
	
	public String appendPost(ArrayList<String> vars, String s){
		String postString = new String();
		postString = s+"";
		String replace="";
		for(String find : vars){
			replace = find+"_post";
			postString = postString.replace(find, replace);	
		}
		return postString;
	}

	private ArrayList<String> extractAtomic(String preCond){
		ArrayList<String> atomic = new ArrayList<String>();
		
		String[] atoms = preCond.split(" or ");
		String[] quarks;
		for(int i=0;i<atoms.length;i++){
			quarks = atoms[i].split(" and ");
			for(int j=0;j<quarks.length;j++){
				atomic.add(fixParens(quarks[j]));
			}
		}
		return atomic;
	}
	
	//Makes sure that the parens are equal
	private String fixParens(String s){
		String retVal = s+"";
		char c;
		int leftParen=0;
		int rightParen=0;
		for(int i=0; i<s.length(); i++){
			c = s.charAt(i);
			if(c=='(')
				leftParen++;
			if(c==')')
				rightParen++;
		}
		int diff = Math.abs(leftParen-rightParen);
		if(leftParen>rightParen){
			for(int i=0;i<diff;i++){
				retVal = retVal+")";
			}
		}
		else if(rightParen>leftParen){
			for(int i=0;i<diff;i++){
				retVal = "("+retVal;
			}
		}
		return retVal;
	}
	
	//d goes through each predicated
	//k goes through pred=true pred=false
	public void getMTSStates(int d, ArrayList<MTS_state> states, ArrayList<String> tempState) {
		  if (d == totalPredList.size()) {
			states.add(new MTS_state(states.size(),tempState));
		    return;
		  }
		  for (int k = 0; k < totalPredList.get(d).size(); k++) {
			  tempState.add(totalPredList.get(d).get(k));
			  getMTSStates(d + 1,states,tempState);
			  tempState.remove(tempState.size()-1);
		  }
		  return;
	}
	
	public String getPredicate(ArrayList<String> preCond){
		String pred;
		if(preCond.size()>1)
			pred = "("+preCond.get(0)+")";
		else
			pred = preCond.get(0);
		for(int i=1;i<preCond.size();i++){
			pred+=" and ("+preCond.get(i)+")";
		}
		return pred;
	}
	
	public String getPredicate(String preCond){		
		return "("+preCond+")";
	}

}
