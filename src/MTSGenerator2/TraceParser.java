// TODO: Introduce levels of splitting in MTS refinement steps.

/*		Old Results 
 * 
 * 		InvParser parser = new InvParser("jEdit.inv.gz");
		AnalysisInstance x = parser.parse("org.gjt.sp.jedit.buffer.UndoManager");
		
		InvParser inv_parser = new InvParser("StackArTester.inv.gz");
		AnalysisInstance x = inv_parser.parse("DataStructures.StackAr");
		
		InvParser parser = new InvParser("StandalonePlayer.inv.gz");
		AnalysisInstance x = parser.parse("javazoom.jlgui.player.amp.playlist.BasePlaylist");
		
		InvParser parser = new InvParser("jEdit2.inv.gz");
		AnalysisInstance x = parser.parse("org.gjt.sp.jedit.browser.VFSBrowser");
*/

package MTSGenerator2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

import DataTypes.AnalysisInstance;
import DataTypes.Event2;
import DataTypes.MTS;
import MTSGenerator.output.outputResults;
import TestGenerator.ModbatModelGenerator;
import TestGenerator.TestGenerator;
import TestGenerator.InvocationSequence;
import StackAr.DataStructures.*;
import daikon.FileIO;
import daikon.PptMap;
import daikon.PptTopLevel;
import daikon.ValueTuple;
import daikon.VarInfo;


public class TraceParser {
	
	List<Long> badIDs = new ArrayList<Long>();
	boolean verbose = false;

