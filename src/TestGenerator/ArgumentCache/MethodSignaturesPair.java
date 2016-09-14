package TestGenerator.ArgumentCache;

import java.io.*;
import java.util.*;

/**
 * Created by NatchaS on 1/23/16.
 *
 * Class MethodSignaturesPair:
 *  - encodes a method name and list its argument types (paramSignatures)
 *  - Used to represent keys in the ArgumentMaps implementation
 *  TODO: refactor MethodSignaturePair.fromSignaturesString to be a method in RandomizedArgumentMap.
 */
public class MethodSignaturesPair implements Serializable {
    private String methodName;
    private List<Class> paramSignatures;

    public MethodSignaturesPair(String methodName, List<Class> paramSignatures){
        this.methodName = methodName;
        this.paramSignatures = paramSignatures;
    }

    // refactor to randomizedArgumentMap.
    public static MethodSignaturesPair fromSignatureString(String methodName, List<String> paramSignatures){
//        System.err.println("== "+paramSignatures);
//        for (String aduh : paramSignatures){
//            System.err.println(String.format("[%s]", aduh));
//        }
        try {
            List<Class> argTypes = new ArrayList<Class>();
            for(String argTypeStr : paramSignatures){
                argTypes.add(Class.forName(argTypeStr));
            }
            return new MethodSignaturesPair(methodName, argTypes);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public String getMethodName(){ return this.methodName; }
    public List<Class> getParamSignatures() { return this.paramSignatures; }

    public List<String> getSignatureNames() {
        List<String> sigNames = new ArrayList<String>();
        for (Class classObj : this.paramSignatures){
            sigNames.add(classObj.getName());
        }
        return sigNames;
    }

    public void addSignatures(List<Class> signatures){
        for(Class classObj : this.paramSignatures){
            this.paramSignatures.add(classObj);
        }
    }

    public void addSignature(Class classObj){
        this.paramSignatures.add(classObj);
    }

    @Override
    public boolean equals(Object obj){
        MethodSignaturesPair msPair = (MethodSignaturesPair)obj;
        if (!msPair.getMethodName().equals(this.methodName)) return false;
        if (msPair.getParamSignatures().size()!=this.paramSignatures.size()) return false;
        List<Class> msPairSigs = msPair.getParamSignatures();

        for(int i = 0; i < this.paramSignatures.size(); i++){
            if (!this.paramSignatures.get(i).equals(msPairSigs.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        StringBuilder sb = new StringBuilder(this.methodName);
        for(Class classObj : this.paramSignatures){
            sb.append(classObj.getName());
        }
        return sb.toString().hashCode();
    }

    @Override
    public String toString(){
        StringBuilder argTuples = new StringBuilder();
        for (int i = 0; i < this.paramSignatures.size(); i++){
            argTuples.append(String.format("%s", paramSignatures.get(i).getName()));
            if (i!=this.paramSignatures.size()-1) argTuples.append(", ");
        }
        return String.format("%s(%s)", this.methodName, argTuples.toString());
    }
}
