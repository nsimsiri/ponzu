package TestGenerator.ArgumentCache;

import TestGenerator.UnitTests.UniversalTypeAdapterFactoryTest;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.IllegalClassException;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by NatchaS on 7/11/16.
 *TODO: cannot serialize cyclic reference of arrays and primitive objects. i.e B = [A,..] and A = [B,..]
 *TODO: cannot serialize complex enum type with cyclic references.
 */
public class UniversalTypeAdapterFactory implements TypeAdapterFactory {
    private Set<Class<?>> adapterSet = new HashSet<>();
    public static final String class_token = "__%class__";
    public static final String primitive_token = "__%primobj__";
    public static final String array_token = "__%array__";
    private static final String ref_token = "__%ref__";
    private static final String has_ref_token = "__%has_ref__";
    public static final Set<Class<?>> primitiveWrappers = new HashSet<Class<?>>();

    public ThreadLocal<GraphAdapterBuilder.Graph> referenceGraphThread;
    public ThreadLocal<Set<String>> allClassNamesThread;

    static {
        primitiveWrappers.add(Integer.class);
        primitiveWrappers.add(Character.class);
        primitiveWrappers.add(Byte.class);
        primitiveWrappers.add(Double.class);
        primitiveWrappers.add(Byte.class);
        primitiveWrappers.add(Long.class);
        primitiveWrappers.add(Short.class);
        primitiveWrappers.add(Boolean.class);
        primitiveWrappers.add(Float.class);
    }

    public UniversalTypeAdapterFactory() {
    }

    public UniversalTypeAdapterFactory(Set<Class<?>> adapterSet) {
        this.adapterSet = adapterSet;
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        // ignore java library's types, adapter only for foreign object type.
        if (type != null){
            if (type.getRawType().isArray())
                return null;
        }

        TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        TypeAdapterFactory thisTypeAdapterFactory = this;
        TypeAdapter<T> result = new UniversalTypeAdapter(gson, jsonElementAdapter, thisTypeAdapterFactory);
        if (referenceGraphThread != null){
            result = new UniversalTypeAdapter<T>(gson, jsonElementAdapter, thisTypeAdapterFactory, referenceGraphThread, allClassNamesThread);
        }
        if(allClassNamesThread != null){
            ((UniversalTypeAdapter)result).allClassNameThread = allClassNamesThread;
        }
        return (TypeAdapter<T>) result;
    }

    public static class UniversalTypeAdapter<T> extends TypeAdapter<T>{
        public Gson gson;
        public TypeAdapter<JsonElement> jsonElementAdapter;
        public TypeAdapterFactory thisTypeAdapterFactory;
        public ThreadLocal<GraphAdapterBuilder.Graph> referenceGraphThread;
        public ThreadLocal<Set<String>> allClassNameThread;

        public UniversalTypeAdapter(Gson gson, TypeAdapter<JsonElement> jsonElementAdapter, TypeAdapterFactory thisTypeAdapterFactory,
                                    ThreadLocal<GraphAdapterBuilder.Graph> referenceGraphThread, ThreadLocal<Set<String>> allClassNameThread){
            this.allClassNameThread = allClassNameThread;
            this.referenceGraphThread = referenceGraphThread;
            this.gson = gson;
            this.jsonElementAdapter = jsonElementAdapter;
            this.thisTypeAdapterFactory = thisTypeAdapterFactory;
        }

