package TestGenerator.ArgumentCache;

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
import java.util.Collection;
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
 */
public class UniversalTypeAdapterFactory implements TypeAdapterFactory {
    private Set<Class<?>> adapterSet = new HashSet<>();
    private static final String class_token = "__%class__";
    private static final String primitive_token = "__%primobj__";
    private static final String array_token = "__%array__";
    private static final Set<Class<?>> primitiveWrappers = new HashSet<Class<?>>();
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

    static Set<Integer> cyclicDetect = new HashSet<>();
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
        TypeAdapter<T> result = new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    jsonElementAdapter.write(out, null);
                }
                else {
                    TypeAdapter adapter = gson.getDelegateAdapter(thisTypeAdapterFactory, TypeToken.get(value.getClass()));
                    System.out.println("value= " + value + " type=" + value.getClass());
                    if (!primitiveWrappers.contains(value.getClass()) && !value.getClass().isArray() && !value.getClass().equals(String.class)){
                        if (cyclicDetect.contains(System.identityHashCode(value))){
                            System.out.println("CYCLE DETECTED IN");
                            System.exit(1);
                        } else cyclicDetect.add(System.identityHashCode(value));
                    }
                    JsonElement jsonElement = adapter.toJsonTree(value);
                    JsonElement o = null;
                    if (jsonElement.isJsonPrimitive()) {
                        JsonObject jsonPrimitiveWrapper = new JsonObject();
                        jsonPrimitiveWrapper.add(primitive_token, jsonElement.getAsJsonPrimitive());
                        jsonPrimitiveWrapper.add(class_token, new JsonPrimitive(value.getClass().getName()));
                        o = jsonPrimitiveWrapper;

                    } else if (jsonElement.isJsonObject()){
                        o = jsonElement.getAsJsonObject();
                        ((JsonObject)o).add(class_token, new JsonPrimitive(value.getClass().getName()));
                    } else if (jsonElement.isJsonArray()){
                        JsonArray jsonArray = jsonElement.getAsJsonArray();
                        JsonObject arrayWrapper = new JsonObject();
                        arrayWrapper.add(array_token, jsonArray);
                        arrayWrapper.add(class_token, new JsonPrimitive(value.getClass().getName()));
                        o = arrayWrapper;

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
//                    System.out.println(je.getClass().getSimpleName() + " " + je);
                    if (je.isJsonNull()) {
                        return null;
                    } else if (je.isJsonObject()) {
                        JsonObject o = je.getAsJsonObject();

                        String className = o.get(class_token).getAsString();
                        Class clazz = Class.forName(className);
                        TypeAdapter adapter = gson.getDelegateAdapter(thisTypeAdapterFactory, TypeToken.get(clazz));
//                        System.out.println("Adapter: " + adapter.getClass());
                        // parse wrapped primitive wrapper from jsonObject -> jsonPrimitive
                        if (o.has(primitive_token)) {
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
                        throw new IllegalArgumentException("JsonElement object must be either JsonObject or JsonNull, is currently "
                                + je.getClass().getSimpleName() + " JsonElement -> " + je);

                    }
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
                return null;
            }
        };
        return (TypeAdapter<T>) result;
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
//            for(Field nextField : field.get)
        }

        return null;
    }

    public static <T> Object deserializeObjectArray(Object o, Class<T> o_class, Gson gson) throws ClassNotFoundException{

        Function<Object, Boolean> isObjectArray = (Object obj) -> {
            return obj.getClass().isArray() && obj.getClass().getName().contains(Object[].class.getName());
        } ;

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
        if (gson == null){
            GsonBuilder gb = new GsonBuilder();
            gb.registerTypeAdapterFactory(new UniversalTypeAdapterFactory());
            gson = gb
                    .serializeNulls()
                    .enableComplexMapKeySerialization()
                    .create();
        }
        if (oType.getName().equals("java.util.Arrays$ArrayList")){
            return (T)Arrays.asList(deserialize(json, ArrayList.class, gson).toArray());
        }
        T o = gson.fromJson(json, oType);
        o = (T)deserializeObjectArray(o, o.getClass(), gson);
        return o;
    }

    /**
     * Example/Test classes
     * NOTE: nothing to do with Ponzu
     *
     * */

    public static abstract class Tree implements Serializable {

        public Tree L; public Tree R;
        private static final long serialVersionUID = -6074996219705033171L;
        public Tree(Tree L, Tree R){ this.L = L; this.R = R; }
    }

    public static interface SomeTree {
        public void hi();
    }

    public static class BinTree extends Tree{
        public int value;
        public SomeTree st;
        public BinTree(Tree L, int value, Tree R){ super(L, R); this.value = value; }
        @Override public String toString() { return String.format("(%s L=%s R=%s)", value, L!=null ? L.toString() : null, R!= null ? R.toString() : null);}
        @Override public boolean equals(Object o){
            if (o instanceof BinTree){
                BinTree bt = (BinTree)o;
                return Arrays.asList(new Boolean[]{
                        (this.L == bt.L || this.L.equals(bt.L)),
                        (this.R == bt.R || R.equals(bt.R)),
                        this.value == bt.value})
                        .stream()
                        .reduce((a, b) -> a && b).get();
            }
            return false;
        }
    }

    public static class ScalaTree extends Tree {
        public int[] values;
        public ScalaTree(Tree L, int[] values, Tree R){ super(L, R); this.values = values; }

        @Override public String toString() { return String.format("(%s L=%s R=%s)", Arrays.toString(values),  L!=null ? L.toString() : null, R!= null ? R.toString() : null);}
        @Override public boolean equals(Object o){
            if (o instanceof ScalaTree){
                ScalaTree bt = (ScalaTree)o;
                return Arrays.asList(new Boolean[]{
                        (this.L == bt.L || this.L.equals(bt.L)),
                        (this.R == bt.R || R.equals(bt.R)),
                        Arrays.equals(this.values, bt.values)})
                        .stream()
                        .reduce((a, b) -> a && b).get();
            }
            return false;
        }

    }

    public static class DeqNode {
        public DeqNode next;
        public DeqNode prev;
        public Integer x;
        public DeqNode(){}
        public DeqNode(Integer x){this.x=x;}
        public void add(Integer x){
            this.next = new DeqNode(x);
            this.next.prev = this;
        }

        @Override
        public String toString(){
            return String.format("[%s <- %s (%s) -> %s] <-> %s", System.identityHashCode(this.prev), System.identityHashCode(this),
                    this.x, System.identityHashCode(this.next), this.next == null ? null : this.next.toString());
        }
    }

    public static class Delt {
        private Comparable[] A;
        public Delt(Comparable[] A){
            this.A = A;
        }
    }

    public static void main(String[] args) throws Exception{
        Tree l1 = new ScalaTree(null, new int[]{3,2,2}, null);
        Tree l12 = new ScalaTree(null, new int[]{99, 199, 299}, null);
        Tree l2 = new BinTree(null, 1337, null);
        Tree l22 = new BinTree(null, 9, null);
        Tree t = new BinTree(l2, 1, l22);
        Tree t2 = l12;
        t2.L = t; t2.R = l1;

        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapterFactory(new UniversalTypeAdapterFactory());
        Gson gson = gb
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .setPrettyPrinting()
//                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT)
                .create();


        String json = "";


        Object[] Os = new Object[]{"x", 1, 'c'};
        json = gson.toJson(Os);
        Object[] _Os = (Object[])deserialize(json, Os.getClass(), gson);
        System.out.println("Testing.. " + Arrays.asList(_Os));
        assert(Arrays.asList(_Os).equals(Arrays.asList(Os)));

        Object[][] Oss = new Object[][]{Os, Os};
        json = gson.toJson(Oss);
        List OssCheck = Arrays.asList(Oss).stream().map((Object[] __o) -> Arrays.asList(__o)).collect(Collectors.toList());
        Object[][] _Oss = (Object[][]) deserialize(json, Oss.getClass(), gson);
        List _OssCheck = Arrays.asList(_Oss).stream().map((Object[] __o) -> Arrays.asList(__o)).collect(Collectors.toList());
        System.out.println("Testing.. " + _OssCheck);
        assert(Arrays.asList(OssCheck).equals(Arrays.asList(_OssCheck)));

        List<Object> ol = Arrays.asList(new Object[]{new Integer(2), new Character ('p')});
        json = gson.toJson(ol);
        List<Object> _ol = deserialize(json, ol.getClass(), gson);
        System.out.println("Testing.." + _ol);
        assert(ol.equals(_ol));

//        DeqNode head = new DeqNode(1);
//        head.add(2);
//        System.out.println(head);
//        json = gson.toJson(head);
//        System.out.println(json);


        /*
        json = gson.toJson(t);
        System.out.println(json);
        Tree _t = gson.fromJson(json, t.getClass());
        System.out.println(_t);

        json = gson.toJson(t2);
        System.out.println(json);
        Tree _t2 = gson.fromJson(json, t2.getClass());
        System.out.println(_t2);

        List l = new ArrayList(Arrays.asList(new Integer[]{1,2}));
//        List l = new LinkedList(Arrays.asList(new Integer[]{1,2}));
        json = gson.toJson(l);
        System.out.println(json);
        List _l = gson.fromJson(json, l.getClass());
        System.out.println(_l);

        Set s = new HashSet(Arrays.asList(new Integer[]{1,2}));
        json = gson.toJson(s);
        System.out.println(json);
        Set _s = gson.fromJson(json, s.getClass());
        System.out.println(_s);

        List<List> ll = new LinkedList(Arrays.asList(new Object[]{s, l}));
        json = gson.toJson(ll);
        System.out.println(json);
        List<List> _ll = gson.fromJson(json, ll.getClass());
        System.out.println(_ll);
*/

/*
        Map m = new HashMap<String, Integer>();
        m.put("a",1); m.put("b",2);
//        json = gson.toJson(m);
//        System.out.println(json);
//        Map _m = gson.fromJson(json, m.getClass());
//        System.out.println(_m);

        List m2l = new ArrayList();
        m2l.add(1);
        Map m2 = new HashMap<String, Object>();
        m2.put("a",m); m2.put("b",m2l);
        json = gson.toJson(m2);
        System.out.println(json);
        Map _m2 = gson.fromJson(json, m2.getClass());
        System.out.println(_m2);

/*/

    }
}
