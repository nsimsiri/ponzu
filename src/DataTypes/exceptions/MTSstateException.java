package DataTypes.exceptions;

import java.util.ArrayList;

public class MTSstateException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<String> messages = new ArrayList<String>();
	public MTSstateException(){}
	
	public void missingState(String MTS1, String MTS2, ArrayList<String> names, ArrayList<String> values)
	{
		String messageString = "MTS " + MTS1 + " has a state with variable value combination <";
		for(int i = 0; i < values.size() - 1; i++)
		{
			messageString += names.get(i) + "=" + values.get(i) + ",";
		}
		messageString += names.get(values.size() - 1) + "=" + values.get(values.size() - 1) + "> which does not appear in the states of " + MTS2;
		messages.add(messageString);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getMessages()
	{
		return (ArrayList<String>) messages.clone();
	}
}
