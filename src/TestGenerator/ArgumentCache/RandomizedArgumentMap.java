package TestGenerator.ArgumentCache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by NatchaS on 1/14/16.
 *
 * Class ArgumentMap: Used on the SUT's wrapper to generate
 */
public class RandomizedArgumentMap implements  Serializable, IArgumentCache {
    private static final long serializedVersionUID = 1L;
    Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> cacheMap;

    public RandomizedArgumentMap(){
        this.cacheMap = new HashMap<MethodSignaturesPair, List<List<ArgumentObjectInfo>>>();
    }

    public void append(String methodName, List<ArgumentObjectInfo> objInfoList){
        List<Class> paramClasses = new ArrayList<Class>();
        for (ArgumentObjectInfo objInfo : objInfoList) {
            paramClasses.add(objInfo.getParamType());
        }
        MethodSignaturesPair key = new MethodSignaturesPair(methodName, paramClasses);
        if (cacheMap.containsKey(key)){
            List<List<ArgumentObjectInfo>> argList = cacheMap.get(key);
            argList.add(objInfoList);
        } else {
            List<List<ArgumentObjectInfo>> argList = new ArrayList<>();
            argList.add(objInfoList);
            cacheMap.put(key, argList);
        }
    }

    public List<Object> get(MethodSignaturesPair key){
        if (this.cacheMap.containsKey(key)) {
            // randomizes cached object
            List<List<ArgumentObjectInfo>> argList = this.cacheMap.get(key);
            Random rand = new Random();
            int randInt = rand.nextInt(argList.size());
            return argList.get(randInt).stream().map(x->x.getObject_()).collect(Collectors.toList());
        } else {
            throw new NullPointerException("No such method name cached: " + key + ".");
        }
    }

    public List<Object> get(String methodName, List<String> argumentType){
        MethodSignaturesPair key = MethodSignaturesPair.fromSignatureString(methodName, argumentType);
        return this.get(key);
    }

    public boolean contains(MethodSignaturesPair key){
        return this.cacheMap.containsKey(key);
    }

    public boolean contains(String methodName, List<String> argumentTypeString){
        MethodSignaturesPair key = MethodSignaturesPair.fromSignatureString(methodName, argumentTypeString);
        return this.contains(key);
    }


    private void readObjectNoData() throws ObjectStreamException{
        throw new NotImplementedException();
    }

    public Set<MethodSignaturesPair> keySet(){
        return this.cacheMap.keySet();
    }

    public Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> getCacheMap() {
        return this.cacheMap;
    }

    @Override
    public String toString(){
        return String.format("[%s methods]", this.cacheMap.entrySet().size()) + this.cacheMap.toString();
    }

    /* Deprecated 3/16 - ArgumentObjectInfo handles arbitrary library type serialization process

    private void writeObject(ObjectOutputStream out) throws IOException{
        Map<MethodSignaturesPair, List<List<String>>> jsonMap = new HashMap<MethodSignaturesPair, List<List<String>>>();
        Gson gson = new Gson();
        for(Map.Entry<MethodSignaturesPair, List<List<Object>>> e : this.cacheMap.entrySet()){
            List<List<String>> jsonArgTuples = new ArrayList<List<String>>();
            for (List<Object> argObjs : e.getValue()){
                List<String> jsonArgTuple = new ArrayList<String>();
                for(Object argObj : argObjs){
                    // Let GSON do the serialization for unknown object type.
                    String jsonArgObj = gson.toJson(argObj);
                    jsonArgTuple.add(jsonArgObj);
                }
                jsonArgTuples.add(jsonArgTuple);
            }
            jsonMap.put(e.getKey(), jsonArgTuples);
        }
        out.writeObject(jsonMap);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Map<MethodSignaturesPair, List<List<String>>> jsonMap = (Map<MethodSignaturesPair, List<List<String>>>) in.readObject();
        this.cacheMap = new HashMap<MethodSignaturesPair, List<List<Object>>>();
        Gson gson = new Gson();
        for(Map.Entry<MethodSignaturesPair, List<List<String>>> e : jsonMap.entrySet()){
            List<Class> signatures = e.getKey().getObjectSignatures(); // must use objectSignatures since paramSignatures may be an interface or superclass.
            List<List<Object>> argTuples = new ArrayList<List<Object>>();
            for(List<String> jsonArgTuple : e.getValue()){
                List<Object> argTuple = new ArrayList<Object>();
                // indexed type from the signatures list must match the type of the serialized object
                // i.e signatures.get(i) equals argTuple.get(i).getClass()
                for(int i = 0; i < signatures.size(); i++){
                    Type type = TypeToken.get(signatures.get(i)).getType();
                    argTuple.add(gson.fromJson(jsonArgTuple.get(i), type));
                }
                argTuples.add(argTuple);
            }
            this.cacheMap.put(e.getKey(), argTuples);
        }
    }
    */

}
