package MTSGenerator.analysis;

import java.util.ArrayList;
import DataTypes.MTS;
import DataTypes.exceptions.MTSstateException;

public class MTSanalysis {
	
	public MTSanalysis() {}
	
	public void analyzeMTSs(ArrayList<MTS> componentMTSs) throws MTSstateException
	{
		MTSstateException returnException = new MTSstateException();
		boolean exceptionFound = false;
		
		//Compare value combinations of the same significant variables
		for(int i = 0; i < componentMTSs.size(); i++)
		{
			MTS componentMTS1 = componentMTSs.get(i);
			ArrayList<String> varNames1 = componentMTS1.getVariableNames();
			for(int j = i + 1; j < componentMTSs.size(); j++)
			{
				MTS componentMTS2 = componentMTSs.get(j);
				ArrayList<String> varNames2 = componentMTS2.getVariableNames();
				
				ArrayList<String> interSection = new ArrayList<String>();
				
				//Calculate the intersection significant variables
				for(int k = 0; k < varNames1.size(); k++)
				{
					if(varNames2.contains(varNames1.get(k)))
					{
						interSection.add(varNames1.get(k));
					}
				}
				
				ArrayList<ArrayList<String>> combinations1 = componentMTS1.getVarValueCombinations(interSection);
				ArrayList<ArrayList<String>> combinations2 = componentMTS2.getVarValueCombinations(interSection);
				
				for(int k = 0; k < combinations1.size(); k++)
				{
					if(!combinations2.contains(combinations1.get(k)))
					{
						exceptionFound = true;
						returnException.missingState(componentMTS1.getName(), componentMTS2.getName(), interSection, combinations1.get(k));
					}
				}
				for(int k = 0; k < combinations2.size(); k++)
				{
					if(!combinations1.contains(combinations2.get(k)))
					{
						exceptionFound = true;
						returnException.missingState(componentMTS2.getName(), componentMTS1.getName(), interSection, combinations2.get(k));
					}
				}				
			}
		}
		if(exceptionFound) throw returnException;
	}
}
