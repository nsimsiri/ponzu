package TestGenerator.UnitTests;

import TestGenerator.ArgumentCache.ArgumentCacheStream;
import TestGenerator.ArgumentCache.ArgumentObjectInfo;
import TestGenerator.ArgumentCache.IArgumentCache;
import TestGenerator.ArgumentCache.MethodSignaturesPair;
import TestGenerator.ArgumentCache.RandomizedArgumentMap;
import com.sun.javafx.tools.ant.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by NatchaS on 3/7/16.
 */
public class ArgumentCacheTest {
    public static void test1(){
        RandomizedArgumentMap cacheMap = new RandomizedArgumentMap();

        Object[] arg1 = new Object[]{1,2,3};
        Object[] arg2 = new Object[]{12,22,23};
        Object[] arg3 = new Object[]{"s", 1.0, new int[][]{{1},{2}}};
        List<ArgumentObjectInfo> testObj1 = Arrays.asList(arg1).stream().map(x->new ArgumentObjectInfo(x, Object.class)).collect(Collectors.toCollection(ArrayList<ArgumentObjectInfo>::new));;
        List<ArgumentObjectInfo> testObj2 = Arrays.asList(arg2).stream().map(x->new ArgumentObjectInfo(x, Object.class)).collect(Collectors.toCollection(ArrayList<ArgumentObjectInfo>::new));;
        List<ArgumentObjectInfo> testObj3 = Arrays.asList(new ArgumentObjectInfo[]{new ArgumentObjectInfo(arg3[0], Object.class),new ArgumentObjectInfo(arg3[1], Object.class), new ArgumentObjectInfo(arg3[2], Object.class)});

        cacheMap.append("push", testObj1);
        cacheMap.append("push", testObj2);
        cacheMap.append("push", testObj3);
        List<Object> randArgs1 = cacheMap.get("push", Arrays.asList(new String[]{Object.class.getName(), Object.class.getName(), Object.class.getName()}));
        List<Object> randArgs2 = cacheMap.get("push", Arrays.asList(new String[]{Object.class.getName(), Object.class.getName(), Object.class.getName()}));
        List<Object> randArgs3 = cacheMap.get("push", Arrays.asList(new String[]{Object.class.getName(), Object.class.getName(), Object.class.getName()}));
        System.out.println(randArgs2);
        System.out.println(randArgs1);
        System.out.println(randArgs3);

        Set<Object> s1 = new HashSet<>(Arrays.asList(new Object[]{arg1[0], arg2[0], arg3[0]}));
        Set<Object> s2 = new HashSet<>(Arrays.asList(new Object[]{arg1[1], arg2[1], arg3[1]}));
        Set<Object> s3 = new HashSet<>(Arrays.asList(new Object[]{arg1[2], arg2[2], arg3[2]}));

        assert(s1.contains(randArgs1.get(0)) && s2.contains(randArgs1.get(1)) && s3.contains(randArgs1.get(2)));
        assert(s1.contains(randArgs2.get(0)) && s2.contains(randArgs2.get(1)) && s3.contains(randArgs2.get(2)));
        assert(s1.contains(randArgs3.get(0)) && s2.contains(randArgs3.get(1)) && s3.contains(randArgs3.get(2)));
        System.out.println("completed test 1: get with default filter");
    }

    public static void test2(){
        RandomizedArgumentMap cacheMap = new RandomizedArgumentMap();
        List<String> types = Arrays.asList(new String[]{Integer.class.getName(), Double.class.getName(), String.class.getName()});
        Object[] arg1 = new Object[]{10,20.0,"a"};
        Object[] arg2 = new Object[]{-10,100.0,"b"};
        Object[] arg3 = new Object[]{20,60.0,"b"};
        Object[] arg4 = new Object[]{40,10.0,"a"};
        Function<Object[], List<ArgumentObjectInfo>> m = (Object[] A) -> {
            ArgumentObjectInfo a = new ArgumentObjectInfo(A[0], Integer.class);
            ArgumentObjectInfo b = new ArgumentObjectInfo(A[1], Double.class);
            ArgumentObjectInfo c = new ArgumentObjectInfo(A[2], String.class);
            return Arrays.asList(new ArgumentObjectInfo[]{a,b,c});
        };

        List<ArgumentObjectInfo> testObj1 = m.apply(arg1);
        List<ArgumentObjectInfo> testObj2 = m.apply(arg2);
        List<ArgumentObjectInfo> testObj3 = m.apply(arg3);
        List<ArgumentObjectInfo> testObj4 = m.apply(arg4);


        cacheMap.append("hello", testObj1);
        cacheMap.append("hello", testObj2);
        cacheMap.append("hello", testObj3);
        cacheMap.append("hello", testObj4);

        Function<ArgumentObjectInfo, Boolean> f1 = (ArgumentObjectInfo o) ->{
            Integer x = (Integer) o.getObject_();
            return x >= 0;
        };

        Function<ArgumentObjectInfo, Boolean> f2 = (ArgumentObjectInfo o) -> {
            Double y = (Double)o.getObject_();
            return y >= 50;
        };

        Function<ArgumentObjectInfo, Boolean> f3 = (ArgumentObjectInfo o) -> {
            String z = (String) o.getObject_();
            return z.equals("a");
        };

        List<Function<ArgumentObjectInfo, Boolean>> filters = Arrays.asList(new Function[]{f1,f2,f3});
        List<Object> argSet1 = cacheMap.get("hello", types, filters);
        List<Object> argSet2 = cacheMap.get("hello", types, filters);
        List<Object> argSet3 = cacheMap.get("hello", types, filters);
        System.out.println("Should have: arg1 >=0 , arg2 >= 50, arg3 == a");
        System.out.println("\t" + argSet1);
        System.out.println("\t" + argSet2);
        System.out.println("\t" + argSet3);
        System.out.println("completed test 2: applied filters");
    }

    public static void main(String[] args) throws Exception{
        test1();
        test2();
        /*
        Class.forName("java.lang.Comparable");
        String fname = "./input_generator/ARGCACHE_PKG_TEST.ser";
        ArgumentCacheStream stream = new ArgumentCacheStream(fname);


        List<ArgumentObjectInfo> what_arg = Arrays.asList(new ArgumentObjectInfo[]{new ArgumentObjectInfo(null, String.class)});
        List<ArgumentObjectInfo> what_arg2 = Arrays.asList(new ArgumentObjectInfo[]{new ArgumentObjectInfo("yay", String.class)});

        cacheMap.append("push", testObj1);
        cacheMap.append("push", testObj2);
        cacheMap.append("push", testObj3);
        cacheMap.append("pop", testObj1);
        cacheMap.append("what", what_arg);
        cacheMap.append("what", what_arg2);
        System.out.println(cacheMap.keySet());
        System.out.println(cacheMap);
        stream.writeObject(cacheMap);

        System.out.println("DESERIALIZING......\n");
        IArgumentCache de_cache = stream.readObject();
        System.out.println(de_cache);
        */


    }
}
