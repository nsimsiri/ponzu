package TestGenerator;

/**
 * Created by NatchaS on 11/29/15.
 */
import DataTypes.MTS;
import DataTypes.MTS_inv_transition;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;

import TestGenerator.ArgumentCache.ArgumentCacheStream;
import com.sun.org.apache.xpath.internal.operations.Mod;
import daikon.PptName;
import daikon.inv.Invariant;
import daikon.inv.binary.twoScalar.TwoScalar;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;

/***
 * TestGenerator class
 * <p>
 * Takes an MTS and generates an FSM  in scala as formatted by modbat. Refer to modebat's read me for more details.
 *  TODO:(2) Finish primitive -> wrapper conversion.
* </p>
* @author Natcha Simsiri
* */
public class ModbatModelGenerator {
    public static boolean verbose = true;
    public static boolean makeInvariantAssertion = false;

    private MTS mts;
    private String instanceName;
    private String classname; //shortened class name, e.g "Integer" from "java.lang.Integer"
    private String packagename; // prefix of fullclassname, e.g "java.lang" from  "java.lang.Integer"
    private Class clazz;
    private String fullClassname; // should be packagename + "." + classname, e.g "java.lang.Integer"

    private final String[] IMPORT_STATEMENTS = new String[]{
            "modbat.dsl._",
            "TestGenerator.ArgumentCache._",
            "TestGenerator._"
    };

    private final String TRACE_MARKER = "";//"[TRACE]";

    private ModbatArgumentFormatter argumentFormatter;
    private HashMap<String, String> primitiveWrapperMap;

    public static final String NULL_VAR = "NULL_VAR"; // if the statement has no variable, i.e just a method call
    private static Set<String> primitiveTypes = new HashSet<String>(
            (Arrays.asList(new String[]{
                    "int",
                    "double",
                    "byte",
                    "float",
                    "short",
                    "long",
                    "boolean",
                    "char"
            }))
    );