        public UniversalTypeAdapter(Gson gson, TypeAdapter<JsonElement> jsonElementAdapter, TypeAdapterFactory thisTypeAdapterFactory){
            this.gson = gson;
            this.jsonElementAdapter = jsonElementAdapter;
            this.thisTypeAdapterFactory = thisTypeAdapterFactory;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {

            if (value == null) {
                jsonElementAdapter.write(out, null);
            }
            else {
                boolean hasReference = false;
                TypeAdapter adapter = gson.getDelegateAdapter(thisTypeAdapterFactory, TypeToken.get(value.getClass()));
//                System.out.printf("<%s>\n[%s] %s\n", adapter, (value == null) ? null : value.getClass(), Arrays.asList(value));
                JsonElement o = null;
                if (hasReference){
                    o = new JsonObject();
                    ((JsonObject)o).add(class_token, new JsonPrimitive(value.getClass().getName()));
                    ((JsonObject)o).add(ref_token, new JsonPrimitive(System.identityHashCode(value)));
                    ((JsonObject)o).add(has_ref_token, new JsonPrimitive(true));
                } else {
                    out.setLenient(true);
                    JsonElement jsonElement = adapter.toJsonTree(value);
                    if (jsonElement.isJsonPrimitive()) {
                        JsonObject jsonPrimitiveWrapper = new JsonObject();
                        jsonPrimitiveWrapper.add(primitive_token, jsonElement.getAsJsonPrimitive());
                        jsonPrimitiveWrapper.add(class_token, new JsonPrimitive(value.getClass().getName()));
                        if (allClassNameThread != null) allClassNameThread.get().add(value.getClass().getName());
                        o = jsonPrimitiveWrapper;

                    } else if (jsonElement.isJsonObject()){
                        o = jsonElement.getAsJsonObject();
                        ((JsonObject)o).add(class_token, new JsonPrimitive(value.getClass().getName()));
                        if (allClassNameThread != null) allClassNameThread.get().add(value.getClass().getName());
//                            ((JsonObject)o).add(ref_token, new JsonPrimitive(System.identityHashCode(value)));

                    } else if (jsonElement.isJsonArray()){
                        JsonArray jsonArray = jsonElement.getAsJsonArray();
                        JsonObject arrayWrapper = new JsonObject();
                        arrayWrapper.add(array_token, jsonArray);
                        arrayWrapper.add(class_token, new JsonPrimitive(value.getClass().getName()));
                        if (allClassNameThread != null)  allClassNameThread.get().add(value.getClass().getName());
                        o = arrayWrapper;
                    }
                }
                jsonElementAdapter.write(out, o);
            }
        }

        @Override
        public T read(JsonReader jsonReader) throws IOException {
            try {
                // treat null value case;
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    return null;
                }

                JsonElement je = jsonElementAdapter.read(jsonReader);
                if (je.isJsonNull()) {
                    return null;
                } else if (je.isJsonObject()) {
                    JsonObject o = je.getAsJsonObject();

                    String className = o.get(class_token).getAsString();
                    Class clazz = Class.forName(className);
                    TypeAdapter adapter = gson.getDelegateAdapter(thisTypeAdapterFactory, TypeToken.get(clazz));


                    // parse wrapped primitive wrapper from jsonObject -> jsonPrimitive

                    if (o.has(primitive_token)) {
                        if (clazz.equals(Double.class) || clazz.equals(double.class)){
                            adapter = gson.getAdapter(clazz);
                        }

                        return (T) adapter.fromJsonTree(o.get(primitive_token));
                    } else if (o.has(array_token) &&  Collection.class.isAssignableFrom(clazz)){

                        Collection genericCollection = (Collection) adapter.fromJsonTree(new JsonArray());
                        JsonArray ja = o.get(array_token).getAsJsonArray();

                        for(int i = 0; i < ja.size(); i++){
                            JsonElement je_i = ja.get(i);
                            String className_i = je_i.getAsJsonObject().get(class_token).getAsString();
                            Class clazz_i = Class.forName(className_i);
                            genericCollection.add(gson.fromJson(je_i, clazz_i));
                        }

                        return (T)genericCollection;
                    } else if (o.has(array_token) && Map.class.isAssignableFrom(clazz)){
                        JsonArray ja = o.get(array_token).getAsJsonArray();
                        Map genericMap =  (Map) adapter.fromJsonTree(new JsonArray());

                        for(int i = 0; i < ja.size(); i++){
                            JsonArray key_value_tuple = ja.get(i).getAsJsonArray();
                            JsonObject key_obj = (JsonObject) key_value_tuple.get(0);
                            JsonObject value_obj = (JsonObject) key_value_tuple.get(1);
                            Object key_real_obj = gson.fromJson(key_obj, Class.forName(key_obj.get(class_token).getAsString()));
                            Object value_real_obj = gson.fromJson(value_obj, Class.forName(value_obj.get(class_token).getAsString()));

                            genericMap.put(key_real_obj, value_real_obj);

                        }
                        return (T)genericMap;
                    }
                    return (T) adapter.fromJsonTree(o);
                } else {
                    if (je.isJsonPrimitive() && GraphAdapterBuilder.Graph.isName(je.getAsString()) && referenceGraphThread != null){
                        GraphAdapterBuilder.Graph referenceGraph = referenceGraphThread.get();
                        GraphAdapterBuilder.Element<?> e = referenceGraph.map.get(je.getAsString());
                        if (e == null){
                            throw new NullPointerException("GraphAdapterBuilder.Element cannot be null");
                        }
                        if (e.value == null){
                            if (e.typeAdapter == null){
                                e.typeAdapter = (TypeAdapter)this;
                            }
                            e.read(referenceGraph);
                        }
                        return (T)e.value;
                    }

                }
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            return null;
        }
    };

