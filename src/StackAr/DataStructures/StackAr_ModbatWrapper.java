
package StackAr.DataStructures;

import java.util.LinkedHashMap;
import TestGenerator.ArgumentCache.ArgumentCacheStream;
import TestGenerator.ArgumentCache.IArgumentCache;
import TestGenerator.ArgumentCache.RandomizedArgumentMap;

/*
* GENERATED JAVA SUT WRAPPER - GENERATION IS DEPRECATED, METHOD INSTRUMENTATION IS DONE THROUGH CUSTOMIZED CHICORY.
*
* */
public class StackAr_ModbatWrapper
    extends StackAr
{

    private IArgumentCache cacheMap = new RandomizedArgumentMap();
    private ArgumentCacheStream cacheStream = ArgumentCacheStream.onShutdownCacheStream("StackArObjectMap.ser", cacheMap); //need to include filepath

    public StackAr_ModbatWrapper(int p0) {
        super(p0);
        LinkedHashMap args = new LinkedHashMap();
        args.put(p0, (java.lang.Integer.class));
//        cacheMap.append("StackAr", args); // uncommented for new version of ArgumentCache
    }

    public void push(Object p0)
        throws Overflow
    {
        LinkedHashMap args = new LinkedHashMap();
        args.put(p0, (java.lang.Object.class));
//        cacheMap.append("push", args);
        super.push(p0);
    }




}
