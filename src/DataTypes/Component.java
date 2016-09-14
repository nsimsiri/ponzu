package DataTypes;

import java.util.ArrayList;


public class Component {
	
	private String name;
	private ArrayList<Event> compInterface = new ArrayList<Event>();
	private ArrayList<String> significantVariable = new ArrayList<String>();
	private ArrayList<String> absoluteVariable = new ArrayList<String>();

	public Component(String name)
	{
		this.name = name;
	}
	
	public Event getEvent(String name)
	{
		Event returnEv = null;
		
		for(int i = 0; i < compInterface.size(); i++)
		{
			if(compInterface.get(i).getName().equals(name)) return compInterface.get(i);
		}
		
		return returnEv;
	}
	
	public ArrayList<Event> getEvents()
	{
		return compInterface;
	}
	
	public boolean addEventProvided(Event newEvent)
	{
		if(compInterface.contains(newEvent)) return false;
		else compInterface.add(newEvent);
		
		return true;
	}
	
	//Returns events in the provided interface
	public ArrayList<Event> getEventProvided()
	{
		ArrayList<Event> providedInterface = new ArrayList<Event>();
		for(int i = 0; i < compInterface.size(); i++)
		{
			if(compInterface.get(i).getType().equals("in") || compInterface.get(i).getType().equals("inout"))
			{
				providedInterface.add(compInterface.get(i));
			}
		}
		return providedInterface;
	}
	
	//Returns the names of the provided operations
	public ArrayList<String> getProvidedOperationNames()
	{
		ArrayList<String> providedInterface = new ArrayList<String>();
		for(int i = 0; i < compInterface.size(); i++)
		{
			if(compInterface.get(i).getType().equals("in") || compInterface.get(i).getType().equals("inout"))
			{
				providedInterface.add(compInterface.get(i).getName());
			}
		}
		return providedInterface;
	}
	
	//Returns the names of the required operations
	public ArrayList<String> getRequiredOperationNames()
	{
		ArrayList<String> providedInterface = new ArrayList<String>();
		for(int i = 0; i < compInterface.size(); i++)
		{
			if(compInterface.get(i).getType().equals("out") || compInterface.get(i).getType().equals("inout"))
			{
				providedInterface.add(compInterface.get(i).getName());
			}
		}
		return providedInterface;
	}
	
	public boolean addEventRequired(Event newEvent)
	{
		if(compInterface.contains(newEvent)) 
		{
			return false;
		}
		else compInterface.add(newEvent);
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getVariables()
	{
		return (ArrayList<String>) significantVariable.clone();
	}
	
	//Insert a significant domain variable
	public boolean addVariable(String var)
	{
		if(significantVariable.contains(var)) return false;
		else significantVariable.add(var);
		
		return true;
	}
	
	//Insert a scoped domain variable
	public boolean addAbsoluteVariable(String var)
	{
		absoluteVariable.add(var);
		return true;
	}
	
	public void removeAnyVariable(String var)
	{
		if(significantVariable.contains(var)) significantVariable.remove(var);
		if(absoluteVariable.contains(var)) absoluteVariable.remove(var);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getAbsoluteVariables()
	{
		return (ArrayList<String>) absoluteVariable.clone();
	}
	
	public String getName()
	{
		return this.name.toString();
	}
}
