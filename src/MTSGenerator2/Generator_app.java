package MTSGenerator2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DataTypes.AnalysisInstance;
import DataTypes.MTS;
import MTSGenerator.output.outputResults;
import MTSGenerator2.*;

public class Generator_app {
	private static boolean WRITE_TRACES = false;

	public static void main(String[] args)
	{
		// Write out plaintext traces
		if (WRITE_TRACES)
		{
			createPlaintextTraces(args);
			System.exit(0);
		}

		// Get args, e.g.:
		// java.util StringTokenizer eval_organized/StringTokenizer StringTokenizer.inv.gz [[tracesBelow]]
		String packagename = args[0];
		String classname = args[1];
		String filepath = args[2];
		String invpath = filepath + "/" + args[3];

		// Get trace args, e.g.,:
		// [[argsAbove]] jEdit.dtrace.gz jEdit2.dtrace.gz StandalonePlayer.dtrace.gz
		List<String> tracepaths = new LinkedList<>();
		for (int i = 4; i < args.length; i++)
		{
			tracepaths.add(filepath + "/" + args[i]);
		}

		// String tracepath = args[0];
		// String invpath = args[1];
		// String packagename = args[2];
		// String classname = args[3];
		// String filepath = args[4] + "/";
		// //
		// String invariants_file = "StackArTester.inv.gz";
		// String traces_file = "";
		// String component_name = "DataStructures.StackAr";
		//
		// // Output file
		// String output_file = "StackArInvariants3.txt";
		//

		// String classname = "StackAr";
		// String filepath = "evaluation/NEW/Socket/";
		// String packagename = "DataStructures";
		// String tracename = "StackAr";
		//
		// String tracepath = filepath + tracename + ".dtrace.gz";
		// String invpath = filepath + tracename + ".inv.gz";

		InvParser parser = new InvParser(invpath);
		AnalysisInstance x = parser.parse(packagename + "." + classname);
		x.printInvariants(filepath + classname + "-INV.txt");

		// System.out.println("Invariants Parsed...");

		for (String tracepath : tracepaths)
		{
			new TraceParser(tracepath, classname, x);
		}

		// System.out.println("Traces Parsed...");

		// This code eliminates supposedly private methods (those with event
		// counts in the traces equal to 0). This may be modified by feeding in
		// the list of private methods.
		x.eliminatePrivate();

		Initial_MTS_Generator init_gen = new Initial_MTS_Generator();
		MTS invariant_based_MTS = init_gen.generateInitialMTS(x);

		ArrayList<MTS> outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(invariant_based_MTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + "_inva_based_2" + ".lts");

		TraceAnalyzer trace_analysis = new TraceAnalyzer(x, invariant_based_MTS, init_gen.getYicesContext(),
		        init_gen.getConverter());

		MTS traditionalKTailMTS = trace_analysis.traditionalKTail(1);
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(traditionalKTailMTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + traditionalKTailMTS.getName() + ".lts");

		System.out.println("Created model: " + traditionalKTailMTS);

		MTS enhancedKTailMTS = trace_analysis.kTailEnhanced(1);
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(enhancedKTailMTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + enhancedKTailMTS.getName() + ".lts");

		System.out.println("Created model: " + enhancedKTailMTS);

		MTS traditionalKTailMTS2 = trace_analysis.traditionalKTail(2);
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(traditionalKTailMTS2);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + traditionalKTailMTS2.getName() + ".lts");

		System.out.println("Created model: " + traditionalKTailMTS2);

