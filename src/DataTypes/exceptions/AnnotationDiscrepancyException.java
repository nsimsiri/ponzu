package DataTypes.exceptions;

import java.util.ArrayList;

public class AnnotationDiscrepancyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<String> messages = new ArrayList<String>();
	
	public AnnotationDiscrepancyException() {}
	
	public void addMessage(String message)
	{
		messages.add(message);
	}
	
	public ArrayList<String> getMessages()
	{
		return messages;
	}
	
	public int size()
	{
		return messages.size();
	}
}
