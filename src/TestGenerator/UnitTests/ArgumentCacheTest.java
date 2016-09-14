package TestGenerator.UnitTests;

import TestGenerator.ArgumentCache.ArgumentCacheStream;
import TestGenerator.ArgumentCache.ArgumentObjectInfo;
import TestGenerator.ArgumentCache.IArgumentCache;
import TestGenerator.ArgumentCache.RandomizedArgumentMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by NatchaS on 3/7/16.
 */
public class ArgumentCacheTest {
    public static void main(String[] args) throws Exception{
//        Class.forName("DataStructures.MyInteger");
////        RandomizedArgumentMap cache = new RandomizedArgumentMap();
////        cache.append();
////        ArgumentCacheStream stream = new ArgumentCacheStream("./input_generator/StackAr/PonzuDaikonOutput/DataStructures_StackArObjectMap.ser");
//
//        ArgumentCacheStream stream = new ArgumentCacheStream("./input_generator/StackAr/DataStructures_StackArObjectMap.ser");
//        IArgumentCache cache = stream.readObject();

        Class.forName("java.lang.Comparable");
        String fname = "./input_generator/ARGCACHE_PKG_TEST.ser";
        ArgumentCacheStream stream = new ArgumentCacheStream(fname);
        RandomizedArgumentMap cacheMap = new RandomizedArgumentMap();

        Object[] arg1 = new Object[]{1,2,3};
        Object[] arg2 = new Object[]{12,22,23};

        List<ArgumentObjectInfo> testObj1 = Arrays.asList(arg1).stream().map(x->new ArgumentObjectInfo(x, Object.class)).collect(Collectors.toCollection(ArrayList<ArgumentObjectInfo>::new));;
        List<ArgumentObjectInfo> testObj2 = Arrays.asList(arg2).stream().map(x->new ArgumentObjectInfo(x, Object.class)).collect(Collectors.toCollection(ArrayList<ArgumentObjectInfo>::new));;
        List<ArgumentObjectInfo> testObj3 = Arrays.asList(new ArgumentObjectInfo[]{new ArgumentObjectInfo("s", Object.class),new ArgumentObjectInfo(1.0, Object.class), new ArgumentObjectInfo(new int[][]{{1},{2}}, Object.class)});


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


    }
}
