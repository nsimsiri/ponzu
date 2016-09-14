package MTSGenerator;

import java.util.*;

import DataTypes.Component;
import DataTypes.ComponentScenarios;
import DataTypes.Event;
import DataTypes.MTS;
import DataTypes.Scenario;
import DataTypes.exceptions.AnnotationDiscrepancyException;
import DataTypes.exceptions.InputException;
import DataTypes.exceptions.MTSstateException;
import DataTypes.exceptions.ScenarioException;
import MTSGenerator.algorithm.Component_constraint_generation;
import MTSGenerator.algorithm.FinalMTS_Generator;
import MTSGenerator.algorithm.Initial_MTS_Generator;
import MTSGenerator.algorithm.ScenarioAnnotator;
import MTSGenerator.analysis.AnnotationAnalysis;
import MTSGenerator.analysis.InputAnalysis;
import MTSGenerator.analysis.MTSanalysis;
import MTSGenerator.initialization.Component_Generator;
import MTSGenerator.initialization.ConstraintSet_Generator;
import MTSGenerator.initialization.Scenario_Extractor;
import MTSGenerator.output.outputResults;

public class Generator_app {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
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
		
		//Extension of the input files
		String systemDir = "\\StressTest7";
		String extension = "_test";
		
		//Timing the synthesis
		int timeStart, timeEnd, overallTime;
		
		//Reading constraint set and events from a specially formatted XML file 
		ConstraintSet_Generator constraintReader = new ConstraintSet_Generator();
		eventList = constraintReader.extractConstraints(".\\src\\Input_files" + systemDir + "\\constraints" + extension + ".xml", variable_names);
		
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
	}
}
