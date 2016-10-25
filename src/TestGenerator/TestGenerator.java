package TestGenerator;
import DataTypes.MTS;
import DataTypes.MTS_transition;
import DataTypes.MTS_state;
import DataTypes.MTS_inv_transition;
import DataTypes.Event2;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import daikon.inv.Invariant;
import daikon.PptName;



/**
 * Created by NatchaS on 11/18/15.
 *
 * <p>Custom test generator that takes an MTS an input and does random walk on the MTS. User will have to provide a a ITestInstance
 * object with access to the SUT's intance, and must implement the interface's callbacks. The TestGenerator will do a random-walk
 * and for each transition, it will call the callback of the SUT. At the end of the random-walk it will parse out the invariant of
 * the latest transition and do an invariant check.
 * </p>
 *
 * <p>
 * TODO: (1) - write out the tests to a .java file - so user doesn't have to implement interface.
 * TODO: (2) - Deal with invariants that refer to argument variables.
 * TODO: (3) - Fully implement invokeMethodFromPpt - support multiple arugments.
 * @author Natcha Simsiri
 *</p>
 */


public class TestGenerator {
    private static boolean verbose = false;
    private MTS mts;
    private ITestInstance testInstance;
    private Random rand;
    /** Constructor
    * @param mts (required) Sekt's FSM
    * @param testInstance (required) the interface to the SUT (System Under Test), user must implement this interface.
    * */
    public TestGenerator(MTS mts, ITestInstance testInstance) {
        this.testInstance = testInstance;
        this.mts = mts;
        this.rand = new Random();
    }

    /** obtains type declaration of a ppt
    * @param pptname (required) Daikon's program point obtained from the MTS's transitions/edges
    * */
    public static List<String> parseArgSignature(PptName pptname){
        String pptSignature = pptname.getSignature();
        String args = pptSignature.substring(pptSignature.indexOf("(")+1, pptSignature.indexOf(")"));
        String[] parsedSignatures = args.split(",");
        List<String> signatures = new ArrayList<String>();

        // filter empty strings
        for(int i= 0; i < parsedSignatures.length;i++){
            if (parsedSignatures[i].length()>0) {
                String removedSpaceSignature = parsedSignatures[i].replaceAll("\\s+","");
                signatures.add(removedSpaceSignature);
            }
        }
        System.out.println("SIGNATURES: " + signatures);
        return signatures;
    }


    /** randomizes input for methods that requires and argument. Will make this more robust - TODO (1).
    * @param methodArgSignatures (required) list of types.
    */
    public static ArrayList<Object> randomizeArgumentsFromSignature(List<String> methodArgSignatures){
        ArrayList<Object> args = new ArrayList<Object>();
        for(int i = 0; i < methodArgSignatures.size(); i++){
            String sig = methodArgSignatures.get(i);
            if (sig.contains("java.lang.Object")) {
                args.add(new Integer((int)Math.random()));
            }
        }
        return args;
    }

