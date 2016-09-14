package DataTypes;

import daikon.PptName;

public class MTS_transition {
	
	private PptName name = null;
	private int start, end, occurrences;
	private String type,event;
	
	public MTS_transition(String event, int start, int end, String type) 
	{
		this.start = start;
		this.end = end;
		this.type = type;
		this.event = event;
		this.occurrences = 0;
	}
	
	public MTS_transition(PptName name, int start, int end, String type)
	{
		this.name = name;
		this.start = start;
		this.end = end;
		this.type = type;
		if (name != null)
			this.event = name.getName();// getMTSAName(name);
		else
			this.event = "";
		this.occurrences = 0;
	}

	public PptName getName()
	{
		return name;
	}
	
	public int getStart()
	{
		return start;
	}
	
	public int getEnd()
	{
		return end;
	}
	
	public String getType()
	{
		return type;
	}
	
	public String getEvent()
	{
		return event;
	}
	
	public int getOccurrences()
	{
		return occurrences;
	}
	
	public void changeStart(int newStart)
	{
		start = newStart;
	}
	
	public void changeDest(int newDest)
	{
		end = newDest;
	}
	
	public void changeType(String type)
	{
		this.type = type;
	}
	
	public int increaseOccurrences()
	{
		occurrences++;
		return occurrences;
	}
	
	public boolean equals(Object second)
	{
		if(second instanceof MTS_transition)
		{
			MTS_transition comparison = (MTS_transition) second;
			if(this.start == comparison.start && this.end == comparison.end && 
					this.event.equals(comparison.event) && this.type.equals(comparison.type))
			{
				return true;
			}
		}
		return false;
	}

	public int increaseOccurrencesBy(int occurrences) {
		
		this.occurrences += occurrences;
		return this.occurrences;
	}
	
	public String toString()
	{
		return this.event.toString();
	}
	
	public String getMTSAName()
	{
		if (this.name == null)
			return this.event;
		return getMTSAName(this.name);
	}
	
	private static String getMTSAName(PptName name)
	{
		String methodName = name.getMethodName();
		methodName = methodName.toLowerCase().charAt(0) + methodName.substring(1);
		String str = methodName + "_EXIT" + name.exitLine();
		if(name.getPoint().contains("not(return == true)"))
			str += "_FALSE";
		else if (name.getPoint().contains("true"))
			str += "_TRUE";
		return str;
	}
}
