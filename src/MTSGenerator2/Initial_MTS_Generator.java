 package MTSGenerator2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import DataTypes.AnalysisInstance;
import DataTypes.Event2;
import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;
import DataTypes.Scenario2;
import MTSGenerator.output.outputResults;
import daikon.PptName;
import daikon.VarInfo;
import daikon.inv.Invariant;

public class Initial_MTS_Generator {
	
    public ArrayList<ArrayList<String>> predicateList;
    public ArrayList<String> objectInvariants;
    private Converter c;
    private YicesRunner yicesRun;
    private String component_name;
	
	public MTS generateInitialMTS(AnalysisInstance instance)
	{
		yicesRun = new YicesRunner();
		predicateList = new ArrayList<ArrayList<String>>();
		objectInvariants = new ArrayList<String>();
		c = new Converter(instance);
		
		yicesRun.reset();
		
		component_name = instance.component_name;
		
		// Define variables
		yicesRun.defineVars(instance .var_names);
		
		for(VarInfo variable : instance.variables)
		{
			if (variable.type.baseIsBoolean() && !variable.name().startsWith("arg")
					&& !variable.name().contains("return"))
			{
				String preCond = "(or (= " + variable.name() + " 0) (= " 
					+ variable.name() + " 1))";
				String postCond = "(or (= " + variable.name() + "_post 0) (= " 
				+ variable.name() + "_post 1))";
				yicesRun.assertExpr(preCond.replace('.', '_'));
				yicesRun.assertExpr(postCond.replace('.', '_'));
			} /*else if (!variable.type.baseIsBoolean() && !variable.type.baseIsIntegral()) {
				String preCond = "(or (= " + variable.name() + " 0) (= " 
				+ variable.name() + " 1))";
				String postCond = "(or (= " + variable.name() + "_post 0) (= " 
				+ variable.name() + "_post 1))";
				yicesRun.assertExpr(preCond.replace('.', '_'));
				yicesRun.assertExpr(postCond.replace('.', '_'));
			}*/
		}
		
		System.out.println("\nObject Invariants");
		// Change object invariants to Yices format
		for (Invariant inv : instance.object_invariants) {
			String preCond = c.toYicesExpr(inv, true);
			String postCond = c.toYicesExpr(inv, false);
			yicesRun.assertExpr(preCond);
			yicesRun.assertExpr(postCond);
			objectInvariants.add(preCond);
			objectInvariants.add(postCond);
		}
		
		System.out.println("\nPredicate List:");
		// Initialize the predicate list
		for (Event2 e : instance.eventList)
		{
			for (String inv : e.getPreCond_str())
			{
				//Constructors should not have preconditions
				if(e.isConstructor()) continue;
				
				// Avoid predicates that involve method parameters
				if(!inv.contains("arg0") && !inv.contains("arg1") && !inv.contains("arg2") && !inv.contains("arg3") && !inv.contains("arg4")
						&& !inv.contains("arg5") && !inv.contains("arg6") && !inv.contains("arg7") && !inv.contains("arg8") && !inv.contains("arg9")) 
				{	
					addToPredicateList(inv);
				}
			}
		}
		
		// Create initial state of the MTS
//		ArrayList<String> initial_values = new ArrayList<String>();
		
//		System.out.println("\nInitial State:");
		Set<String> eventNames = instance.events.keySet();
		ArrayList<String> constructorNames = new ArrayList<String>();
		
		// Getting all the constructor entry points (most of the time it will have only one)
		for(String name : eventNames)
		{
			if(name.contains(component_name
					+ component_name.substring(component_name.lastIndexOf('.')) + "("))
			{
				constructorNames.add(name);
			}
		}
		
/*		Scenario2.Invocation initial = instance.scenario.getInvocation(0);
		for (VarInfo var : instance.variables)
		{
			String str = "(= " + c.toYicesExpr(var, true) + " " 
				+ c.toYicesExpr(initial.get_post_value(var.name())) + ")";
			initial_values.add(str);
			System.out.println(str);
		}
*/
		//System.exit(1);
			
		// Use post value off

		MTS componentMTS = new MTS("Invariant_based", instance.var_names, constructorNames);
		MTS_state initialState = componentMTS.getInitialState();
/*		componentMTS.addMTSState(initialState);
*/		
		// Enumerate all possible states
		ArrayList<MTS_state> processingStates = new ArrayList<MTS_state>();
/*		processingStates.add(initialState);
*/		getMTSStates(0, processingStates, new ArrayList<String>());

//		for (MTS_state s : processingStates){
//			System.out.println(s.getName() + ": ");
//			for(String vs : s.getVariableState()){
//				System.out.println(vs + " ");
//			}
//			System.out.println();
//		}
//		System.exit(1);
		//Processing Queue
		Queue<MTS_state> processingQueue = new LinkedList<MTS_state>(); // array of all possible legit paths of pre-state invariants
		
//		Use initial state to figure out which state to start in
//		yicesRun.push();
//		yicesRun.assertExpr(initialState.getVariableState());
		
		System.out.println("There are " + (processingStates.size() + 1) + " states"); // does all combinations consistency check
		
		//System.exit(0);
		
		//System.out.println("Assert: " + initialState.getVariableState());
		
		for (String name : constructorNames)
		{
			yicesRun.push();
			Event2 constructor = instance.events.get(name);
			yicesRun.assertExpr(constructor.getPostCond_str());
			
			if(yicesRun.isInconsistent()) {
				yicesRun.pop();
				continue;
			}
			
			for (MTS_state state : processingStates) {
				
				if (state.equals(initialState))
					continue;
				
				yicesRun.push();
				yicesRun.assertExpr(appendPostAll(instance.var_names, state.getVariableState()));
				
				//System.out.println("Assert: " + state.getVariableState());
				
				if (!yicesRun.isInconsistent()) {
					
					//System.out.println("Consistent");
					
					processingQueue.add(state);
					state.setProcessed();
					componentMTS.addMTSState(state);
					assert(constructor.getPptName()!=null);
					componentMTS.addMTSTransition(new MTS_transition(constructor.getPptName(), initialState.getName(), state.getName(), "true"));
				}
				//else
				//	System.out.println("Not consistent");
				
				yicesRun.pop();
			}
			
			yicesRun.pop();
		}
		
//		yicesRun.pop();
		
		
		//Iterate through queue of unprocessed states
		while (!processingQueue.isEmpty()) {
			MTS_state currState = processingQueue.poll();
			componentMTS.addMTSState(currState);
			
			yicesRun.push();
			//Add Current state expressions
			yicesRun.assertExpr(currState.getVariableState());
			
			if(yicesRun.isInconsistent()) {
				yicesRun.pop();
				continue;
			}
			
			for (Event2 event : instance.events.values()) {
				
				//Commented out because in very simple runs the pre/postconditions may
				// be empty even if the method has executed.
				//if (event.getPostCond().isEmpty() && event.getPreCond().isEmpty())
				//	continue;

				if (event.isConstructor())
					continue;
				
				yicesRun.push();
				//Add Event expressions
				yicesRun.assertExpr(event.getPreCond_str());
				if (!yicesRun.isInconsistent()) {
					for (MTS_state nextState : processingStates) {
						
						if (nextState.equals(initialState))
							continue;
						
						yicesRun.push();
						//Next State
						yicesRun.assertExpr(appendPostAll(instance.var_names, nextState.getVariableState()));
						yicesRun.assertExpr(event.getPostCond_str());
						
						if (!yicesRun.isInconsistent()) {
							//yicesRun.dumpContext();
							//System.out.println(currState.getVariableState());
							//System.out.println("  "+eventList.get(i).getName()+" -> State "+nextState.getName());
							//Add MTS Transition
							assert(event.getPptName()!=null);
							componentMTS.addMTSTransition(new MTS_transition(event.getPptName(), currState.getName(), nextState.getName(), "true"));
							//Add to queue
							if(!nextState.isProcessed()){
								nextState.setProcessed();
								processingQueue.add(nextState);
							}
						}
						yicesRun.pop();
					} //End state iteration
				}
				yicesRun.pop();
			}
			yicesRun.pop();
		}
		
		for (MTS_state state : componentMTS.getAllStates())
		{
			System.out.println(state.getName() + ": " + state.getVariableState());
		}

		outputResults results_out = new outputResults();
		ArrayList<MTS> mtsList = new ArrayList<MTS>();
		mtsList.add(componentMTS);
		results_out.outputToMTSA(mtsList, "mtsOUT.lts");
				
		return componentMTS;
	}
	
