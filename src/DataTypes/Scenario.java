package DataTypes;

import java.util.ArrayList;

public class Scenario {
	
	//Ordered sequence of events
	private ArrayList<Event> eventList = new ArrayList<Event>();
	
	//The index of the particular interaction in the corresponding event's interaction pair list
	private ArrayList<Integer> pairNumber = new ArrayList<Integer>();
	
	//Annotations on the scenario
	private ArrayList<ArrayList<String>> annotations = new ArrayList<ArrayList<String>>();
	private String name;
	
	public Scenario(String name)
	{
		this.name=name;
	}
	
	//Storing the event and the interaction pair index for the corresponding event's interaction pair list
	public boolean addEvent(Event newEvent, int interactionPairNumber)
	{
		eventList.add(newEvent);
		pairNumber.add(new Integer(interactionPairNumber));
		return true;
	}
	
	//Add multiple events to the event list (used when creating annotated component-level scenarios)
	public boolean addEvents(ArrayList<Event> events)
	{
		eventList = events;
		return true;
	}
	
	//Setting all the annotations
	public void setAnnotations(ArrayList<ArrayList<String>> newAnnotations)
	{
		annotations = newAnnotations;
	}
	
	//Getter for all the annotations
	public ArrayList<ArrayList<String>> getAnnotations()
	{
		return annotations;
	}
	
	//Getter for the annotations on events in which the specified component participates
	public ArrayList<ArrayList<String>> getAnnotationsForComp(String compName)
	{
		ArrayList<ArrayList<String>> returnAnnotations = new ArrayList<ArrayList<String>>();
		//Disregard: Index of the last annotation that was returned
		//int index = -1; 
		
		for(int i = 0; i < eventList.size(); i++)
		{
			//In case the component is participating in the interaction, add the annotations into the return annotation set
			if(eventList.get(i).getInteractionIn(pairNumber.get(i).intValue()).equals(compName) ||
					eventList.get(i).getInteractionOut(pairNumber.get(i).intValue()).equals(compName))
			{
				//If the pre-annotation is not already returned, put it into the return list
				returnAnnotations.add(annotations.get(i));
				returnAnnotations.add(annotations.get(i + 1));
				//index = i + 1;
			}
		}		
		return returnAnnotations;
	}
	
	//Get an event with index i
	public Event getEvent(int i)
	{
		return eventList.get(i);
	}
	
	//Get the list of events in which the component participates
	@SuppressWarnings("unchecked")
	public ArrayList<Event> getEventList(String component)
	{
		ArrayList<Event> returnList = new ArrayList<Event>();
		if(component.equals("System") || component.equals("All")) return (ArrayList<Event>) eventList.clone();
		for(int i = 0; i < eventList.size(); i++)
		{
			if(eventList.get(i).getInteractionIn(pairNumber.get(i).intValue()).equals(component) ||
					eventList.get(i).getInteractionOut(pairNumber.get(i).intValue()).equals(component))
			{
				returnList.add(eventList.get(i));
			}
		}
		return returnList;
	}
	
	//The length of the scenario
	public int size()
	{
		return eventList.size();
	}
	
	//The index of interaction pair in event[number]'s interaction pair list
	public int getIndex(int number)
	{
		return pairNumber.get(number).intValue();
	}
	
	public void setIndex(int index)
	{
		pairNumber.add(new Integer(index));
	}
	
	public String getName()
	{
		return name;
	}
	
	public void removeLast()
	{
		eventList.remove(eventList.size() - 1);
		pairNumber.remove(pairNumber.size() - 1);
	}
}
