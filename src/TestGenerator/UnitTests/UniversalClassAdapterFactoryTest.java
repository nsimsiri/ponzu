package TestGenerator.UnitTests;

import TestGenerator.ArgumentCache.ClassRetrievalAdapterFactory;
import TestGenerator.ArgumentCache.UniversalClassAdapterFactory;
import TestGenerator.ArgumentCache.UniversalTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by NatchaS on 10/11/16.
 */
public class UniversalClassAdapterFactoryTest {
    public static void test1(){
        GsonBuilder gb = new GsonBuilder();
        Gson gson = gb.registerTypeAdapterFactory(new UniversalClassAdapterFactory()).create();
        String json = "";

        Class<Integer> c1 = Integer.class;
        Class<Comparable> c2 = Comparable.class;
        Class<List> c3 = List.class;
        Class<Number> c4 = Number.class;

        json = gson.toJson(c1);
        assert(c1.equals(gson.fromJson(json, c1.getClass())));
        json = gson.toJson(c2);
        assert(c2.equals(gson.fromJson(json, c2.getClass())));
        json = gson.toJson(c3);
        assert(c3.equals(gson.fromJson(json, c3.getClass())));
        json = gson.toJson(c4);
        assert(c4.equals(gson.fromJson(json, c4.getClass())));
    }

    public static void test2(){
        List<Class> l = new ArrayList<Class>(Arrays.asList(new Class[]{Integer.class, Integer.class, Number.class}));
        Set s = ClassRetrievalAdapterFactory.getUniqueClassname(l);
        System.out.println(s);
    }

    public static void testWithUTAF() throws ClassNotFoundException{
        List<Class> l = new ArrayList<Class>(Arrays.asList(new Class[]{Integer.class, Integer.class, Number.class}));
        Gson gson = UniversalTypeAdapterFactory.buildGson(l);
        String json = gson.toJson(l);

        List<Class> _l = UniversalTypeAdapterFactory.deserialize(json, l.getClass(), gson);
        assert(_l.toString().equals(l.toString()));
    }

    public static void main(String[] args) throws Exception {
        test1();
        test2();
        testWithUTAF();
    }
}