    public ModbatModelGenerator(MTS mts, String packagename, String classname){
        this.mts = mts;
        this.instanceName = String.format("%sInstance", mts.getName());
        this.classname = classname;
        this.packagename = packagename;
        this.fullClassname = packagename + "." + classname;
        String fullClassnameUnderscoreFormat = String.format("%s_%s", packagename, classname); // for reading Serialized File.
        this.argumentFormatter= new ModbatArgumentFormatter(ArgumentCacheStream.getDefaultNaming(fullClassname));

        // scala doesn't recognize java's primitive types, only uses wrappers.
        // This hash table maps the primitive type to its respective wrapper types.
        this.primitiveWrapperMap = new HashMap<String, String>();
        this.primitiveWrapperMap.put("int", "java.lang.Integer");
        this.primitiveWrapperMap.put("double", "java.lang.Double");
        this.primitiveWrapperMap.put("byte", "java.lang.Byte");
        try {
            this.clazz = Class.forName(fullClassname);
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public static String getPrimitiveTypeWrapper(String type){
        if (type.equals("int")) type="Integer";
        type = Character.toUpperCase(type.charAt(0))+type.substring(1);
        return String.format("java.lang.%s", type);
    }

    public static boolean isPrimitive(String type){return primitiveTypes.contains(type);}



    /**
     * create a string of series of method call on the SUT (System Under Test, method names are parsed from Daikon's Ppt)
     * Serves as a helper method to form modbat's scala transition block.
     *
     * @param fromState (required) MTS_state that method comes from
     * @param toState (required) MTS_state (node) the method from the transition goes into.
     * @param lines (required) statements for the block,
     */
    private String formFSMTransitionBlock(int fromState, int toState, ArrayList<String> lines){
        StringBuffer innerMethodLines = new StringBuffer("");
        for(int i = 0; i < lines.size(); i++){
            innerMethodLines.append("\t\t" + lines.get(i));
            if (i<lines.size()-1) innerMethodLines.append("\n");
        }
        String methodBlock = String.format("\t\"S%s\" -> \"S%s\" := {\n%s\n\t}\n\n", fromState, toState, innerMethodLines.toString());
        return methodBlock;
    }

    /**
     * given a PptName, returns an a string of combined argument variables i.e "param_0, param_1, param_2" such that each params' type matches
     * with the signature list from PptName's method signatures.
     *
     * @param ppt - program point, encodes methods and its type signatures and parameters' type signatures.
     * @param argVariables - variables associated with each statements. variables that is NULL_VAR corresponds to statements that were not assignment statements.
     *                       variables used as parameters to the functional call must be a parameter variable checked by calling ModbatArgumentFormatter.isAParamVariable function.
     * @param argCreationStatements - statements that is written to file.
     * @return
     */

    private List<String> formAndProcessArgumentString(PptName ppt, ArrayList<String> argVariables, ArrayList<String> argCreationStatements,
                                                      Map<String, String> injectedArgStatementMap){
        List<String> methodSignatures = TestGenerator.parseArgSignature(ppt);

        // convert all primitive types to its corresponding wrapper type.
        for(int i = 0; i < methodSignatures.size(); i++){
            String type = methodSignatures.get(i);
            if (this.primitiveTypes.contains(type)) methodSignatures.set(i, getPrimitiveTypeWrapper(type));
        }

        if (methodSignatures.size()>0){
            // maps argument variables to statements that instantiates these variables
            Map<String, String> argStatementMap = argumentFormatter.getArgumentStatementsMap(ppt.getMethodName(),methodSignatures);
            for(Map.Entry<String, String> argStatementEntry : argStatementMap.entrySet()){
                argVariables.add(argStatementEntry.getKey());
                argCreationStatements.add(argStatementEntry.getValue());
                if (injectedArgStatementMap != null){
                    injectedArgStatementMap.put(argStatementEntry.getKey(), argStatementEntry.getValue());
                }
            }
        }
        return argVariables;
    }

    /**
     * takes a program point and does instantiation on the SUT, must also implement TODO (1)
     *
     * @param constructorPpt (required) Daikon top program point, must encode a constructor method
     * @param outGoingState (required) next MTS_state after initial state/state constructor transition goes to.
     * */
    private String formConstructorMethodBlock(PptName constructorPpt, int outGoingState){
        if (!constructorPpt.isConstructor()){
            System.out.println(constructorPpt.toString() + " is not a constructor");
            System.exit(1);
        }

        ArrayList<String> constructorLines = new ArrayList<String>(); // statements to be executed in constructor
        ArrayList<String> argVariables = new ArrayList<String>(); // argument variable
        List<String> argumentList = this.formAndProcessArgumentString(constructorPpt, argVariables, constructorLines, null);
        String combinedArgumentString = ModbatArgumentFormatter.argListToString(argumentList);
        constructorLines.add(String.format("%s = new %s(%s)", this.instanceName, constructorPpt.getMethodName(), combinedArgumentString));
        constructorLines.add(String.format("println(\"%s %s\")", TRACE_MARKER, constructorPpt.getMethodName()));
        return formFSMTransitionBlock(0, outGoingState, constructorLines);
    }

    /** Takes a Daikon.ppt and creates a scala transition block by using the method on the SUT (this.instanceName)
    *  If there are any arguments to the method, an int will be randomized and used as input. Refer to TODO (1).
     *
     *  @param methodPpt (required) Daikon top program point for non-constructor instance methods.
     *  @param fromState (required) MTS_state the method transitions from.tt
     *  @param toState (required) MTS_state the method transitions to.
    */
    private String formInstanceMethodBlock(PptName methodPpt, int fromState, int toState, List<Invariant> postConditions){
        // PPT must not be a constructor.
        if (methodPpt.isConstructor()) return "";

        /*
        * First parses argument signatures to find how many & what kind of args are required. If any
        * arguments needed, use the ModbatArgumentFormatter to create and format arguments*/
        List<String> methodSignatures = TestGenerator.parseArgSignature(methodPpt);
        ArrayList<String> argVariables = new ArrayList<String>(); // argument variable
        ArrayList<String> argCreationStatements = new ArrayList<String>();
        Map<String, String> argStatementMap = new HashMap<>(); // injected to formAndProcessArgumentString. Map contains "variables -> statment";
        List<String> argumentList = formAndProcessArgumentString(methodPpt, argVariables, argCreationStatements, argStatementMap);

        //[INVARIANT ASSERTION] initialize invariant tester
        ModbatAssertionFormatter assertionFormatter = new ModbatAssertionFormatter(postConditions, this.instanceName);
        if (makeInvariantAssertion){
            argCreationStatements.add(assertionFormatter.getInvariantTesterInitializationFormat());
            argCreationStatements.add(assertionFormatter.getInstancePreVarsFormat());
        }

        //METHOD CALLED! make invocation and add arguments. Use formFSMTransitionBlock to format the transition block.
        ModbatModelMethodFormatter methodFormatter = new ModbatModelMethodFormatter(this.instanceName, this.clazz);
        String argumentListVariable = ModbatArgumentFormatter.getSignatureArrayVariable(argStatementMap);
        methodFormatter.formatMethod(methodPpt.getMethodName(), argumentListVariable, methodSignatures, argumentList, argCreationStatements);


        //[INVARIANT ASSERTION] obtain fields' value of instance after method call and make the assertion
        if (makeInvariantAssertion){
            argCreationStatements.add(assertionFormatter.getInstancePostVarsFormat());
            argCreationStatements.add(assertionFormatter.getInvariantTesterAssertionFormat());
        }

        //print to log file
        argCreationStatements.add(String.format("println(\"%s\")", methodPpt.getMethodName()));

        //process and form assertions on method's post conditions
//        System.out.format("[DEBUG INVAS for %s]:\n", methodPpt.getMethodName());
//        for(Invariant inv : postConditions){
//            System.out.format("\t %s , Type: %s\n", inv.toString(), inv.getClass().getCanonicalName());
//            if (TwoScalar.class.isAssignableFrom(inv.getClass())){
//                System.out.format("\t\t ->%s\n", ((TwoScalar)inv).var1().str_name());
//                System.out.format("\t\t ->%s\n", ((TwoScalar)inv).var2().str_name());
//                System.out.format("\t\t ->%s\n", ((TwoScalar)inv).var1().simplify_name());
//                System.out.format("\t\t ->%s\n", ((TwoScalar)inv).var2().simplify_name());
//            }
//        }

        return formFSMTransitionBlock(fromState, toState, argCreationStatements);
    }

    private String formInstanceVariableBlock(Map<String, String> varStatementMap){
        StringBuilder instanceVarDecl = new StringBuilder();
        for (Map.Entry<String,String> varStatement: varStatementMap.entrySet()){
            instanceVarDecl.append("\t"+varStatement.getValue()+"\n"); //uses single indents for instance variables.
        }
        return instanceVarDecl.toString();
    }


    /**
     *  Formats import statements. SUT's importPath is specified by the User.
     *  @param importPath (required) name of the SUT's import path.
     *  */
    private String importStatements(String importPath){
        String importStatements = "";
        for(String importClass : IMPORT_STATEMENTS){
            importStatements += String.format("import %s\n", importClass);
        }
        return importStatements+=String.format("import %s._\n\n", importPath);
//        return String.format("import %s\nimport %s\nimport %s._\n\n", MODBAT_IMPORT_STR, ARGUMENT_CACHE_IMPORT_STR, importPath);
    }

    /**
     *  Main code for MTS to Scala FSM conversion
     * @param path (required) path to write generated scala file to.
     * @param importPath (required) SUT's import path
     */
    public void ouputScalaModel(String path, String importPath){
        String modelName = this.classname+this.mts.getName();
        try {
            BufferedWriter outputFile = new BufferedWriter(new FileWriter(path+modelName+".scala"));
            outputFile.write(importStatements(importPath)); // important import statements.

            /* TODO: obtain classname here */
            Map<String, String> varStatementMap = this.argumentFormatter.getInstanceVarCreationMap();
            varStatementMap.put(instanceName, String.format("var %s: %s = _", instanceName, this.classname));
            String instanceVariableDeclaration = this.formInstanceVariableBlock(varStatementMap);
            outputFile.write(String.format("class %s extends Model {\n%s\n", modelName, instanceVariableDeclaration));

            ArrayList<MTS_state> allStates = mts.getAllStates();

            //Begin writing out the transition for constructors
            ArrayList<MTS_transition> constructorTrans = mts.getAllOutGoing(mts.getInitialState().getName());
            if (constructorTrans.isEmpty()){
                System.out.println("Trace contains no constructor");
                System.exit(1);
            }

            for(MTS_transition constructorTran : constructorTrans){
                PptName constructorPptName = constructorTran.getName();
                assert(constructorPptName!=null);
                outputFile.write(formConstructorMethodBlock(constructorPptName, constructorTran.getEnd()));
            }

            //Go through all instance public methods by simply retrieving all outgoing transitions.
            for(MTS_state currentState : allStates){
                if (mts.isUnreachable(currentState.getName())) continue;
                ArrayList<MTS_transition> transitions = mts.getAllOutGoing(currentState.getName());
                for(MTS_transition transition : transitions){
                    PptName pptname = transition.getName();
                    if (pptname==null){
                        System.out.println("PptName not found in transition - check mts creation");
                        System.exit(1);
                    }

                    if (!pptname.isConstructor()){
                        //
                        List<Invariant> postConditions = null;
                        if (transition instanceof MTS_inv_transition){
                            postConditions = ((MTS_inv_transition)transition).getEventObject().getPostCond();
                        }
                        String instanceMethodBlock = formInstanceMethodBlock(pptname, transition.getStart(), transition.getEnd(), postConditions);
                        outputFile.write(instanceMethodBlock);
                    }
                }
            }

            outputFile.write("}");
            outputFile.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /* Random utlity function to count MTS nodes/vtx...should probably be deleted at some point or find a better place to put this in.*/
    public static HashMap<String, Integer> mtsinfo(MTS mts){
        HashMap<String, Integer> infoMap = new HashMap<String, Integer>();
        infoMap.put("state", mts.getAllStates().size());
        int count = 0;
        for (MTS_state state : mts.getAllStates()){
            count+=mts.getAllOutGoing(state.getName()).size();
        }
        infoMap.put("transition", count);
        return infoMap;

    }
    
    public static void main(String[] args){
    	System.out.println(System.getProperty("java.library.path"));
    }


}
