package TestGenerator.ArgumentCache;

import java.io.*;

/**
 * Created by NatchaS on 1/18/16.
 */
public class ArgumentCacheStream {
    String filename;
    public ArgumentCacheStream(String filename){
        this.filename = filename;
    }
    public ArgumentCacheStream(String filename, IArgumentCache argCache){
        this.filename = filename;
        this.writeObjectOnShutdown(argCache);
    }

    public void writeObject(IArgumentCache argCache){
        try {
            OutputStream file = new FileOutputStream(this.filename);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput out = new ObjectOutputStream(buffer);
            out.writeObject(argCache);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDefaultNaming(String name){
        name = name.replace(".","_");
        return String.format("%sObjectMap.ser",name);
    }

    public IArgumentCache readObject(){
        try {
            InputStream file = new FileInputStream(this.filename);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput inp = new ObjectInputStream(buffer);
            IArgumentCache iac = (IArgumentCache)inp.readObject();
            inp.close();
            return iac;

        } catch (IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }

    public void writeObjectOnShutdown(IArgumentCache argCache){
        ArgumentCacheShutdownHook hook = new ArgumentCacheShutdownHook(this, argCache);
        hook.attachShutdownHook();
    }

    public static ArgumentCacheStream onShutdownCacheStream(String filename, IArgumentCache argCache){
        ArgumentCacheStream stream = new ArgumentCacheStream(filename);
        stream.writeObjectOnShutdown(argCache);
        return stream;
    }

    public void setFilename(String name){
        this.filename = name;
    }
}
