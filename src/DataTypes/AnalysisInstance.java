package DataTypes;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import MTSGenerator2.Converter;
import daikon.VarInfo;
import daikon.inv.DiscardInfo;
import daikon.inv.Invariant;


public class AnalysisInstance {
	
	public String component_name;
	public Map<String, Event2> events;
	public List<Event2> eventList;
	public List<VarInfo> variables;
	public Set<Invariant> object_invariants;
	public Map<String, String> constants;
	public Scenario2 scenario;
	
	public ArrayList<String> var_names;
	
	
	public AnalysisInstance(String compname)
	{
		component_name = compname;
		events = new HashMap<String, Event2>();
		eventList = new ArrayList<Event2>();
		variables = new ArrayList<VarInfo>();
		object_invariants = new HashSet<Invariant>();
		constants = new HashMap<String, String>();
		scenario = new Scenario2("temp");
		var_names = new ArrayList<String>();
	}
	
	public void printInvariants(String filename)
	{
		PrintWriter pw = null;
		Converter c = new Converter(this);
		
		try {
			pw = new PrintWriter(filename);
		} catch (Exception e) {
			System.out.println("Exception caught (" + e.getClass().getSimpleName() 
					+ ") : " + e.getMessage());
			System.exit(1);
		}
		
		pw.println("Object Invariants:");
		for (Invariant inv : object_invariants)
		{
			pw.println("   " + inv);
			//pw.println("   " + c.toYicesExpr(inv, true));
			//checkDiscard(pw, inv);
		}
		
		for (Event2 e : eventList)
		{
			pw.println(e.getName());
			pw.println("Preconditions:");
			for (Invariant inv : e.getPreCond())
			{
				pw.println("   " + inv);
				//pw.println("   " + c.toYicesExpr(inv, true));
				//checkDiscard(pw, inv);
			}
			pw.println("Postconditions:");
			for (Invariant inv : e.getPostCond())
			{
				pw.println("   " + inv);
				//pw.println("   " + c.toYicesExpr(inv, false));
				//checkDiscard(pw, inv);
			}
		}
		
		pw.println("\nVariables:");
		for (VarInfo var : variables)
		{
			pw.println(var.name());
			pw.println("   " + var.var_kind);
			if (var.get_all_constituent_vars() != null)
			{
				pw.print("    Function Args: ");
				for (VarInfo arg : var.get_all_constituent_vars())
					pw.print(arg + ", ");
				pw.println("");
			}
		}
		
		pw.println("\nConstants:");
		for (Map.Entry<String, String> entry : constants.entrySet())
			pw.println(entry.getKey() + "=" + entry.getValue());
		
		pw.flush();
		pw.close();
	}
	
	private void checkDiscard(PrintWriter pw, Invariant inv)
	{
		DiscardInfo di = inv.isObvious();
		if (di != null)
			pw.println("   " + di.discardString());
	}

	public void eliminatePrivate() {
		ArrayList<Event2> privateMethods = new ArrayList<Event2>();
		for(Event2 current : eventList) {
			if (current.getTraceCount() == 0) {
				privateMethods.add(current);
				events.remove(current.getName());
			}
		}
		
		eventList.removeAll(privateMethods);
		return;
	}
}