	public static void main(String[] args)
	{
		String classname = args[0];
		String filepath = args[1];
		String packagename = args[2];
		String tracename = args[3];

		// String classname = "ElemNumber$NumberFormatStringTokenizer";
		// String filepath = "evaluation/NEW2/NumberFormatStringTokenizer/";
		// String tracename = "StringTokenizerDaCapo";
		// String packagename = "org.apache.xalan.templates";

		// String classname = "SftpConnection";
		// String filepath = "evaluation/sftpconnection/";
		// String tracename = "SftpConnection";
		// String packagename = "net.sf.jftp.net.wrappers";

		// String classname = "Signature";
		// String filepath = "evaluation/signature-all/";
		// String tracename = "Signature";
		// String packagename = "java.security";

		// String classname = "SMTPProtocol";
		// String filepath = "evaluation/smtpprotocol-all/";
		// String tracename = "SMTPProtocol";
		// String packagename = "org.columba.ristretto.smtp";
		
//		String classname = "Socket";
//		String filepath = "eval_organized/Socket/";
//		String packagename = "java.net";
//		String tracename = "Socket";
		
//		String classname = "StackAr";
//		String filepath = "eval_organized/StackAr/";
//		String packagename = "DataStructures";
//		String tracename = "StackAr";

		/*2016 New Daikon Output*/

//		String classname = "StackAr";
//		String filepath = "input_generator/StackAr/NewDaikonOutput/";
//		String packagename = "DataStructures";
//		String tracename = "StackAr";

		// String classname = "StringTokenizer";
		// String filepath = "evaluation/jedit-comprehensive/";
		// String tracename = "jEdit";
		// String packagename = "org.gjt.sp.javautils";
		
//		String classname = "StringTokenizer";
//		String filepath = "evaluation/randomized/stringtokenizer/";
//		String tracename = "StringTokenizer";
//		String packagename = "java.util";

		// String classname = "ToHTMLStream";
		// String filepath = "evaluation/tohtmlstream/";
		// String tracename = "ToHTMLStream";
		// String packagename = "org.apache.xml.serializer";
	
		// String classname = "ZipOutputStream";
		// String filepath = "evaluation/jarinstaller/";
		// String tracename = "ZipOutputStream";
		// String packagename = "java.util.zip";

		InvParser parser = new InvParser(filepath + tracename + ".inv.gz");

		AnalysisInstance x = parser.parse(packagename + "." + classname);
		x.printInvariants(filepath + classname + "-INV.txt");

		System.out.println("Invariants Parsed...");
		System.exit(1);
		
		// ---- NFST ----
		// new TraceParser(filepath + "StringTokenizerDaCapo.dtrace.gz", "1", x);

		// ---- SOCKET ----
//		new TraceParser(filepath + "Socket1.dtrace.gz", "Socket1", x);
//		new TraceParser(filepath + "Socket2.dtrace.gz", "Socket2", x);
//		// new TraceParser(filepath + "Socket3.dtrace.gz", "Socket3", x);
//		new TraceParser(filepath + "Socket4.dtrace.gz", "Socket4", x);
//		// new TraceParser(filepath + "Socket5.dtrace.gz", "Socket5", x);
//		// new TraceParser(filepath + "Socket6.dtrace.gz", "Socket6", x);
//		new TraceParser(filepath + "Socket7.dtrace.gz", "Socket7", x);
//		// new TraceParser(filepath + "Socket8.dtrace.gz", "Socket8", x);
//		// new TraceParser(filepath + "Socket9.dtrace.gz", "Socket9", x);
//		new TraceParser(filepath + "Socket10.dtrace.gz", "Socket10", x);
//		new TraceParser(filepath + "Socket11.dtrace.gz", "Socket11", x);
//		new TraceParser(filepath + "Columba.dtrace.gz", "Columba1", x);
//		new TraceParser(filepath + "Columba2.dtrace.gz", "Columba2", x);
//		new TraceParser(filepath + "Columba3.dtrace.gz", "Columba3", x);
//		new TraceParser(filepath + "Columba4.dtrace.gz", "Columba4", x);
//		// new TraceParser(filepath + "Columba5.dtrace.gz", "Columba5", x);
//		new TraceParser(filepath + "Columba6.dtrace.gz", "Columba6", x);

		// Parse and obtain traces
		new TraceParser(filepath + tracename + ".dtrace.gz", classname, x);

		System.out.println("Traces Parsed...");
		Function<Void, Void> memLook = (Void v) -> {
			Runtime rt = Runtime.getRuntime();
			System.out.printf("\n----MEMORY USAGE----\nFree=%s bytes\nMax=%s bytes\nTotal=%s bytes\n\n", rt.freeMemory(), rt.maxMemory(), rt.totalMemory());
			return null;
		};

		// This code eliminates supposedly private methods (those with event
		// counts in the traces equal to 0). This may be modified by feeding in
		// the list of private methods.
		x.eliminatePrivate();

		//Ponzu evaluation - output intitial traces for eval.
//		InvocationSequence.outputTraceGraph(x.eventList, filepath+classname + "_trace_graph.log");
		x.scenario.dumpInvocation(filepath+tracename+"_trace.log", false);
		x.scenario.printInvocationGraphDotFile(filepath, x);

		Initial_MTS_Generator init_gen = new Initial_MTS_Generator();
		MTS invariant_based_MTS = init_gen.generateInitialMTS(x);
		TraceAnalyzer.annotateTransitionWithInvariants(invariant_based_MTS, x);

		ArrayList<MTS> outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(invariant_based_MTS);
		(new outputResults(x)).outputToMTSA(outputMTSs, filepath + classname + "_inva_based_2" + ".lts");
		(new outputResults()).outputToMTSADOT(outputMTSs, filepath + classname + "_inva_based_2" + ".dot");
		memLook.apply(null);

/* Natcha's Test Generator
		TestGenerator testGen = new TestGenerator(invariant_based_MTS, new StackArTestGen().new StackArTest());
		testGen.genRandomTestCases(10);
*/

/* Modbat Test Generator
*/
// used for Testing . deleting soon....
//		ModbatModelGenerator modbadGen = new ModbatModelGenerator(invariant_based_MTS, "StackAr");
//		modbadGen.ouputScalaModel(filepath + classname + "_inva_based_2", "StackAr.DataStructures");

//		ModbatModelGenerator modbadGen = new ModbatModelGenerator(invariant_based_MTS, packagename, classname);
//		modbadGen.ouputScalaModel(filepath + classname + "_inva_based_2", packagename);
		System.out.println("[TRACE PARSER] - Trace Proprocessing done.. creating initialTraceModel");
		TraceAnalyzer trace_analysis = new TraceAnalyzer(x, invariant_based_MTS,
															init_gen.getYicesContext(), init_gen.getConverter());
		MTS initialTraceModel = trace_analysis.getInitialTraceModel();
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(initialTraceModel);
		(new outputResults()).outputToMTSADOT(outputMTSs, filepath + classname + "_initial_trace_model" + ".dot");
		System.out.println("[TRACE PARSER] - created initialTraceModel.. creating SEKT next.");
		memLook.apply(null);

/* Traditional KTails ---------------------------------------------------------------------------------------------------
		MTS traditionalKTailMTS = trace_analysis.traditionalKTail(1);
		TraceAnalyzer.annotateTransitionWithInvariants(traditionalKTailMTS, x);
		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(traditionalKTailMTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + traditionalKTailMTS.getName() + ".lts");
		(new outputResults()).outputToMTSADOT(outputMTSs,filepath + classname + traditionalKTailMTS.getName() + ".dot");

*/
/* State Enhanced KTails (SEKT) ---------------------------------------------------------------------------------------------------
*/
        MTS enhancedKTailMTS = trace_analysis.kTailEnhanced(1);
        TraceAnalyzer.annotateTransitionWithInvariants(enhancedKTailMTS, x);
        outputMTSs = new ArrayList<MTS>();

		 outputMTSs.add(enhancedKTailMTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + enhancedKTailMTS.getName() + ".lts");
		(new outputResults()).outputToMTSADOT(outputMTSs,filepath + classname + enhancedKTailMTS.getName()+ ".dot");

		// Generating Scala model for mobbat.
		ModbatModelGenerator modbadGen = new ModbatModelGenerator(enhancedKTailMTS, packagename, classname);
		modbadGen.ouputScalaModel(filepath, packagename);

		 System.out.println("Created model: " + enhancedKTailMTS);

/*Natcha's Test Gen ---------------------------------------------------------------------------------------------------
		TestGenerator testGen = new TestGenerator(enhancedKTailMTS, new StackArTestGen().new StackArTest());
		testGen.genRandomTestCases(10);
*/
/*		Uncomment when ready to roll

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
*/

/*		outputMTSs = new ArrayList<MTS>();
		outputMTSs.add(traditionalKTailMTS);
		outputMTSs.add(enhancedKTailMTS);
		outputMTSs.add(traditionalKTailMTS2);
		outputMTSs.add(enhancedKTailMTS2);
		outputMTSs.add(refinedMTS);
		outputMTSs.add(invariant_based_MTS);
		(new outputResults()).outputToMTSA(outputMTSs, filepath + classname + ".lts");
*/		
/*		AutomataComparison.compareAutomata(traditionalKTailMTS, refinedMTS, 1000, 100);
		AutomataComparison.compareAutomata(traditionalKTailMTS, refinedMTS, 1000, 10);
		AutomataComparison.compareAutomata(enhancedKTailMTS, refinedMTS, 1000, 100);
		AutomataComparison.compareAutomata(enhancedKTailMTS, refinedMTS, 1000, 10);
		AutomataComparison.compareAutomata(traditionalKTailMTS, enhancedKTailMTS, 1000, 100);
		AutomataComparison.compareAutomata(traditionalKTailMTS, enhancedKTailMTS, 1000, 10);
*/		
/*		MTS traditionalKTailMTS = null;
		MTS traditionalKTailMTS2 = null;
		MTS enhancedKTailMTS = null;
		MTS enhancedKTailMTS2 = null;
		MTS invariant_based_MTS_2 = null;
		MTS refinedMTS = null;
		MTS refinedMTS2 = null;
		
		MTS groundTruth = null;
		
		try {
			traditionalKTailMTS = FSPParser.parse("evaluation/voldemort/socket/SocketTraditional_1_tail.lts");
			traditionalKTailMTS2 = FSPParser.parse("evaluation/voldemort/socket/SocketTraditional_2_tail.lts");
			enhancedKTailMTS = FSPParser.parse("evaluation/voldemort/socket/SocketEnhanced_1_tail.lts");
			enhancedKTailMTS2 = FSPParser.parse("evaluation/voldemort/socket/SocketEnhanced_2_tail.lts");
			invariant_based_MTS_2 = FSPParser.parse("evaluation/voldemort/socket/Socket_inva_based.lts");
			refinedMTS = FSPParser.parse("evaluation/voldemort/socket/SocketRefinement.lts");
			refinedMTS2 = FSPParser.parse("evaluation/voldemort/socket/SocketRefinementPess.lts");
			
			sTruth = FSPParser.parse("evaluation/groundtruth/socket/Socket.lts");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		AutomataComparison.compareAutomata(traditionalKTailMTS, groundTruth, 100000, 10);
		AutomataComparison.compareAutomata(traditionalKTailMTS2, groundTruth, 100000, 10);
		AutomataComparison.compareAutomata(enhancedKTailMTS, groundTruth, 100000, 10);
		AutomataComparison.compareAutomata(enhancedKTailMTS2, groundTruth, 100000, 10);
		AutomataComparison.compareAutomata(invariant_based_MTS_2, groundTruth, 100000, 10);
		AutomataComparison.compareAutomata(refinedMTS, groundTruth, 100000, 10);
		AutomataComparison.compareAutomata(refinedMTS2, groundTruth, 100000, 10);
*/		

		/*
		MTS traditionalKTailMTS = null;
		MTS traditionalKTailMTS2 = null;
		MTS enhancedKTailMTS = null;
		MTS enhancedKTailMTS2 = null;
		MTS invariant_based_MTS = null;
		MTS refinedMTS = null;
		
		MTS groundTruth = null;
		MTS groundTruth2 = null;
		
		try {
			traditionalKTailMTS = FSPParser.parse("evaluation/dacapo-stringtokenizer/ElemNumber$NumberFormatStringTokenizerTraditional_1_tail.lts");
			traditionalKTailMTS2 = FSPParser.parse("evaluation/dacapo-stringtokenizer/ElemNumber$NumberFormatStringTokenizerTraditional_2_tail.lts");
			enhancedKTailMTS = FSPParser.parse("evaluation/dacapo-stringtokenizer/ElemNumber$NumberFormatStringTokenizerEnhanced_1_tail.lts");
			enhancedKTailMTS2 = FSPParser.parse("evaluation/dacapo-stringtokenizer/ElemNumber$NumberFormatStringTokenizerEnhanced_2_tail.lts");
			invariant_based_MTS = FSPParser.parse("evaluation/dacapo-stringtokenizer/ElemNumber$NumberFormatStringTokenizer_invariant_based.lts");
			refinedMTS = FSPParser.parse("evaluation/dacapo-stringtokenizer/ElemNumber$NumberFormatStringTokenizerRefinementPess.lts");
			
			groundTruth = FSPParser.parse("evaluation/groundtruth/stringtokenizer/NumberFormatStringTokenizer.lts");
			groundTruth2 = FSPParser.parse("evaluation/groundtruth/stringtokenizer/NumberFormatStringTokenizerCorrect.lts");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
//		AutomataComparison.compareAutomata(traditionalKTailMTS, groundTruth, 10000, 20);
		AutomataComparison.compareAutomata(traditionalKTailMTS, groundTruth2, 100000, 10);
//		AutomataComparison.compareAutomata(traditionalKTailMTS2, groundTruth, 10000, 20);
		AutomataComparison.compareAutomata(traditionalKTailMTS2, groundTruth2, 100000, 10);
//		AutomataComparison.compareAutomata(enhancedKTailMTS, groundTruth, 10000, 20);
		AutomataComparison.compareAutomata(enhancedKTailMTS, groundTruth2, 100000, 10);
//		AutomataComparison.compareAutomata(enhancedKTailMTS2, groundTruth, 10000, 20);
		AutomataComparison.compareAutomata(enhancedKTailMTS2, groundTruth2, 100000, 10);
//		AutomataComparison.compareAutomata(invariant_based_MTS, groundTruth, 10000, 20);
		AutomataComparison.compareAutomata(invariant_based_MTS, groundTruth2, 100000, 10);
//		AutomataComparison.compareAutomata(refinedMTS, groundTruth, 10000, 20);
		AutomataComparison.compareAutomata(refinedMTS, groundTruth2, 100000, 10);
*/

		
//		MTS groundTruth = null;
//		
//		try {
//			groundTruth = FSPParser.parse("evaluation/groundtruth/zipoutputstream/ZipOutputStreamSpec.lts");
//		} catch (IOException e) {
//			System.out.println(e.getMessage());
//			System.exit(1);
//		}
//		
//		AutomataComparison.compareAutomata(traditionalKTailMTS, groundTruth, 1000, 10);
//		AutomataComparison.compareAutomata(traditionalKTailMTS2, groundTruth, 1000, 10);
//		AutomataComparison.compareAutomata(enhancedKTailMTS, groundTruth, 1000, 10);
//		AutomataComparison.compareAutomata(enhancedKTailMTS2, groundTruth, 1000, 10);
//		AutomataComparison.compareAutomata(invariant_based_MTS, groundTruth, 1000, 10);
//		AutomataComparison.compareAutomata(refinedMTS, groundTruth, 1000, 10);
	}
	
