package TestGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by NatchaS on 10/22/16.
 *
 * Utility methods to aid ModbatModelGenerator
 * TODO: refactor other "utility functions" here.
 */

public class ModbatModelUtilities {

    public static Map<String, Class> primitiveToClass = new HashMap<>();
    static {
        primitiveToClass.put("int",int.class);
        primitiveToClass.put("double", double.class);
        primitiveToClass.put("byte", byte.class);
        primitiveToClass.put("float", float.class);
        primitiveToClass.put("short", short.class);
        primitiveToClass.put("long", long.class);
        primitiveToClass.put("boolean", boolean.class);
        primitiveToClass.put("char", char.class);
    }

    public static Object executeMethod(Object o, String methodName, List<String> signatures, Object... args){
        Class clz = o.getClass();
        Class[] signatureClasses = new Class[signatures.size()];
        try {
            for(int i =0; i < signatures.size(); i++){
                signatureClasses[i] = Class.forName(signatures.get(i));
            }
            Method method = clz.getDeclaredMethod(methodName, signatureClasses);
            method.setAccessible(true);
            return method.invoke(o, args);
        } catch(ClassNotFoundException e){
            e.printStackTrace();
        } catch (NoSuchMethodException e){
            e.printStackTrace();
        } catch (IllegalAccessException e){
            e.printStackTrace();
        } catch (InvocationTargetException e){
            System.err.println("TargetException:");
            e.getTargetException().printStackTrace();
        }
        return null;
    }


}
