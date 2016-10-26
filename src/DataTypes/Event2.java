package DataTypes;

import java.util.ArrayList;

import MTSGenerator2.Converter;
import daikon.PptName;
import daikon.inv.Invariant;

public class Event2 {
	
	//List of event preconditions and postconditions
	private ArrayList<Invariant> preCond = new ArrayList<Invariant>();
	private ArrayList<Invariant> postCond = new ArrayList<Invariant>();
	
	private ArrayList<String> preCond_str = new ArrayList<String>();
	private ArrayList<String> postCond_str = new ArrayList<String>();
	
	private PptName name = null;
	
	private int traceCount = 0;
	
	public Event2(PptName name) 
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name.getName();
	}
	
	public PptName getPptName()
	{
		return name;
	}
	
	public boolean isConstructor()
	{
		return name.getShortClassName().equals(name.getMethodName());
	}

    // Natcha: modified to add only invariants Converter.java supports. check Converter.isProcessableInvariantType method for more details.
    // In Converter we're also adding a Yices string version of invariant. This check is to keep the two invariants list consistent
    // (i.e postCond and postCond_str consistent)
	public void addPreCond(Invariant inv)
	{
		if (Converter.isProcessableInvariantType(inv)){
            for (Invariant i : preCond)
                if (i.isSameInvariant(inv))
                    return;
            preCond.add(inv);
        }

	}
	
	public void addPostCond(Invariant inv)
	{
        if (Converter.isProcessableInvariantType(inv)){
            for (Invariant i : postCond)
                if (i.isSameInvariant(inv))
                    return;
            postCond.add(inv);
        }
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Invariant> getPreCond()
	{
		return (ArrayList<Invariant>) preCond.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Invariant> getPostCond()
	{
		return (ArrayList<Invariant>) postCond.clone();
	}
	
	public void addPreCond(String inv)
	{
		preCond_str.add(inv);
	}
	
	public void addPostCond(String inv)
	{
		postCond_str.add(inv);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getPreCond_str()
	{
		return (ArrayList<String>) preCond_str.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getPostCond_str()
	{
		return (ArrayList<String>) postCond_str.clone();
	}

	public void incrementTraceUsage() {
		traceCount++;
	}
	
	public int getTraceCount() {
		return traceCount;
	}
	
}