	/**
	 * Write out plaintext traces of all invocations
	 * 
	 * @param outpath
	 *            Path to output directory. Traces will be written to
	 *            outpath/tracename.log
	 */
	public static void dumpInvocations(String filename, String tracename,
	        AnalysisInstance instance, String outpath)
	{
		new TraceParser(filename, tracename, instance);

		instance.scenario.dumpInvocations(outpath + tracename + ".log");
	}

	private String tracename;
	private PptMap ppts;
	private Stack<EntryInfo> entryStack;
	private HashMap<Long,EntryInfo> currentEntry = new HashMap<Long, EntryInfo>();
	private HashMap<String, HashMap<String, Object>> lastTuples;
	private AnalysisInstance instance;
	private HashMap<Long, Long> callLevel = new HashMap<Long, Long>();
    private int constructorStackCount = 0;

	public TraceParser(String filename, String tracename, AnalysisInstance instance)
	{
		System.out.println(tracename);
		this.tracename = tracename;
		ppts = new PptMap();
		entryStack = new Stack<EntryInfo>();
		lastTuples = new HashMap<String, HashMap<String, Object>>();
		this.instance = instance;
		//callLevel = 0;

		// Load program points map object from the .inv file
		try {
			FileIO.read_data_trace_file(filename, ppts, new TraceProcessor(), false, true);
			
		} catch (Exception e) {
			System.out.println("Exception caught (" + e.getClass().getSimpleName() 
					+ ") : " + e.getMessage());
			System.exit(1);
		}
		
		// This is a check that Daikon wants you to do just for safety
		ppts.repCheck();
		if (verbose){
			System.out.println(ppts);
			for(PptTopLevel p : ppts.ppt_all_iterable()){
				System.out.println(p);
			}
		}

	}
	
