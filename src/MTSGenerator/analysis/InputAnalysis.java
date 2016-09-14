package MTSGenerator.analysis;

import java.util.ArrayList;

import DataTypes.Component;
import DataTypes.Event;
import DataTypes.exceptions.InputException;

public class InputAnalysis {

	public InputAnalysis() {}
	
	public void Analyze(ArrayList<Component> components, ArrayList<Event> events) throws InputException
	{
		InputException discoveredProblems = new InputException();
		boolean round = false;
		boolean eventOccurrence[] = new boolean[events.size()];
		for(int i = 0; i < eventOccurrence.length; i++) eventOccurrence[i] = false;
		
		for(int i = 0; i < components.size(); i++)
		{
			Component currentComponent = components.get(i);
			boolean occurrence = false;
			ArrayList<Event> providedInterface = currentComponent.getEventProvided();
			
			for(int j = 0; j < events.size(); j++)
			{
				Event currentEvent = events.get(j);		
				for(int k = 0; k < providedInterface.size(); k++)
				{
					String providedName = providedInterface.get(k).getName();
					if(providedName.equals(currentEvent.getName()))
					{
						eventOccurrence[j] = true;
						
						//If there is an interaction in an SD where the owner component is 
						//not the destination of the event
						if(!currentEvent.inUnique())
						{
							discoveredProblems.addCompEventMessage(currentComponent.getName(), currentEvent.getName());
						}
					}
				}
				
				if(!occurrence && currentEvent.occurrs(currentComponent.getName())) occurrence = true;
				
				//If the event does not appear in any interaction
				if(!round && currentEvent.getPairNumber() == 0)
				{
					discoveredProblems.addNoEventMessage(currentEvent.getName());
					round = true;
				}				
			}
			
			if(!occurrence)
			{
				discoveredProblems.addNoOccurrence(currentComponent.getName());
			}
		}
		
		for(int i = 0; i < eventOccurrence.length; i++)
		{
			//If an event does not appear in any provided interface
			if(eventOccurrence[i] == false)
			{
				discoveredProblems.addEventUnimplementedMessage(events.get(i).getName());
			}	
		}
		
		ArrayList<String> componentNames = new ArrayList<String>();
		for(int i = 0; i < components.size(); i++)
		{
			componentNames.add(components.get(i).getName());
		}
		for(int i = 0; i < events.size(); i++)
		{
			for(int j = 0; j < events.get(i).getPairNumber(); j++)
			{
				//If there is an SD that uses non-existing component names return error
				if(!componentNames.contains(events.get(i).getInteractionIn(j)))
				{
					discoveredProblems.addNoSuchComponent(events.get(i).getInteractionIn(j));
					break;
				}
				if(!componentNames.contains(events.get(i).getInteractionOut(j)))
				{
					discoveredProblems.addNoSuchComponent(events.get(i).getInteractionOut(j));
					break;
				}
			}
		}
		
		if(discoveredProblems.getMessages().size() > 0) throw discoveredProblems;
	}
}
