package TestGenerator.ArgumentCache;

import TestGenerator.Utility.RandomizedQueue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.org.apache.xpath.internal.Arg;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by NatchaS on 1/14/16.
 *
 * Class ArgumentMap: Used on the SUT's wrapper to generate
 */
public class RandomizedArgumentMap implements Serializable, IArgumentCache {
    private static final long serializedVersionUID = 1L;
    Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> cacheMap;

    public RandomizedArgumentMap(){
        this.cacheMap = new HashMap<MethodSignaturesPair, List<List<ArgumentObjectInfo>>>();
    }

    public RandomizedArgumentMap(Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> cacheMap){
        this.cacheMap = cacheMap;
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

    public List<Object> get(String methodName, List<String> argumentType, List<Function<Object, Boolean>> argumentFilters){
        MethodSignaturesPair key = MethodSignaturesPair.fromSignatureString(methodName, argumentType);
        return _get(key, argumentFilters);
    }

    public List<Object> get(String methodName, List<String> argumentType){
        return this.get(methodName, argumentType, null);
    }

    private List<Object> _get(MethodSignaturesPair key, List<Function<Object, Boolean>> argumentFilters){
        if (this.cacheMap.containsKey(key)) {
            // randomizes cached object
            Random rand = new Random();
            List<List<ArgumentObjectInfo>> argList = this.cacheMap.get(key);

            // argument filters passed
            if (argumentFilters != null && !argumentFilters.isEmpty()){
                List<Object> filteredArguments = new ArrayList<>();
                for(int i = 0; i < argumentFilters.size(); i++){
                    Function<Object, Boolean> filter = argumentFilters.get(i);
                    rand.nextInt(argList.size());
                    final int _i = i;
                    Queue<Object> nArgRandQueue = new RandomizedQueue<Object>(
                            argList
                            .stream()
                            .map((List<ArgumentObjectInfo> argList_i) -> argList_i.get(_i))
                            .collect(Collectors.toList())
                    );

                    // iterate the RandomizedQueue, cont. next arg set if satisfiable
                    boolean foundSatisfiableArg = false;
                    while(!nArgRandQueue.isEmpty()){
                        Object randElm_arg_i = nArgRandQueue.poll();
                        if (foundSatisfiableArg = filter.apply(randElm_arg_i)){
                            filteredArguments.add(randElm_arg_i);
                            break;
                        }
                    }
                    if (!foundSatisfiableArg){
                        throw new NoSuchElementException(String.format("No elements from key: %s satisfies filter %s on element: %s\n", key, argumentFilters, i));
                    }

                }
                return filteredArguments;
            }

            // no arg filters
            return argList.get(rand.nextInt(argList.size()))
                    .stream()
                    .map(x->x.getObject_())
                    .collect(Collectors.toList());
        }

        throw new NullPointerException("No such method name cached: " + key + ".");
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
