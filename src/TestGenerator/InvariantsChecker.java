package TestGenerator;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by NatchaS on 4/6/16.
 */
public class InvariantsChecker {
    private String[] invariantStrings;
    private HashMap<String, Object> preVarMap;
    private HashMap<String, Object> postVarMap;
    private TestInvariant[] invariantTesters;
    public InvariantsChecker(String[] invariantStrings){
        this.invariantStrings = invariantStrings;
        this.invariantTesters = new TestInvariant[invariantStrings.length];
        for(int i =0; i < invariantStrings.length; i++){
            this.invariantTesters[i] = new TestInvariant(invariantStrings[i]);
        }
        this.preVarMap = new HashMap<>();
        this.postVarMap = new HashMap<>();
    }

    private HashMap<String, Object> obtainObjectFields(Object o){
        HashMap<String, Object> varMap = new HashMap<>();
        Field[] fields = o.getClass().getDeclaredFields();
        for(int i =0 ; i < fields.length; i++){
            try {
                fields[i].setAccessible(true);
                varMap.put(fields[i].getName(), fields[i].get(o));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return varMap;
    }

    public void makeAssertions(){
        for(TestInvariant invariantTester : this.invariantTesters){
            invariantTester.makeAssertion(this.preVarMap, this.postVarMap);
        }
    }

    public void setPreVarMap(Object o){
        this.preVarMap = obtainObjectFields(o);
    }
    public void setPostVarMap(Object o){
        this.postVarMap = obtainObjectFields(o);
    }
    public Map<String, Object> getPreVarMap(){ return this.preVarMap; }
    public Map<String, Object> getPostVarMap(){ return this.postVarMap; }



}
