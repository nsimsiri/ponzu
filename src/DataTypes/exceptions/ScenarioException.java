package DataTypes.exceptions;

import java.util.ArrayList;

//import DataTypes.ComponentScenarios;
import DataTypes.Scenario;

public class ScenarioException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String message;
	private String componentName;
	private Scenario compScen = null;
	private int index, order;
	private String scName;
	private String varName;
	private ArrayList<String> pre = new ArrayList<String>();
	private ArrayList<String> post= new ArrayList<String>();
	//Type defines where the annotation failed between two invocations or
	//the annotations for one invocation differ although they should not
	private String type;

	//Define the message and the type of the ScenarioException
	public ScenarioException(String message, String type)
	{
		this.message = message;
		this.type = type;
	}
	
	public String getName()
	{
		return message;
	}
	
	//Set the name of the component for whose perspective the scenario cannot execute
	public void putComponentName(String component)
	{
		this.componentName = component;
	}
	
	//Set the scenario which failed
	public void putComponentScenario(Scenario sc)
	{
		this.compScen = sc;
	}
	
	//Set the index where the scenario annotation failed (order is the position in 
	//the sequence, while index is the variable that was being annotated
	public void setIndex(String scName, int order, int index, String varName)
	{
		this.scName = scName;
		this.order = order;
		this.index = index;
		this.varName = varName;
	}
	
	public String getScenarioName()
	{
		return scName;
	}
	
	public String getVarName()
	{
		return varName;
	}
	
	//In put the scenario annotations (i.e., not the final annotations, but the annotations
	//in the process of value propagation
	public void inputPrePostAnnotations(ArrayList<String> pre, ArrayList<String> post)
	{
		this.pre = pre;
		this.post = post;
	}
	
	public String getComponentName()
	{
		return componentName;
	}
	
	public ArrayList<String> getPreAnnotation()
	{
		return pre;
	}
	
	public ArrayList<String> getPostAnnotation()
	{
		return post;
	}
	
	public Scenario getScenario()
	{
		return compScen;
	}
	
	public int getEventPosition()
	{
		return order;
	}
	
	public int getAnnotationIndex()
	{
		return index;
	}
	
	public String getType()
	{
		return type;
	}
}
