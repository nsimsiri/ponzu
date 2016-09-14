package parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import lts.CompactState;
import lts.CompositeState;
import lts.EmptyLTSOuput;
import lts.LTSCompiler;
import lts.LTSException;
import lts.SymbolTable;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;
import ac.ic.doc.commons.relations.BinaryRelation;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ui.FileInput;
import ui.StandardError;

public class FSPParser {
	
	public static void main(String args[]) throws IOException
	{
		System.out.println("Welcome to Parser Test!");
		
		DataTypes.MTS mts = null;
		
		try {
			//mts = parse("evaluation/groundtruth/DiningPhilosophers.lts");
			//convertForMatchTool("evaluation/jarinstaller/ZipOutputStreamRefinement.lts", "matchtool/ZipOutputStreamMI.xml");
			String classname = "ServiceManager";
			String type = "Req";
			if (type.equals("Impl"))
				convertForMatchTool("matchtool/implementation_models.lts", "matchtool/" + classname + type + ".xml", classname);
			else if (type.equals("Req"))
				convertForMatchTool("matchtool/C4-dynamic-behavior.lts", "matchtool/" + classname + type + ".xml", classname);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		System.out.println("Created MTS!");
	}
	
	public static DataTypes.MTS parse(String filename) throws IOException
	{
		FileInput input = new FileInput(new File(filename));
		//StandardOutput output = new StandardOutput();
		EmptyLTSOuput output = new EmptyLTSOuput();
		StandardError error = new StandardError();
		String currentDirectory = "";
		
		SymbolTable.init();
		input.resetMarker();
		
		Hashtable cs = new Hashtable();
		Hashtable ps = new Hashtable();
		
		LTSCompiler comp = new LTSCompiler(input, output, currentDirectory);
		try {
            comp.parse(cs, ps);
        } catch (LTSException x) {
            error.displayError(x);
            System.exit(1);
        }
		
		System.out.println("Process Spec:");
		for (Object o : ps.entrySet())
		{
			System.out.println(o);
		}
		
		AutomataToMTSConverter converter = AutomataToMTSConverter.getInstance();
		
		input.resetMarker();
		CompositeState c = null;
		try {
            c = comp.compile("DEFAULT");
        } catch (LTSException x) {
            error.displayError(x);
            System.exit(1);
        }
		
		c.compose(output);
		CompactState compact = c.composition;
		//System.out.println(compact.name);
		//System.out.println(c.machines.size());
		
		MTS<Long,String> mts = converter.convert(compact);
		DataTypes.MTS our_mts = new DataTypes.MTS(compact.name, null, null);
		
		Map<Long, BinaryRelation<String, Long>> transitions = mts.getTransitions(TransitionType.REQUIRED);
		
		for (Map.Entry<Long, BinaryRelation<String, Long>> entry : transitions.entrySet())
		{
			Long state = entry.getKey();
			if (state != 0)
				our_mts.addMTSState(new MTS_state(state.intValue(), null));
			
			for (Pair<String, Long> pair : entry.getValue())
			{
				our_mts.addMTSTransition(new MTS_transition(pair.getFirst(), state.intValue(), pair.getSecond().intValue(), "required"));
			}
		}
		
		transitions = mts.getTransitions(TransitionType.MAYBE);
		
		for (Map.Entry<Long, BinaryRelation<String, Long>> entry : transitions.entrySet())
		{
			Long state = entry.getKey();
			if (state != 0)
				our_mts.addMTSState(new MTS_state(state.intValue(), null));
			
			for (Pair<String, Long> pair : entry.getValue())
			{
				our_mts.addMTSTransition(new MTS_transition(pair.getFirst(), state.intValue(), pair.getSecond().intValue(), "maybe"));
			}
		}
		
		return our_mts;
	}
	
	public static void convertForMatchTool(String filename, String outfile, String toCompile) throws IOException
	{
		FileInput input = new FileInput(new File(filename));
		//StandardOutput output = new StandardOutput();
		EmptyLTSOuput output = new EmptyLTSOuput();
		StandardError error = new StandardError();
		String currentDirectory = "";
		
		SymbolTable.init();
		input.resetMarker();
		
		Hashtable cs = new Hashtable();
		Hashtable ps = new Hashtable();
		
		LTSCompiler comp = new LTSCompiler(input, output, currentDirectory);
		try {
            comp.parse(cs, ps);
        } catch (LTSException x) {
            error.displayError(x);
            System.exit(1);
        }
		
		System.out.println("Process Spec:");
		for (Object o : ps.entrySet())
		{
			System.out.println(o);
		}
		
		AutomataToMTSConverter converter = AutomataToMTSConverter.getInstance();
		
		input.resetMarker();
		CompositeState c = null;
		try {
            c = comp.compile(toCompile);
        } catch (LTSException x) {
            error.displayError(x);
            System.exit(1);
        }
		
		//c.compose(output);
		System.out.println(c.machines.size());
		CompactState compact = null;
		for (Object compState_ : c.machines)
		{
			CompactState compState = (CompactState) compState_;
			if (compState.name.equals(toCompile))
				compact = compState;
		}
		
		MTS<Long,String> mts = converter.convert(compact);
		
		PrintWriter pw = new PrintWriter(new File(outfile));
		pw.println("<?xml version=\"1.0\" ?>");
		pw.println("  <statemachine id=\"" + compact.name + "\">");
		
		Set<Long> states = mts.getStates();
		Long initialState = mts.getInitialState();
		Map<Long, BinaryRelation<String, Long>> transitions = mts.getTransitions(TransitionType.POSSIBLE);
		
		for (Long stateid : states)
		{
			pw.println("    <state id=\"" + stateid + "\" initial=\"" + (stateid == initialState) + "\" name=\"\" type=\"\" parentId=\"-1\" depth=\"1\" />");
		}
		
		for (Map.Entry<Long, BinaryRelation<String, Long>> entry : transitions.entrySet())
		{
			Long state = entry.getKey();
			
			for (Pair<String, Long> pair : entry.getValue())
			{
				pw.println("    <transition from=\"" + state + "\" to=\"" + pair.getSecond() + "\" event=\"" + pair.getFirst() + "\" condition=\"\" action=\"\" />");
			}
		}
		
		pw.println("  </statemachine>");
		
		pw.close();
	}
}
