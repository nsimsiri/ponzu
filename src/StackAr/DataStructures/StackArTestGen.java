package StackAr.DataStructures;
import TestGenerator.ITestInstance;
import TestGenerator.TestInstanceException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by NatchaS on 11/19/15.
 */
public class StackArTestGen {
    public class StackArTest implements ITestInstance{
        StackAr stack;
        public StackArTest(){
            this.stack=null;
        }
        public Object invokeMethod(String methodName, ArrayList<Object> args) {
            System.out.println("invoking " + methodName);
            if(methodName.equals("StackAr")){
                if (args.size()==1){
                    if (!(args.get(0) instanceof Integer)){
                        System.out.println("initial capacity non-integer object");
                        System.exit(1);
                    }
                    this.stack=new StackAr((Integer)args.get(0));
                } else {
                    this.stack=new StackAr();
                }
            } else if (methodName.equals("push")){
                if (args.size()!=1) {
                    System.out.println("bad argument list");
                    System.exit(1);
                }
                try {
                    this.stack.push(args.get(0));
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else if (methodName.equals("pop")){
                try {
                    this.stack.pop();
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else if (methodName.equals("topAndPop")){
                try {
                    return this.stack.topAndPop();
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else if (methodName.equals("makeEmpty")){
                this.stack.makeEmpty();
            } else if (methodName.equals("isFull")){
                return new Boolean(this.stack.isFull());
            } else if (methodName.equals("isEmpty")){
                return new Boolean(this.stack.isEmpty());
            } else if (methodName.equals("top")){
                return stack.top();
            } else System.out.format("method [%s] not found\n", methodName);
            return null;
        }

        public Object getTestInstance(){
            return this.stack;
        }
        public Class<?> getRuntimeClass(){
            return StackAr.class;
        }
    }
    public static void main(String[] args){
        //test StackAr serialized map
        try {
            InputStream file = new FileInputStream("StackArObjectMap.ser");
            InputStream buf = new BufferedInputStream(file);
            ObjectInput in = new ObjectInputStream(buf);
            HashMap<String, ArrayList<Object>> objMap = (HashMap<String, ArrayList<Object>>)in.readObject();
            System.out.println(objMap.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }


}