    public static class SpecialDoubleAdapter extends TypeAdapter<Double> {
        TypeAdapter<JsonElement> jsonElementAdapter;
        public SpecialDoubleAdapter(TypeAdapter<JsonElement> jsonElementAdapter){
            this.jsonElementAdapter = jsonElementAdapter;
        }
        @Override public void write(JsonWriter out, Double value) throws IOException {
            out.setLenient(true);
            String doubleClassName = (value == null) ? double.class.getName() : value.getClass().getName();
            JsonElement je = (value == null) ? null : new JsonPrimitive(value);
            JsonObject jo = new JsonObject();
            jo.add(class_token, new JsonPrimitive(doubleClassName));
            jo.add(primitive_token, je);
            jsonElementAdapter.write(out, jo);

        }
        @Override public Double read(JsonReader in) throws IOException {
            JsonElement jsonElement = this.jsonElementAdapter.read(in);
            if (jsonElement.isJsonPrimitive()){
                return jsonElement.getAsDouble();
            } else if (jsonElement.isJsonObject()){
                JsonObject doubleWrapper = jsonElement.getAsJsonObject();
                if (doubleWrapper.get(primitive_token).isJsonNull()) return null;
                return Double.parseDouble(doubleWrapper.get(primitive_token).getAsString());
            }
            throw new IllegalStateException("Expected JsonPrimitive or JsonObject, but it is a " + jsonElement.getClass().getSimpleName());
        }
    }

    /**
     * TODO: Allow overriding same name more than TWICE.
     * */
    public static class DuplicateFieldNamingStrategy implements FieldNamingStrategy {
        @Override
        public String translateName(Field field){
            Class<?> curClass = field.getDeclaringClass();
            Class<?> parClass = curClass.getSuperclass();
            if (parClass != null){
                Set<String> superClassFields = new HashSet<>(
                        (Collection) Arrays.asList(parClass.getDeclaredFields())
                                .stream().map((Field f) -> f.getName()).collect(Collectors.toList())
                );
                Field[] superFields = parClass.getDeclaredFields();
                for(int i = 0; i < superFields.length; ++i){
                    if (superFields[i].getName().equals(field.getName())){
                        return "dup$"+field.getName();
                    }
                }

            }
            return field.getName();
        }
    }


    public static Gson buildGson() {
        return buildGson(null, null);
    }
    public static Gson buildGson(Object obj){
        return buildGson(obj, null);
    }
    public static Gson buildGsonWithKnownClasses(Set<String> trackedClasses, Integer numberCyclicOfReferences){
        ThreadLocal<Integer> numberOfCyclicReferencesWrapper = new ThreadLocal<>();
        numberOfCyclicReferencesWrapper.set(numberCyclicOfReferences);
        return buildGson(null, trackedClasses, numberOfCyclicReferencesWrapper);
    }
    public static Gson buildGson(Object obj, Set<String> trackedClasses){
        return buildGson(obj, trackedClasses, null);
    }

