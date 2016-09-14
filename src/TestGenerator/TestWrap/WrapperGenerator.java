package TestGenerator.TestWrap;

import DataTypes.MTS;
import TestGenerator.ArgumentCache.ArgumentCacheStream;
import TestGenerator.ArgumentCache.IArgumentCache;
import TestGenerator.ArgumentCache.RandomizedArgumentMap;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.sun.org.apache.bcel.internal.generic.ClassGen;

/**
 * Created by NatchaS on 1/27/16.
 *
 * Generates a shim/wrapper over the user's library or SUT.
 */
public class WrapperGenerator {
    private String originalClassName;
    private String className;
    private String packageName;
    private JCodeModel codeModel;
    private JDefinedClass wrapperClass;
    private Class SUTClass;
    JVar cacheMapVar;
    private String cacheMapName;
    private boolean useOriginalConstructorName;

    private HashMap<String, JPrimitiveType> primitiveTypeMap;

    public WrapperGenerator(String packageName, String originalClassName){
        this.originalClassName = originalClassName;
        this.packageName = packageName;
        this.className = String.format("%s_ModbatWrapper", originalClassName);
        this.codeModel = new JCodeModel();
        String wrapperName = originalClassName;
        this.useOriginalConstructorName = false;
        instantiatePrimitiveTypeMap();
        if (packageName.length()!=0) wrapperName = String.format("%s.%s", packageName, this.className);

        try {
            this.wrapperClass = this.codeModel._class(JMod.PUBLIC, wrapperName, ClassType.CLASS);
            this.SUTClass = Class.forName(String.format("%s.%s", packageName, originalClassName));
            JClass superTypeSUT = (JClass)this.codeModel._ref(SUTClass);
            this.wrapperClass._extends(superTypeSUT);
            this.declareInstanceVars();
        } catch (JClassAlreadyExistsException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public WrapperGenerator(String originalClassName){
        this("", originalClassName);
    }

    private void instantiatePrimitiveTypeMap(){
        this.primitiveTypeMap = new HashMap<String, JPrimitiveType>();
        this.primitiveTypeMap.put("int", this.codeModel.INT);
        this.primitiveTypeMap.put("long", this.codeModel.LONG);
        this.primitiveTypeMap.put("short", this.codeModel.SHORT);
        this.primitiveTypeMap.put("double", this.codeModel.DOUBLE);
        this.primitiveTypeMap.put("float", this.codeModel.FLOAT);
        this.primitiveTypeMap.put("byte", this.codeModel.BYTE);
        this.primitiveTypeMap.put("char", this.codeModel.CHAR);
        this.primitiveTypeMap.put("boolean", this.codeModel.BOOLEAN);

    }


    private boolean isPrimitive(String type){
        return this.primitiveTypeMap.containsKey(type);
    }
    private JType getJType(Class<?> someClass){
        JType jType;
        if (this.primitiveTypeMap.containsKey(someClass.toString())){
            jType = this.primitiveTypeMap.get(someClass.toString());
        } else {
            jType = this.codeModel.ref(someClass);
        }
        return jType;
    }

    public Class getBoxedType(Class<?> someClass){
        if (this.codeModel.primitiveToBox.containsKey(someClass)){
            return this.codeModel.primitiveToBox.get(someClass);
        }
        return someClass;
    }
    public String getSerializedFileName(){
        return String.format("%sObjectMap.ser", this.originalClassName);
    }

    private void declareInstanceVars(){
        this.cacheMapName = "cacheMap";
        String cacheStreamName = "cacheStream";
        this.cacheMapVar = this.wrapperClass.field(
                JMod.PRIVATE,
                IArgumentCache.class,
                this.cacheMapName,
                JExpr._new(this.codeModel.ref(RandomizedArgumentMap.class))
        );
        this.wrapperClass.field(
                JMod.PRIVATE,
                ArgumentCacheStream.class,
                cacheStreamName,
                this.codeModel.ref(ArgumentCacheStream.class).staticInvoke("onShutdownCacheStream")
                        .arg(getSerializedFileName())
                        .arg(this.cacheMapVar)
        );

    }

    private void instantiateCachingMechanism(JBlock instanceMethodBlock){
        if (this.cacheMapVar == null) throw new NullPointerException();
        instanceMethodBlock.assign(this.cacheMapVar, JExpr._new(this.codeModel.ref(RandomizedArgumentMap.class)));
        String cacheStreamName = "cacheStream";
        JVar cacheStreamVar = instanceMethodBlock.decl(
                this.codeModel.ref(ArgumentCacheStream.class),
                cacheStreamName,
                JExpr._new(this.codeModel.ref(ArgumentCacheStream.class)).arg(getSerializedFileName())
        );
        instanceMethodBlock.invoke(cacheStreamVar, "writeObjectOnShutdown").arg(this.cacheMapVar);
    }


    private void createConstructor(Constructor constructor){
        Class<?>[] paramSignatures = constructor.getParameterTypes();
        if (paramSignatures.length > 0) try {
            {
                JMethod jConstructor = this.wrapperClass.constructor(JMod.PUBLIC);
                JInvocation consSuperInvoc = jConstructor.body().invoke("super");
                for(int i = 0; i < paramSignatures.length; i++){
                    Class<?> paramSig = paramSignatures[i];
                    JType paramType = getJType(paramSig);
                    JVar param_i = jConstructor.param(paramType, String.format("p%s", i));
                    consSuperInvoc = consSuperInvoc.arg(param_i);
                }


                JVar argToTypePairs = jConstructor.body().decl(
                        this.codeModel.ref(LinkedHashMap.class),
                        "args",
                        JExpr._new(this.codeModel.ref(LinkedHashMap.class))
                );

                for(int i = 0; i < paramSignatures.length; i++){
                    jConstructor.body().invoke(argToTypePairs, "put")
                            .arg(JExpr.ref(String.format("p%s", i)))
                            .arg(JExpr.direct(String.format("%s.class", getBoxedType(paramSignatures[i]).getName()))
                    );
                }

                String daikonConstructorName = this.className;
                if (useOriginalConstructorName) daikonConstructorName = this.originalClassName;

                jConstructor.body().invoke(this.cacheMapVar, "append")
                        .arg(daikonConstructorName)
                        .arg(argToTypePairs);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createInstanceMethod(Method method){
        Class<?> returnTypeSignature = method.getReturnType();
        Class<?>[] paramSignatures = method.getParameterTypes();
        Class<?>[] exceptionSignatures = method.getExceptionTypes();
        JType returnTypeBoxed = this.codeModel.VOID;
        if (paramSignatures.length > 0) {
            if (!returnTypeSignature.equals(void.class)) returnTypeBoxed = getJType(returnTypeSignature);
            JMethod jMethod = this.wrapperClass.method(JMod.PUBLIC, returnTypeBoxed, method.getName());
            for (int i = 0; i < exceptionSignatures.length; i++) {
                jMethod._throws(this.codeModel.ref(exceptionSignatures[i]));
            }
            JVar argToTypePairs = jMethod.body().decl(
                    this.codeModel.ref(LinkedHashMap.class),
                    "args",
                    JExpr._new(this.codeModel.ref(LinkedHashMap.class))
            );

            JInvocation superInvoc = JExpr._super().invoke(method.getName());
            for (int i = 0; i < paramSignatures.length; i++) {
                String param_i = String.format("p%s", i);
                JType jType = getJType(paramSignatures[i]);
                JVar paramVar_i = jMethod.param(jType, param_i);
                jMethod.body().invoke(argToTypePairs, "put")
                        .arg(paramVar_i)
                        .arg(JExpr.direct(String.format("%s.class", getBoxedType(paramSignatures[i]).getName())));
                superInvoc = superInvoc.arg(paramVar_i);
            }
            jMethod.body().invoke(this.cacheMapVar, "append")
                    .arg(method.getName())
                    .arg(argToTypePairs);
            if (!returnTypeBoxed.equals(this.codeModel.VOID)) jMethod.body()._return(superInvoc);
            else jMethod.body().add(superInvoc);
        }
    }

    public void setUseOriginalConstructorName(boolean b){
        useOriginalConstructorName = b;
    }

    public void outputWrapper(String filename){
        Constructor<?>[] constructors = this.SUTClass.getConstructors();
        for(int i = 0; i < constructors.length; i++){
            createConstructor(constructors[i]);
        }

        Method[] methods = this.SUTClass.getDeclaredMethods();
        for(int i = 0; i < methods.length; i++){
            int modifier = methods[i].getModifiers();
            if (!Modifier.isStatic(modifier) && Modifier.isPublic(modifier)){
                createInstanceMethod(methods[i]);
            }
        }

        try {
            this.codeModel.build(new File(filename));
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void useOriginalConstructorName(boolean b){
        this.useOriginalConstructorName = b;
    }

    public static void main(String[] args){
        WrapperGenerator wg = new WrapperGenerator("StackAr.DataStructures", "StackAr");
        wg.setUseOriginalConstructorName(true); // argument cached original constructor name
        wg.outputWrapper("src/");
    }

}
