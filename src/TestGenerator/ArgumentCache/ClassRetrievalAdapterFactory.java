package TestGenerator.ArgumentCache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by NatchaS on 9/20/16.
 */
public class ClassRetrievalAdapterFactory implements TypeAdapterFactory {
    public Set<String> uniqueClassSet;
    final Set<Integer> uniqueObjectSet;
    public ThreadLocal<Integer> numberOfCyclicReferencesThread;

    public ClassRetrievalAdapterFactory(Set<String> uniqueClassSet){
        this.uniqueClassSet = uniqueClassSet;
        this.uniqueObjectSet = new HashSet<Integer>();
        this.numberOfCyclicReferencesThread = new ThreadLocal<>();
        this.numberOfCyclicReferencesThread.set(0);
    }

    public static class ClassRetrievalAdapter<T> extends TypeAdapter<T> {
        public Gson gson;
        public TypeAdapter<JsonElement> jsonElementAdapter;
        public TypeAdapterFactory thisTypeAdapterFactory;
        public ThreadLocal<Set<String>> uniqueClassSetThread;
        public ThreadLocal<Set<Integer>> uniqueObjectSetThread;
        public ThreadLocal<Integer> numberOfCyclicReferencesThread;

        public ClassRetrievalAdapter(Gson gson, TypeAdapterFactory thisTypeAdapterFactory, TypeAdapter<JsonElement> jsonElementTypeAdapter,
                                     Set<String> uniqueClassSet, Set<Integer> uniqueObjectSet, ThreadLocal<Integer> numberOfCyclicReferencesThread){
            this.uniqueClassSetThread = new ThreadLocal<>();
            this.uniqueClassSetThread.set(uniqueClassSet);
            this.uniqueObjectSetThread = new ThreadLocal<>();
            this.uniqueObjectSetThread.set(uniqueObjectSet);
            this.gson = gson;
            this.jsonElementAdapter = jsonElementTypeAdapter;
            this.thisTypeAdapterFactory = thisTypeAdapterFactory;
            this.numberOfCyclicReferencesThread = numberOfCyclicReferencesThread;

        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.setLenient(true);
            Set<Integer> uniqueObjectSet = this.uniqueObjectSetThread.get();
            if (value == null){
                jsonElementAdapter.write(out, null);
            } else {
                TypeToken typeToken = TypeToken.get(value.getClass());
                TypeAdapter adapter = gson.getDelegateAdapter(thisTypeAdapterFactory, typeToken);

                boolean isPrimitive = UniversalTypeAdapterFactory.primitiveWrappers.contains(value.getClass()) ||
                                      value.getClass().equals(String.class) ||
                                      value.getClass().isArray() ||
                                      value.getClass().isEnum() ||
                                      Class.class.isAssignableFrom(value.getClass());

                if (!isPrimitive) ClassRetrievalAdapter.addUniqueClass(this.uniqueClassSetThread, typeToken.getRawType());
                if (!uniqueObjectSet.contains(System.identityHashCode(value))) {
                    if (!isPrimitive){
                        uniqueObjectSet.add(System.identityHashCode(value));
                    }
                    JsonElement jsonElement = adapter.toJsonTree(value);
                    jsonElementAdapter.write(out, new JsonPrimitive(typeToken.getRawType().getSimpleName()));
                } else {
                    this.numberOfCyclicReferencesThread.set(this.numberOfCyclicReferencesThread.get()+1);
                    jsonElementAdapter.write(out, new JsonPrimitive("null"));
                }
            }
        }

        public static void addUniqueClass(ThreadLocal<Set<String>> uniqueClassSetThread, Class<?> clz){
            if (uniqueClassSetThread!=null){
                Set<String> uniqueClassSet = uniqueClassSetThread.get();
                if (uniqueClassSet!=null){
                    uniqueClassSet.add(clz.getName());
                }
            }
        }

        @Override
        public T read(JsonReader in) throws IOException{
            return null;
        }
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type){
        TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
        TypeAdapterFactory thisTypeAdapterFactory = this;
        ClassRetrievalAdapter classRetrievalAdapter = new ClassRetrievalAdapter(gson,  thisTypeAdapterFactory,
                jsonElementTypeAdapter, this.uniqueClassSet, this.uniqueObjectSet, this.numberOfCyclicReferencesThread);
        return (TypeAdapter<T>)classRetrievalAdapter;
    }

    public static Set<String> getUniqueClassname(Object obj){
        return getUniqueClassname(obj, null);
    }

    public static Set<String> getUniqueClassname(Object obj, ThreadLocal<Integer> numberOfCyclicReferencesThread){
        Set<String> uniqueClassSet = new HashSet<>();
        ClassRetrievalAdapterFactory classRetrievalAdapterFactory = new ClassRetrievalAdapterFactory(uniqueClassSet);
        UniversalTypeAdapterFactory.SpecialDoubleAdapter doubleAdapter=
                new UniversalTypeAdapterFactory.SpecialDoubleAdapter(new Gson().getAdapter(JsonElement.class));
        GsonBuilder gb = new GsonBuilder()
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .serializeSpecialFloatingPointValues();

        gb.setFieldNamingStrategy(new UniversalTypeAdapterFactory.DuplicateFieldNamingStrategy());
        gb.registerTypeAdapterFactory(new UniversalClassAdapterFactory());
        gb.registerTypeAdapterFactory(classRetrievalAdapterFactory);
        gb.registerTypeAdapter(Double.class, doubleAdapter);
        gb.registerTypeAdapter(double.class, doubleAdapter);

        Gson gson = gb.create();
        String json = gson.toJson(obj);
        if(numberOfCyclicReferencesThread != null)
            numberOfCyclicReferencesThread.set(classRetrievalAdapterFactory.numberOfCyclicReferencesThread.get());
        Set<String> filteredUniqueClassSet = new HashSet<>();
        for(String cn : uniqueClassSet){
            try {
                Class<?> c = Class.forName(cn);
                if (!Collection.class.isAssignableFrom(c) && !Map.class.isAssignableFrom(c)){
                    filteredUniqueClassSet.add(cn);
                }
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        }
        return filteredUniqueClassSet;
    }

}
