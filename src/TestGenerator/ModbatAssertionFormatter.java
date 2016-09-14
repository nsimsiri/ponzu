package TestGenerator;

import daikon.inv.Invariant;

import java.util.List;
import java.util.Map;

/**
 * Created by NatchaS on 4/4/16.
 */
public class ModbatAssertionFormatter {
    private List<Invariant> postConditions;
    private String instanceName;
    public static final String invariantTesterName = "invariantTester";
    public ModbatAssertionFormatter(List<Invariant> postConditions, String instanceName){
        this.postConditions = postConditions;
        this.instanceName = instanceName;

    }

    public List<String> formAssertionProcess(){
        return null;
    }

    public String getInvariantTesterInitializationFormat(){
        String invs = "";
        for(int i = 0; i < postConditions.size(); i++){
            invs+=String.format("\"%s\"", postConditions.get(i).toString());
            if (i < postConditions.size()-1){
                invs+=", ";
            }
        }
        return String.format("var %s : InvariantsChecker = new InvariantsChecker(Array[String](%s))", invariantTesterName, invs);
    }

    public String getInstancePreVarsFormat(){
        return String.format("%s.setPreVarMap(%s)", invariantTesterName, instanceName);
    }

    public String getInstancePostVarsFormat(){
        return String.format("%s.setPostVarMap(%s)", invariantTesterName, instanceName);
    }

    public String getInvariantTesterAssertionFormat(){
        return String.format("%s.makeAssertions()", invariantTesterName);
    }
}