		MTS enhancedKTailMTS2 = trace_analysis.kTailEnhanced(2);
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(enhancedKTailMTS2);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + enhancedKTailMTS2.getName() + ".lts");

		System.out.println("Created model: " + enhancedKTailMTS2);

		MTS refinedMTS = trace_analysis.refinementStrategy("Refinement");
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(refinedMTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + refinedMTS.getName() + ".lts");

		System.out.println("Created model: " + refinedMTS);
		
		// /////////////////////////////////////////////////////////////
		
		
		//
		// String invariants_file = "StackArTester.inv.gz";
		// String traces_file = "";
		// String component_name = "DataStructures.StackAr";
		//
		// // Output file
		// String output_file = "StackArInvariants3.txt";
		//
		// //Timing the synthesis
		// int timeStart, timeEnd, overallTime;
		//
		// ArrayList<Event> eventList = new ArrayList<Event>();
		// ArrayList<String> variable_names = new ArrayList<String>();
		
		//InvParser inv_parser = new InvParser();
		//inv_parser.parse(invariants_file, component_name, eventList, variable_names);
	}
	
	/*public static void main(String[] args) {
		
		//List of events
		ArrayList<Event> eventList = new ArrayList<Event>();	
		
		//List of components
		ArrayList<Component> componentList = new ArrayList<Component>();
		
		//List of scenarios
		ArrayList<Scenario> scenarioList = new ArrayList<Scenario>();
		
		//System state variables and their initial values
		HashMap<String,String> variable_names = new HashMap<String,String>();
		
		//Scenarios annotated from components' perspectives
		ArrayList<ComponentScenarios> annotated_scenarios = new ArrayList<ComponentScenarios>();
		
		//Scenarios annotated from system perspective
		ComponentScenarios systemScenarios = null;
		
		//List of component MTSs
		ArrayList<MTS> initialMTSs = null;
		
		
		
		
		
		//Reading component provided interface signatures from a specially formatted XML file
		Component_Generator componentReader = new Component_Generator();
		componentList = componentReader.extractComponents(".\\src\\Input_files" + systemDir + "\\components" + extension + ".xml");
		
		//Reading scenarios from a specially formatted XML file
		Scenario_Extractor scenarioReader = new Scenario_Extractor();
		scenarioList = scenarioReader.extractScenarios(".\\src\\Input_files" + systemDir + "\\scenarios" + extension + ".xml", eventList);
		
		timeStart = (int) System.currentTimeMillis();
		
		//Generating component-level constraints from system-level constraints
		Component_constraint_generation compConstraints = new Component_constraint_generation();
		compConstraints.generate_complevel_constraints(eventList, scenarioList, componentList, variable_names);
		
		//Block that analyzes inputs for inconsistencies
		InputAnalysis analyzeInputs = new InputAnalysis();
		try {
			analyzeInputs.Analyze(componentList, eventList);
		} catch (InputException e1) {
			ArrayList<String> messages = e1.getMessages();
			for(int i = 0; i < messages.size(); i++)
			{
				System.err.println("Warning: " + messages.get(i));
			}
		}
		
		//Generating the initial MTS from the available component-level constraints
		Initial_MTS_Generator initMTSs = new Initial_MTS_Generator();
		initialMTSs = initMTSs.generateInitialMTS(eventList, componentList, variable_names);
		
		//Test if the same combinations of overlapping system state variable values occur
		MTSanalysis MTSStateAnalysis = new MTSanalysis();
		try {
			MTSStateAnalysis.analyzeMTSs(initialMTSs);
		} catch (MTSstateException e1) {
			ArrayList<String> messages = e1.getMessages();
			for(int i = 0; i < messages.size(); i++)
			{
				System.err.println("Warning: " + messages.get(i));
			}
		}
		
		ScenarioAnnotator annotate = new ScenarioAnnotator();
		try {
			//Annotating scenarios for every component
			annotated_scenarios = annotate.annotateScenarios(componentList, scenarioList, eventList, variable_names);
		} catch (ScenarioException e) {
			System.err.println("Error: " + e.getName());
			System.err.println("This happened in scenario " + e.getScenarioName() + " while annotating for component " + e.getComponentName());
			System.err.println("The problem was the following result in annotation for " + e.getVarName() + " variable: ");
			String type = e.getType();
			Scenario compScenario = e.getScenario();
			int occurrenceIndex = e.getEventPosition();
			for(int i = 0; i < compScenario.size(); i++)
			{
				if(i == occurrenceIndex && type.equals("prepost"))
				{
					System.err.print("<" + e.getPreAnnotation().get(e.getAnnotationIndex()) + "> -> " + compScenario.getEvent(i).getName() + " -> <" + e.getPostAnnotation().get(e.getAnnotationIndex()) + "> -> ");
				}
				else if(i == occurrenceIndex && type.equals("postpre"))
				{
					System.err.print(compScenario.getEvent(i).getName() + " -> <" + e.getPostAnnotation().get(e.getAnnotationIndex()) + "> -> " + "<" + e.getPreAnnotation().get(e.getAnnotationIndex()) + "> -> ");
				}
				else System.err.print(compScenario.getEvent(i).getName() + "-> ");
			}
			System.exit(-1);
		}
				
		boolean sysExceptionFound = false;
		try {
			//Annotating scenarios for the system perspective
			systemScenarios = annotate.systemScenario(scenarioList, eventList, variable_names);
		} catch (ScenarioException e) {
			sysExceptionFound = true;
			System.err.println("Warning: A discrepancy which would prevent a scenario from executing from system's perspective, but can actually execute from the perspective of the components");
			System.err.println("This would happen in scenario " + e.getScenarioName());
			System.err.println("The problem was the following result in annotation for " + e.getAnnotationIndex() + " variable: ");
			String type = e.getType();
			Scenario compScenario = e.getScenario();
			int occurrenceIndex = e.getEventPosition();
			for(int i = 0; i < compScenario.size(); i++)
			{
				if(i == occurrenceIndex && type.equals("prepost"))
				{
					System.err.print("<" + e.getPreAnnotation().get(e.getAnnotationIndex()) + "> -> " + compScenario.getEvent(i).getName() + " -> <" + e.getPostAnnotation().get(e.getAnnotationIndex()) + "> -> ");
				}
				else if(i == occurrenceIndex && type.equals("postpre"))
				{
					System.err.print(compScenario.getEvent(i).getName() + " -> <" + e.getPostAnnotation().get(e.getAnnotationIndex()) + "> -> " + "<" + e.getPreAnnotation().get(e.getAnnotationIndex()) + "> -> ");
				}
				else System.err.print(compScenario.getEvent(i).getName() + "-> ");
			}
		}

		//Check if there are inconsistencies (pre-sending vs. pre-system and post-sending vs. post-receiving vs. post-system)
		AnnotationAnalysis analyzeAnnotations = new AnnotationAnalysis();
		try {
			if(sysExceptionFound == false) analyzeAnnotations.Analyze(annotated_scenarios, systemScenarios, componentList, variable_names);
		} catch (AnnotationDiscrepancyException e) {
			ArrayList<String> messages = e.getMessages();
			//Display all the warnings regarding the annotations
			for(int i = 0; i < messages.size(); i++)
			{
				System.err.println("Warning: " + messages.get(i));
			}
		}
		
		timeEnd = (int) System.currentTimeMillis();
		overallTime = timeEnd - timeStart;
		
		System.out.println("Time before the generation of final MTSs is: " + overallTime);
		
		//Output code
		outputResults writeResults = new outputResults();
		writeResults.outputMTSs(initialMTSs, ".\\src\\Output_files" + systemDir + "\\initial_MTSs" + extension + ".xml");
		
		timeStart = (int) System.currentTimeMillis();		
		
		//Synthesize the final MTSs by refining initial MTSs according to scenarios
		FinalMTS_Generator MTSGenerator = new FinalMTS_Generator();
		MTSGenerator.generateMTS(initialMTSs, annotated_scenarios);
				
		timeEnd = (int) System.currentTimeMillis();
		overallTime += timeEnd - timeStart;
		System.out.println("Overall synthesis time is: " + overallTime);
		
		int overallStateNumber = 0;
		double average;
		for(int i = 0; i < initialMTSs.size(); i++) {
			overallStateNumber += initialMTSs.get(i).getStateSize();
		}
		average = (double) overallStateNumber / initialMTSs.size();
		System.out.println("Average number of states in the final MTSs is: " + average);
		
		writeResults.outputComponentConstraints(componentList, ".\\src\\Output_files" + systemDir + "\\component_constraints" + extension + ".xml");
		writeResults.outputAnnotatedScenarios(annotated_scenarios, componentList, variable_names, ".\\src\\Output_files" + systemDir + "\\annotated_scenarios" + extension + ".xml");
		writeResults.outputMTSs(initialMTSs, ".\\src\\Output_files" + systemDir + "\\final_MTSs" + extension + ".xml");
		writeResults.outputToMTSA(initialMTSs, ".\\src\\Output_files" + systemDir + "\\final_MTSs" + extension + ".lts");
	}*/

	/**
	 * Parse traces and invariants files, then write out plaintext traces of all
	 * invariant invocations
	 * 
	 * @param args
	 *            Args format is
	 *            "tracepath invpath packagename classname outpath" (e.g.,
	 *            StackAr.dtrace.gz StackAr.inv.gz DataStructures StackAr
	 *            traces.txt)
	 */
	private static void createPlaintextTraces(String[] args)
	{
		String tracepath = args[0];
		String invpath = args[1];
		String packagename = args[2];
		String classname = args[3];
		String outpath = args[4] + "/";

		InvParser parser = new InvParser(invpath);
		AnalysisInstance x = parser.parse(packagename + "." + classname);

		TraceParser.dumpInvocations(tracepath, classname, x, outpath);

		System.out.println("Traces parsed.");
	}
}