	public void addToPredicateList(String y)
	{
		if (!isInPredicateList(y) && !isObjectInvariant(y))
		{
			ArrayList<String> predicate = new ArrayList<String>();
			predicate.add(y);
			predicate.add("(not " + y + ")");
			predicateList.add(predicate);
			
			System.out.println(y);
		}
	}
	
	public boolean isObjectInvariant(String y)
	{
		for (String x : objectInvariants)
			if (x.equals(y))
				return true;
		return false;
	}
	
	public boolean isInPredicateList(String y)
	{
		for (ArrayList<String> definedPred : predicateList){
			if (definedPred.get(0).equals(y)){
				return true;
			}
		}
		return false;
	}
	
	// The recursion should be explained in comments, but it seems to work fine
	// d goes through each predicate
	// k goes through pred = true pred = false
	/*
	* pred in predlist are possible statements. This searches every paths (2^invs) for consistent invariant paths)
	* */
	public void getMTSStates(int d, ArrayList<MTS_state> states, ArrayList<String> tempState)
	{
		if (d == predicateList.size()) {
			states.add(new MTS_state(states.size() + 1, tempState));
		}
		else {
			ArrayList<String> predicate = predicateList.get(d);
			
			for (int k = 0; k < predicate.size(); k++) {	
				String assignment = predicate.get(k);
				yicesRun.push();
				yicesRun.assertExpr(assignment);
				
				if (!yicesRun.isInconsistent()) {
					tempState.add(assignment);
					getMTSStates(d + 1, states, tempState);
					tempState.remove(tempState.size()-1);
				}
				yicesRun.pop();
			}
		}
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
			replace = find + "_post";
			if (! postString.contains("size_" + find)) {
				postString = postString.replace(find, replace);
			}
		}
		return postString;
	}
	
	public YicesRunner getYicesContext()
	{
		return yicesRun;
	}
	
	public Converter getConverter()
	{
		return c;
	}
}