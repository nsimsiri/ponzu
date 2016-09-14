package TestGenerator.ArgumentCache;

/**
 * Created by NatchaS on 1/19/16.
 */
public class ArgumentCacheShutdownHook {
    ArgumentCacheStream out;
    IArgumentCache cache;
    boolean verbose = true;
    public ArgumentCacheShutdownHook(ArgumentCacheStream stream, IArgumentCache cache){
        this.out = stream;
        this.cache = cache;
    }
    public void attachShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run(){
                out.writeObject(cache);
                if (verbose) System.out.println("[PONZU] Writing arguments to cache: " + cache.toString());
            }
        });
    }
}
