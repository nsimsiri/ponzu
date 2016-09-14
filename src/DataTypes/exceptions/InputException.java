package DataTypes.exceptions;

import java.util.ArrayList;

public class InputException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<String> messages = new ArrayList<String>();
	
	public InputException() {}
	
	public void addCompEventMessage(String component, String event)
	{
		messages.add("In one of the scenarios, the destination of event " + event + " is not component " +component + ".");
	}
	
	public void addNoEventMessage(String event)
	{
		messages.add("The operation " + event + " does not have an example sequence diagram.");
	}
	
	public void addEventUnimplementedMessage(String event)
	{
		messages.add("The operation " + event + " is not provided by any component.");
	}
	
	public void addNoOccurrence(String component)
	{
		messages.add("The component " + component + " does not appear in any requried interaction.");
	}
	
	public void addNoSuchComponent(String component)
	{
		messages.add("The component " + component + " used in a sequence diagram is not specified.");
	}
	
	public ArrayList<String> getMessages()
	{
		return messages;		
	}
}
