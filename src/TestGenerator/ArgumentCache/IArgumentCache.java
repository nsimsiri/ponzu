package TestGenerator.ArgumentCache;

import java.util.List;
import java.util.Set;

/**
 * Created by NatchaS on 1/18/16.
 * TODO: Propagate interface change, may have to change every argument tuple to a LinkedHashMap<Cached_Obj, class>
 */
public interface IArgumentCache {

    public void append(String method, List<ArgumentObjectInfo> objInfoList);
    public List<Object> get(String method, List<String> argumentTypes);
    public boolean contains(String method, List<String> argumentTypes);
    public Set<MethodSignaturesPair> keySet();
}