    /***
     * Two cases:
     * 1) obj is not null and trackedClasses empty -> serialize.
     * 2) obj is null, trackedClasses already has clases -> deserialize.
     */
    public static Gson buildGson(Object obj, Set<String> trackedClasses, ThreadLocal<Integer> numberOfCyclicReferencesWrapper){
        if (numberOfCyclicReferencesWrapper == null){
            numberOfCyclicReferencesWrapper = new ThreadLocal<Integer>();
            numberOfCyclicReferencesWrapper.set(0);
        }
        Set<String> _class_set = new HashSet<>();
        // Serialization Case: we want to obtain all Non-java classes and have them added to trackedClasses set.
        if (obj != null) {
            _class_set = ClassRetrievalAdapterFactory.getUniqueClassname(obj, numberOfCyclicReferencesWrapper);
            if (trackedClasses!=null) trackedClasses.addAll(_class_set);
        }
        // Deserialization case: there's trackedClasses but object isn't passed/null.
        // if object is null, we are in former case, but usually trackedClasses will be empty.
        if (trackedClasses!=null){
            _class_set = trackedClasses;
        }

        final Set<String> class_set = _class_set;

        Function<Set<String>, GraphAdapterBuilder> gab_builder = (Set<String> class_set_) -> {
            GraphAdapterBuilder gab = new GraphAdapterBuilder();
            for(String class_name : class_set){
                try {
                    gab.addType(Class.forName(class_name));
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
            }
            return gab;
        };

        GsonBuilder gb = new GsonBuilder();
        UniversalTypeAdapterFactory utaf = new UniversalTypeAdapterFactory();

        if (!class_set.isEmpty() && numberOfCyclicReferencesWrapper.get() > 0){
            gab_builder.apply(class_set)
                    .registerUniversalAdapter(utaf)
                    .registerOn(gb);
        }

        FieldNamingStrategy exclusionStrategy = new UniversalTypeAdapterFactory.DuplicateFieldNamingStrategy();
        SpecialDoubleAdapter doubleAdapter = new SpecialDoubleAdapter(new Gson().getAdapter(JsonElement.class));
        gb.setFieldNamingStrategy(exclusionStrategy);
        gb.registerTypeAdapterFactory(utaf);
        gb.registerTypeAdapter(Double.class, doubleAdapter);
        gb.registerTypeAdapter(double.class, doubleAdapter);

        Gson gson = gb
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create();
        return gson;
    }


    public static boolean isForeignClass(Class<?> clz){
        String packageName = clz.getPackage().getName();
        return clz.isPrimitive() || clz.isArray() || packageName.equals("java.util") || packageName.equals("java.lang");
    }

    private static interface ICallable{ void call(); }

    private static boolean isCallable(ICallable body){
        try {
            body.call();
            return true;
        } catch (Exception e){}
        return false;
    }

    // Don't remember what this idea was - maybe delete
    public static <T> Set<Class<?>> classesFromObjectTree(T o){
        Set<Class<?>> objDependenciesSet = new HashSet<>();
        Queue<Field> q = new LinkedList<>();
        for(Field field : o.getClass().getDeclaredFields()){
            q.add(field);
        }

        while(!q.isEmpty()){
            Field field = q.poll();
            Class<?> declaredClass = field.getDeclaringClass();
            Class<?> objectClass = field.getType();
            if (isForeignClass(declaredClass)){
                objDependenciesSet.add(declaredClass);
            }
            if (isForeignClass(objectClass)){
                objDependenciesSet.add(objectClass);
            }
        }
        return null;
    }

    public static <T> Object deserializeObjectArray(Object o, Class<T> o_class, Gson gson) throws ClassNotFoundException{
        Function<Object, Boolean> isObjectArray = (Object obj) -> {
            return obj.getClass().isArray() && obj.getClass().getName().contains(Object[].class.getName());
        };

        if (isObjectArray.apply(o)){
            Object[] _o = (Object[]) o;
            for(int i = 0; i < _o.length; i++){
                if (_o[i].getClass().equals(LinkedTreeMap.class) && ((LinkedTreeMap)_o[i]).containsKey(class_token)){
                    LinkedTreeMap ltm = (LinkedTreeMap)_o[i];
                    Class<?> ltm_class = Class.forName((String)ltm.get(class_token));
                    Gson gson2 = new Gson();
                    JsonObject jo = gson2.toJsonTree(ltm).getAsJsonObject();
                    Object ltm_o = gson.fromJson(jo, ltm_class);
                    _o[i] = ltm_o;
                } else if (isObjectArray.apply(_o[i])){
                    _o[i] = deserializeObjectArray(_o[i], o_class, gson);
                }
            }
            return (T)_o;
        }
        return (T)o;
    }
    public static <T> T deserialize(String json, Class<T> oType, Gson gson) throws ClassNotFoundException{
        if (oType.getName().equals("java.util.Arrays$ArrayList")){
            return (T)Arrays.asList(deserialize(json, ArrayList.class, gson).toArray());
        }
        T o = gson.fromJson(json, oType);
        o = (T)deserializeObjectArray(o, o.getClass(), gson);
        return o;
    }

}
