package DataTypes;

import java.util.ArrayList;

public class MTS_state {
	
	private ArrayList<String> state_variables = new ArrayList<String>();
	private int name;
	private boolean processed;
	
	@SuppressWarnings("unchecked")
	public MTS_state(int name, ArrayList<String> variables) 
	{
		this.name = name;
		if(variables != null)
		{
			state_variables = (ArrayList<String>) variables.clone();
		}
		processed = false;
	}
	
	public int getName()
	{
		return name;
	}
	
	public void setName(int n){
		name = n;
	}
	
	public String toString()
	{
		return "S" + String.valueOf(name);
	}
	
	//Returns the value assignment of the particular MTS state
	public ArrayList<String> getVariableState()
	{
		return state_variables;
	}
	
	public boolean isProcessed(){
		return processed;
	}
	
	public void setProcessed(){
		processed = true;
	}
	
	public boolean equals(Object second)
	{
		if(second instanceof MTS_state)
		{
			MTS_state comparison = (MTS_state) second;
			if(this.name == comparison.name)
			{
				return true;
			}
		}
		return false;
	}
}