    /** This method randomly walks through the MTS and invokes calls to the testInstance. It will walk until
    * maxPathLength. To use this the user must implement a wrapper around their Datastructure and implement
    * the ITestInstance interface.
    *
    * @param maxPathLength (required) maximum number of paths the test case wants to do the random walk on.
    * */
    public void genRandomTestCases(int maxPathLength){
        ArrayList<MTS_state> states=  mts.getAllStates();
        MTS_state initState = mts.getInitialState();
        for(int i =1 ; i < states.size(); i++){
            if (!mts.isUnreachable(initState.getName())) break;
            initState = states.get(i);
        }
        if (initState==null){
            System.out.println("unable to find reachable states");
            System.exit(1);
        }

        ArrayList<MTS_transition> initialTransitions = new ArrayList<MTS_transition>();
        Stack<MTS_transition> st = new Stack<MTS_transition>();

        for (MTS_transition initTrans : mts.getAllOutGoing(initState.getName())) {
            PptName pptname = initTrans.getName();
            if (pptname.isConstructor()) initialTransitions.add(initTrans);
        }

        int pathCount =  0;
        MTS_transition currentTransition = initialTransitions.get(this.rand.nextInt(initialTransitions.size()));
        // begin random walk process, terminates on empty stack.
        while(pathCount!=maxPathLength || !st.isEmpty()) {
            boolean isEndOfPath = false;
            MTS_state reachedState = mts.getState(currentTransition.getEnd());

            if (verbose) System.out.format("S%s -> S%s: ", currentTransition.getStart(), currentTransition.getEnd());

            // invoke method for test instance
            PptName pptname = currentTransition.getName();
            String methodName = pptname.getMethodName();
            List<String> methodArgSignatures = parseArgSignature(pptname);
            ArrayList<Object> randomizedArgs = randomizeArgumentsFromSignature(methodArgSignatures);

            HashMap<String, Object> origVarMap = new HashMap<String, Object>();
            if (!pptname.isConstructor()) origVarMap = getFieldsMap(); // pre-value of fields of testIntance

            // calls method to walk through different transitions
            testInstance.invokeMethod(methodName, randomizedArgs);
            if (!pptname.isConstructor()) invokeMethodFromPpt(pptname, randomizedArgs); // <- experimenting without requiring user to input method data in interface.

            HashMap<String, Object> varMap = getFieldsMap(); // post-value of fields of testInstance
            if (pptname.isConstructor()) origVarMap = varMap;

            // Does invariant check if at the end of calls
            pathCount += 1;
            if (pathCount == maxPathLength || isEndOfPath) {
                // evaluate invariant at this point.
                if (currentTransition instanceof MTS_inv_transition) {
                    MTS_inv_transition invTransition = (MTS_inv_transition) currentTransition;
                    Event2 event = invTransition.getEventObject();
                    ArrayList<TestInvariant> testInvariants = new ArrayList<TestInvariant>();
                    for (Invariant postCond : event.getPostCond()) {
                        if (verbose){
                            System.out.format("[%s] -> %s Slice.VAR_INFO: %s\n", postCond.toString(), postCond.varNames(), postCond.ppt.var_infos.length);
                        }
                        TestInvariant testInv = new TestInvariant(postCond);
                        testInv.makeAssertion(origVarMap, varMap);
                    }

                } else {
                    System.out.println("ERR: MTS's transitions need to be annotated with invariants.");
                    System.exit(1);
                }
                if (verbose)
                    System.out.format("end of path length %s at S%s\n", pathCount, reachedState.getName());
                if (pathCount == maxPathLength) break;
            }

            // traverse down reachable paths
            if (mts.getAllOutGoing(reachedState.getName()).size() != 0) {
                ArrayList<MTS_transition> transitions = mts.getAllOutGoing(reachedState.getName());
                int randIndex = this.rand.nextInt(transitions.size());
                currentTransition = transitions.get(randIndex);
                st.push(currentTransition);
            } else {
                st.pop();
            }
        }
    }

    /** invokes a method for test case execution using Java's Reflection. Replaces ITestInterface's invokeMethod function so users no longer have
     * to implement this redundant string check method.
     * @param pptName (required) Daikon's program point information
     * @param args (required) list of generated arguments for the ppt's method.
     * */
    private Object invokeMethodFromPpt(PptName pptName, ArrayList<Object> args){
        String methodName = pptName.getMethodName();
        List<String> argSignatures = parseArgSignature(pptName);
        try {
            if (pptName.isConstructor()){
                throw new UnsupportedOperationException("Construction calls during the random walk has not yet been implemented.");
            } else {
                Method method;
                if (args.size()==0) {
                    method = testInstance.getRuntimeClass().getMethod(methodName);
                    return method.invoke(this.testInstance.getTestInstance()); // no arg method
                }
                else if (args.size()==1){
                    // method with single argument
                    method = testInstance.getRuntimeClass().getMethod(methodName, Class.forName(argSignatures.get(0)));
                    return method.invoke(this.testInstance.getTestInstance(), args.get(0));
                } else if (args.size()==2){
                    // method with two args
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /** Check if a field exists - both public and private
    * @param reflection (required) - SUT's reflection
    * @param fieldName (required) - name of the field for its value look-up.
    * */
    private boolean fieldExistsInClass(Class<?> reflection, String fieldName){
        Field[] fields = reflection.getDeclaredFields();
        try{
            for(int i = 0; i < fields.length; i++){
                fields[i].setAccessible(true);
                if (fieldName.equals(fields[i].getName())) return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    /** Get the value of this field.
    * @param reflection (required) - reflection of the SUT instance, obtained from the user's callback
    * @param fieldName (required) - name of the field for its value look-up.
    * */
    private Object getFields(Class<?> reflection, String fieldName) throws TestInstanceException{
        try{
            Field[] fields = reflection.getDeclaredFields();
            for(int i = 0; i < fields.length; i++){
                fields[i].setAccessible(true);
                if (fieldName.equals(fields[i].getName())) return fields[i].get(this.testInstance.getTestInstance());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        throw new TestInstanceException("unable to find field.");
    }
    /** return a Hashmap of all private/public fields
    * */
    private HashMap<String, Object> getFieldsMap() {
        Class<?> reflection = this.testInstance.getRuntimeClass();
        HashMap<String, Object> fieldMap = new HashMap<String, Object>();
        try {
            Field[] fields = reflection.getDeclaredFields();
            for (int i = 0; i < fields.length; i++){
                fields[i].setAccessible(true);
                Object fieldValue = fields[i].get(this.testInstance.getTestInstance());
                fieldMap.put(fields[i].getName(), fieldValue);
            }
            return fieldMap;
        } catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return null;
    }

}
