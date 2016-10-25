package TestGenerator;

import TestGenerator.ArgumentCache.ArgumentCacheStream;
import TestGenerator.ArgumentCache.IArgumentCache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by NatchaS on 1/20/16.
 *
 * Generates strings for modbat scala model to load the serialized cache to memory during Modbat runtime.
 * - Formats ArgumentCacheStream and IArgumentCache object for ModbatModelGenerator.
 *
 * TODO: implement signature-cache map.
 * TODO: look into different cases of interface.
 */
public class ModbatArgumentFormatter {

    String filename;
    String streamName;
    String cacheName;
    IArgumentCache cache;

    public ModbatArgumentFormatter(String filename){
        this.filename = filename;
        this.streamName = "argCacheReader";
        this.cacheName = "argCache";
        ArgumentCacheStream stream = new ArgumentCacheStream(filename);
        this.cache = stream.readObject();
    }

    public Map<String, String> getInstanceVarCreationMap() {
        // must maintain map insertion order
        Map<String, String> varStatementMap = new LinkedHashMap<>();
        String streamCreationStatement = String.format("var %s: ArgumentCacheStream = new ArgumentCacheStream(\"%s\")", this.streamName, this.filename);
        String cacheCreationStatement = String.format("var %s: IArgumentCache = %s.readObject().asInstanceOf[IArgumentCache]", this.cacheName, this.streamName);
        varStatementMap.put(this.streamName, streamCreationStatement);
        varStatementMap.put(this.cacheName, cacheCreationStatement);
        return varStatementMap;
    }

    // Forms string of series of arguments input ie. "x, y, z" to be used in the method call f(x,y,z)
    public static String argListToString(List<String> argVariables){

        StringBuffer combinedArgs = new StringBuffer("");
        for(int i = 0; i < argVariables.size(); i++){
            // make sure the variable is a variable for the arguments
            if (ModbatArgumentFormatter.isAParamVariable(argVariables.get(i))){
                combinedArgs.append(argVariables.get(i));
                if (i<argVariables.size()-1) combinedArgs.append(",");
            }
        }
        return combinedArgs.toString();
    }

    public static String getSignatureArrayVariable(Map<String, String> argStatementMap){
        if (argStatementMap.containsKey("signatureArrayVar")){
            return "signatureArrayVar";
        }
        return null;
    }

    public Map<String, String> getArgumentStatementsMap(String methodName, List<String> signatures){
        if (!this.cache.contains(methodName, signatures)){
            System.err.println(cache.toString());
            throw new IllegalArgumentException("No such method " + methodName + ": " + signatures.toString()+  " found.");
        }
        Map<String, String> argStatementMap = new LinkedHashMap<String, String>();

        String signatureListVar = "signatureArrayVar";
        String signatureListStatement = String.format("var %s: java.util.List[String] = new java.util.ArrayList[String]()", signatureListVar);
        argStatementMap.put(signatureListVar, signatureListStatement);
        int nullVarCount = 0;
        for(String signature : signatures){
            argStatementMap.put(String.format("%s_%s",nullVarCount, ModbatModelGenerator.NULL_VAR), String.format("%s.add(\"%s\")", signatureListVar, signature));
            nullVarCount++;
        }

        int variableCount = 0;
        for(int signatureIndex = 0; signatureIndex < signatures.size(); signatureIndex++){
            String signature = signatures.get(signatureIndex);
            String varName = String.format("param_%s", variableCount);

            // HARDCODED java.lang.Comparable -> java.lang.Comparable[_] - require scala wildcard
            if (signature.equals("java.lang.Comparable")){
                signature = String.format("%s[_]", signature);
            }
            //  For the 'cache.get(methodName).get(signatureIndex)' statement:
            // .get(methodName)     -> List<Object> argument objects
            // .get(signatureIndex) -> Object of type corresponding to the signature index.
            String deserializeArgumentStr = String.format(
                    "var %s: %s = %s.get(\"%s\", %s).get(%s).asInstanceOf[%s]",
                    varName,
                    signature,
                    this.cacheName,
                    methodName,
                    signatureListVar,
                    signatureIndex,
                    signature
            );

            argStatementMap.put(varName, deserializeArgumentStr);
            variableCount++;
        }
        return argStatementMap;
    }

    public boolean didCacheMethod(String methodName, List<String> signatures){
        return this.cache.contains(methodName, signatures);
    }

    public static boolean isAParamVariable(String variable){
        return variable.contains("param") && !variable.contains(ModbatModelGenerator.NULL_VAR);
    }


    public String getStreamName() { return this.streamName; }
    public String getCacheName() { return this.cacheName; }

    public static void main(String[] args){
        ArgumentCacheStream stream = new ArgumentCacheStream("StackArObjectMap.ser");
        IArgumentCache cacheMap = (IArgumentCache)stream.readObject();
        System.out.println(cacheMap.toString());
    }
}
