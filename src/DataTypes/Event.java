package DataTypes;

import java.util.*;
import java.util.regex.*;

public class Event {
	
	//List of event preconditions and postconditions
	private ArrayList<String> preCond = new ArrayList<String>();
	private ArrayList<String> postCond = new ArrayList<String>(); 
	
	//Type of operation - in, out, inout
	private String type = null;
	private String name = null;
	
	//List of interacting components for this event in the provided set of SDs
	private interactionPair interactionList = new interactionPair();

	//Class describing the interaction pairs for an event
	private class interactionPair{
		public ArrayList<String> inComp = new ArrayList<String>();
		public ArrayList<String> outComp = new ArrayList<String>();
		
		public interactionPair() {}
	}
	
	public Event(String name, String type) 
	{
		this.name = name;
		this.type = type;
	}
	
	public boolean addPre(String pre)
	{
		if(preCond.contains(pre))
		{
			return false;
		}
		else if(pre.contains("for(")){
			expandFor(pre,true);
		}
		else preCond.add(pre);
		return true;
	}

	public boolean addPost(String post)
	{
		if(postCond.contains(post))
		{
			return false;
		}
		else if(post.contains("for(")){
			expandFor(post,false);
		}
		else postCond.add(post);
		return true;
	}
	
	public void removePost(String post)
	{
		if(postCond.contains(post))
		{
			postCond.remove(post);
		}
	}
	
	public void expandFor(String expr,boolean isPreCond){
		String forPatternStr = "for\\(";
		Pattern forPattern = Pattern.compile(forPatternStr);
		//Matcher matcher = forPattern.matcher(expr);
		String forStr="";
		String iterVar="";
		int iterStart=0;
		int iterFinish=0;
		String expression="";
		
		int forStart = expr.indexOf("for(");
		
		//while(hasFor){
			forStr = expr.substring(forStart+3, expr.length()-1);
			System.out.println(forStr);
			//parse for loop - ex. "for(x=[1:3];(expr);)"
			iterVar = forStr.substring(forStr.indexOf('(')+1,forStr.indexOf('='));
			iterStart = Integer.parseInt(forStr.substring(forStr.indexOf('[')+1,forStr.indexOf(':')));
			iterFinish = Integer.parseInt(forStr.substring(forStr.indexOf(':')+1,forStr.indexOf(']')));
			int semiIdx = forStr.indexOf(';')+1;
			expression = forStr.substring(semiIdx,forStr.indexOf(';',semiIdx));
			
			String startString = expr.substring(0, forStart);
			String endString = expr.substring(forStart+3+forStr.indexOf(';',semiIdx+1)+1,expr.length());
			
			for(int i=iterStart;i<=iterFinish;i++){
				String find="["+iterVar+"]";
				String replace = "["+i+"]";
				String loopExpr = expression.replace(find, replace);
				String expanded = startString+loopExpr+endString;
				if(isPreCond){
					preCond.add(expanded);
				}
				else{
					postCond.add(expanded);
				}
				System.out.println(startString+loopExpr+endString);
			}
		//}
		
	}
	
	//Adds an interaction pair once it is found in a SD
	public int addInteractionPair(String outComp, String inComp)
	{
		for(int i = 0; i < interactionList.outComp.size(); i++)
		{
			if(interactionList.outComp.get(i).equals(outComp) && interactionList.inComp.get(i).equals(inComp)) 
			{
				return interactionList.outComp.indexOf(outComp);
			}
		}
		
		interactionList.outComp.add(outComp);
		interactionList.inComp.add(inComp);
		
		return interactionList.outComp.size()-1;
	}
	
	//Returns the component that makes call to this operation at index
	public String getInteractionOut(int index)
	{
		return interactionList.outComp.get(index).toString();
	}
	
	//Returns the component that receives invocation to this operation at index
	public String getInteractionIn(int index)
	{
		return interactionList.inComp.get(index).toString();
	}
	
	//Number of occurrences of this operation in the SDs
	public int getPairNumber ()
	{
		return interactionList.inComp.size();
	}
	
	//Does this operation have an owner (if not, that is probably an error)
	public boolean inUnique()
	{
		String inComp = interactionList.inComp.get(0);
		for(int i = 1; i < interactionList.inComp.size(); i++)
		{
			if(!interactionList.inComp.get(i).equals(inComp)) return false;
		}
		return true;
	}
	
	//Tests if a component occurs in any interaction with a particular operation
	public boolean occurrs(String comp)
	{
		for(int i = 0; i < interactionList.inComp.size(); i++)
		{
			if(interactionList.inComp.get(i).equals(comp)) return true;
			else if(interactionList.outComp.get(i).equals(comp)) return true;
		}
		return false;
	}
	
	public String getName()
	{
		return name.toString();
	}
	
	public String getType()
	{
		return type.toString();
	}
	
	public void setType(String type)
	{
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getPreCond()
	{
		return (ArrayList<String>) preCond.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getPostCond()
	{
		return (ArrayList<String>) postCond.clone();
	}
}
