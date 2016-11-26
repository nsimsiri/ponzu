package DataTypes;

import daikon.VarInfo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scenario2 {
	
	public class Invocation {
		
		public Event2 event;
		public Map<String, Object> pre_values;
		public Map<String, Object> post_values;
		
		public Invocation(Event2 event, Map<String, Object> pre_values, Map<String, Object> post_values)
		{
			this.event = event;
			this.pre_values = pre_values;
			this.post_values = post_values;
		}
		
		public long get_pre_value(String varname)
		{
			return convert_to_long(pre_values.get(varname));
		}
		
		public long get_post_value(String varname)
		{
			return convert_to_long(post_values.get(varname));
		}

		public boolean has_post_var(String varname){
            return post_values.containsKey(varname);
        }

        public boolean has_pre_var(String varname){
            return pre_values.containsKey(varname);
        }
		
		/*public String get_pre_value_str(String varname)
		{
			return convert_to_string(pre_values.get(varname));
		}
		
		public String get_post_value_str(String varname)
		{
			return convert_to_string(post_values.get(varname));
		}*/
		
	}
	
	private ArrayList<Invocation> eventList = new ArrayList<Invocation>();
	private HashMap<String,ArrayList<Invocation>> eventListByID = new HashMap<String, ArrayList<Invocation>>();
	
	private String name;
	
	public Scenario2(String name)
	{
		this.name = name;
	}
	
	public Invocation addInvocation(String ID, Event2 event, Map<String, Object> pre_values, Map<String, Object> post_values)
	{
		System.out.printf("[DELETE] %s\nPRE: %s\nPOST: %s\n\n", event.getName(), pre_values, post_values);
		Invocation i = new Invocation(event, pre_values, post_values);
		eventList.add(i);
		
		if(!eventListByID.containsKey(ID))
		{
			ArrayList<Scenario2.Invocation> newIDList = new ArrayList<Scenario2.Invocation>();
			newIDList.add(i);
			eventListByID.put(ID, newIDList);
		}
		else
		{
			ArrayList<Scenario2.Invocation> IDList = eventListByID.get(ID);
			IDList.add(i);
			eventListByID.put(ID, IDList);
		}
		
		return i;
	}
	
	public Invocation getInvocation(int index)
	{
		return eventList.get(index);
	}
	
	public void printInvocations()
	{
		for(Invocation inv:eventList)
		{
			System.out.println(inv.event.getName());
			
			System.out.println("Entry: ");
			for(String eventName : inv.pre_values.keySet())
			{
				System.out.print(eventName + "=" + inv.get_pre_value(eventName) + ", ");
			}
			
			System.out.println("Exit: ");
			for(String eventName : inv.post_values.keySet())
			{
				System.out.print(eventName + "=" + inv.get_post_value(eventName) + ", ");
			}
			
			
		}
	}

	/**
	 * Dump all invocations as plaintext traces in the format
	 * "traceID /// eventName" to the filename supplied
	 * 
	 * @param outfile
	 *            Name of the file where traces will be written
	 */
	public void dumpInvocations(String outfile)
	{
		PrintStream out = null;
		try
		{
			out = new PrintStream(outfile);

			// For each trace, loop over events and write each one
			for (String key : eventListByID.keySet())
			{
				for (Invocation inv : eventListByID.get(key))
				{
					out.println(key + " /// " + inv.event.getName());
				}
			}
			out.close();
		} catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void dumpInvocation(String outfile, boolean isLong){
		if (isLong) dumpInvocations(outfile);
		else {
			PrintStream out = null;
			try {
				out = new PrintStream(outfile);

				// For each trace, loop over events and write each one
				for (String key : eventListByID.keySet()) {
					for (Invocation inv : eventListByID.get(key)) {
						out.printf("%s /// %s -> %s\n", key, inv.event.getPptName().getMethodName(), inv.post_values);
					}
				}
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

	}

    public void printInvocationGraphDotFile(String filepath, AnalysisInstance instance){
        PrintStream out = null;
        try {

            boolean isConst = true;
            for(String key : eventListByID.keySet()){
                out = new PrintStream(filepath + "trace-" + key + ".dot");
                out.println("digraph Trace {");
                int cur = 0;
                for(Invocation inv : eventListByID.get(key)){
                    String pre_node = "";
                    if (isConst){
                        for(String var : inv.pre_values.keySet()){
                            pre_node+=var + ": "+ inv.pre_values.get(var)+"\n";
                        }
                        isConst = false;
                        out.printf("S%s[label=\"%s\"];\n", cur , inv.pre_values.isEmpty() ? String.format("%s \n{ empty pre-values }", key) : pre_node);
                    }
                    String node_inf = "";

                    for (VarInfo var : instance.variables) {
						if (inv.has_post_var(var.name())){
							node_inf+=var + ": "+ inv.get_post_value(var.name()) +"\n";
						}
                    }
                    out.printf("S%s[label=\"%s\"];\n", cur+1 ,node_inf);
                    String name = inv.event.getPptName().getMethodName();
                    if (inv.event.getName().contains(":::")){
                        String[] name_spl = inv.event.getName().split(":::");
                        name+=":::"+name_spl[name_spl.length-1].replace("\"", "");
                    }
                    String preCondStr = "";
                    String postCondStr = "";
                    for(String preCond : inv.event.getPreCond_str()){
                        preCondStr+="\n"+preCond;
                    }
                    for(String postCond : inv.event.getPostCond_str()){
                        postCondStr += "\n"+postCond;
                    }

                    out.printf("S%s -> S%s [label=\"%s\nPRE-COND:%s\nPOST-COND:%s\"];\n", cur, cur+1, name, preCondStr, postCondStr);
                    cur+=1;
                }
                cur+=1;
                isConst = true;
                out.println("\n}");
                out.close();
            }
        } catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

	public int getLength() {
		return eventList.size();
	}
	
	public Set<String> getSequenceIDs()
	{
		return eventListByID.keySet();
	}

	public List<Invocation> getInvocations(){
		return  this.eventList;
	}

	public ArrayList<Invocation> getSequenceByID(String sequenceID) {
		
		return eventListByID.get(sequenceID);
	}

	public static long convert_to_long(Object o)
	{
		if (o instanceof Integer)
			return ((Integer)o).longValue();
		else if (o instanceof Long)
			return ((Long)o).longValue();
		return 0;
	}
	
	public static String convert_to_string(Object o)
	{
		if (o instanceof Integer)
			return ((Integer)o).toString();
		else if (o instanceof Long)
			return ((Long)o).toString();
		return null;
	}
}