	public void printAllVariables()
	{
		Iterator<PptTopLevel> i = ppts.pptIterator();
		while ( i.hasNext() )
        {
			PptTopLevel ppt = i.next();
			if (InvParser.isInComponent(ppt, instance.component_name))
			{
				System.out.println(ppt.name);
				System.out.println("  " + ppt.varNames());
			}
        }
	}
	
	private class EntryInfo {
		
		PptTopLevel ppt;
		ValueTuple vt;
		Integer nonce;
		
		public EntryInfo(PptTopLevel ppt, ValueTuple vt, Integer nonce)
		{
			this.ppt = ppt;
			this.vt = vt;
			this.nonce = nonce;
		}
	}

	private class TraceProcessor extends FileIO.Processor {

        /**
         * @param ppt: Program point. Datastructure for the entering/exiting a function call.
         * @param vt: "values" of objects associated at the ppt
         *
         *
         * */
		@Override
		public void process_sample(PptMap all_ppts, PptTopLevel ppt,ValueTuple vt, /*@Nullable*/ Integer nonce)
		{
//            System.out.println("========");
//            System.out.println(ppt);
//            System.out.println("toplevel: " + ppt.varNames());
//			System.out.println("VT: " + vt.vals.length);
//            System.out.println(vt);
//            System.out.println("Enter nonce=" + nonce + " object_ID=" + vt.vals[0] + " ppt_name=" + ppt.name + " -- TYPE" + vt.vals[0].getClass().getSimpleName());
//            if (callLevel.containsKey(vt.vals[0])) System.out.println("call level: " + callLevel.get(vt.vals[0]));
//            System.out.println("========");

//			for(PptTopLevel _ppt : all_ppts.ppt_all_iterable()){
//				System.out.println(_ppt);
//			}
			Long object_ID;
			if (nonce == null)
			{
				System.out.println("Error: The sample doesn't contain nonce string!");
				System.exit(1);
			}
			// Only record traces that are in component_name
			if (!InvParser.isInComponent(ppt, instance.component_name)){
				return;
			}
			if (ppt.is_enter())
			{
                // Natcha: push entry ppt to stack (stack kept by callValue in callLevel map)
                // callValue = 0 is outer most or where the test suite is making call.

                // NATCHA: (POTENTIAL BUGGY FIX), vt.vals[0] for constructors doesn't give Object ID but values for arguments of the
                // constructor so we skip constructor::ENTER
                // TODO: VERIFY THIS IS TRUE

                if(vt.vals.length == 0)
                {
                    return;
                }
                if (!ppt.ppt_name.isConstructor()){
                    object_ID = (Long) vt.vals[0];

                    if(badIDs.contains(object_ID)) return;

//                    if (verbose)
//                        System.out.println("Enter nonce=" + nonce + " object_ID=" + object_ID + " ppt_name=" + ppt.name);


                    // Only record external calls

                    if(callLevel.containsKey(object_ID))
                    {
                        if (callLevel.get(object_ID) == 0)
                        {
                            currentEntry.put(object_ID, new EntryInfo(ppt, vt, nonce));
                        }
                        Long callValue = callLevel.get(object_ID);
                        callValue++;
                        callLevel.put(object_ID, callValue);
                    }
                } else {
                    constructorStackCount++;
//                    System.out.printf("[Ponzu]: %s | %s", constructorStackCount, ppt.ppt_name.getName());
                }
			}

			else
			{
				if(vt.vals.length == 0)
				{
					return;
				}
				
				object_ID = (Long) vt.vals[0];
				
				if(badIDs.contains(object_ID)) return;
				
				if (verbose)
					System.out.println("Exit nonce=" + nonce + " object_ID=" + object_ID + " ppt_name=" + ppt.name);
				boolean newItem = false;
				Long callValue;
				
				if(!callLevel.containsKey(object_ID))
				{
					// Check whether it is the constructor
					String methodName = instance.component_name.substring(instance.component_name.lastIndexOf('.'));
					if(!ppt.ppt_name.isConstructor()) {
						System.out.println("[WARNING] Unidentified or out-of-order event " + ppt.name()); // not an entering ppt, and not a constructor then quit reading this ppt.
						return;
					}
                    //exiting ppt & IS constructor, we begin with 0 <-- new item point, constructor exit contains object value.
                    // first check if constructor is externally called.
                    constructorStackCount--;
                    if (constructorStackCount > 0){
//                        System.out.printf("[WARNING] in nested Overloaded Constructor call (%s), skip ppt: %s\n", constructorStackCount,  ppt.name());
                        return;
                    }

                    //found external constructor, we add object_id and proceed to trace parsing.
//                    System.out.printf("[Ponzu]: FOUND %s | %s", constructorStackCount, ppt.ppt_name.getName());
                    constructorStackCount = 0;
					callValue = new Long(0);
					callLevel.put(object_ID, callValue); // will be subtracted to 0 below
					newItem = true;
				}
				else
				{
					callValue = callLevel.get(object_ID);
					if(callValue > 0) callValue--;
					callLevel.put(object_ID, callValue);
				}

				if (callValue > 0)
					return;
				
				EntryInfo entry = null;
				if(!newItem)
				{
					entry = (EntryInfo) currentEntry.get(object_ID);
					if (entry != null && !entry.nonce.equals(nonce))
					{
						System.out.println("Error: The traces do not follow stack discipline!");
						System.out.println("This implies some degree of concurrency in the code.");
						System.out.println("object_ID=" + object_ID + " entry.nonce=" + entry.nonce + " nonce=" + nonce);
						badIDs.add(object_ID);
						return;
					}
					else if (entry == null)
					{
						return;
					}
				}
					
				String name = ppt.name;
				
				// Append return value (true/false) to the subexit program point if applicable
				VarInfo varReturn = ppt.find_var_by_name("return");
				if (varReturn != null && varReturn.type.baseIsBoolean())
					if (varReturn.getIndexValue(vt) == 1)
						name += ";condition=\"return == true\"";
					else
						name += ";condition=\"not(return == true)\"";
				
				Event2 event = instance.events.get(name);
				if (event == null)
				{
					System.out.println("Event not found: " + name);
					System.exit(1);
				}

				if (verbose) {
					System.out.println(nonce + " " + name);
					for (VarInfo var : ppt.var_infos)
					{
						System.out.println("   " + var.name() + "=" + ValueTuple.valToString(var.getValue(vt)) //broken for new daikon.jar
								+ " type=" + var.rep_type);
					}
				}
				HashMap<String, Object> entry_map = new HashMap<String, Object>();
/*				lastTuples.get(entry.ppt.name);
				if (entry_map == null)
					lastTuples.put(entry.ppt.name, entry_map = new HashMap<String, Object>());
*/				
				HashMap<String, Object> exit_map = new HashMap<String, Object>();
/*				lastTuples.get(ppt.name);
				if (exit_map == null)
					lastTuples.put(ppt.name, exit_map = new HashMap<String, Object>());
*/				
				for (VarInfo var : instance.variables)
				{
					String varname = var.name();
					
					if (var.is_size())
					{
						String arrayname = varname.substring(5, varname.length()-1);
						
						if(entry != null)
						{
							VarInfo pre_var = entry.ppt.find_var_by_name(arrayname);
							if (pre_var != null) //&& pre_var.isModified(entry.vt))
							{
								Object pre_value = pre_var.getValueOrNull(entry.vt);
								if(pre_value != null)
								{
									entry_map.put(varname, getArraySize(pre_value));
									if (verbose)
										System.out.println("pre: " + varname + " = " + getArraySize(pre_value));
								}
							}
						}
						
						VarInfo post_var = ppt.find_var_by_name(arrayname);
						if (post_var != null) //&& post_var.isModified(vt))
						{
							Object post_value = post_var.getValueOrNull(vt);
							if(post_value != null)
							{
								exit_map.put(varname, getArraySize(post_value));
								if (verbose)
									System.out.println("post: " + varname + " = " + getArraySize(post_value));
							}
						}
					}
					else
					{
						if(entry != null)
						{
							VarInfo pre_var = entry.ppt.find_var_by_name(varname);
							if (pre_var != null) //&& pre_var.isModified(entry.vt))
							{
								Object pre_value = pre_var.getValueOrNull(entry.vt);
								if(pre_value != null)
								{
									entry_map.put(varname, pre_value);
									if (verbose)
										System.out.println("pre: " + varname + " = " + pre_value);
								}
							}
						}
						
						VarInfo post_var = ppt.find_var_by_name(varname);
						if (post_var != null) //&& post_var.isModified(vt))
						{
							Object post_value = post_var.getValueOrNull(vt);
							if(post_value != null)
							{
								exit_map.put(varname, post_value);
								if (verbose)
									System.out.println("post: " + varname + " = " + post_value);
							}
						}
					}										
				}
				
/*				System.out.println(ppt.varNames());
*/				event.incrementTraceUsage();
				
				instance.scenario.addInvocation(tracename + object_ID, event, 
					(HashMap<String, Object>) entry_map.clone(), 
					(HashMap<String, Object>) exit_map.clone());
			}
		}
		
		private Integer getArraySize(Object o)
		{
			return ((long[])o).length;
		}
	}
}
