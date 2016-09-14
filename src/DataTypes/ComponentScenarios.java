package DataTypes;

import java.util.ArrayList;

public class ComponentScenarios {
	
	private ArrayList<Scenario> annotated_scenarios = new ArrayList<Scenario>();
	//The name of the component
	private String name;
	
	public ComponentScenarios(String name) 
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Scenario> getScenarios()
	{
		return (ArrayList<Scenario>) annotated_scenarios.clone();
	}

	public void addScenario(Scenario sc)
	{
		annotated_scenarios.add(sc);
	}
	
	public Scenario getScenario(int index)
	{
		return annotated_scenarios.get(index);
	}
	
	//Retrieve the scenario by scenario name
	public Scenario getScenarioByName(String name)
	{
		for(int i = 0; i < annotated_scenarios.size(); i++)
		{
			if(annotated_scenarios.get(i).getName().equals(name)) return annotated_scenarios.get(i);
		}
		return null;
	}
	
	public int size()
	{
		return annotated_scenarios.size();
	}
}
