package TestGenerator.ArgumentCache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by NatchaS on 3/16/16.
 * Wrapper over Argument Object
 * - wraps Gson serialization/deserialization process
 * [COMPLETED] does not handle Generic Collection types.
 * TODO: does not handle cyclic references.
 */
public class ArgumentObjectInfo implements Serializable {
    transient private static final long serializedVersionUID = 1L;
    transient private Object object_;
    transient private Class class_;
    transient private Class paramType;
    public String serializedObject;
    public String class_string;
    public String paramType_string;
    public Set<String> tracked_classnames;

    public ArgumentObjectInfo(){}
    public ArgumentObjectInfo(Object object_, Class class_, Class paramType, String serializedObject) {
        this.object_ = object_;
        this.class_ = class_;
        this.paramType = paramType;
        this.serializedObject = serializedObject;
    }
    public ArgumentObjectInfo(Object obj, Class paramType){
        this.object_ = obj;
        this.class_ = null;
        if (obj != null){
            this.class_ = obj.getClass();
        }
        if (paramType == null){
            throw new IllegalArgumentException("[PONZU Exception] Parameter type cannot be null.");
        }
        this.paramType = paramType;
    }
    private void writeObject(ObjectOutputStream out) throws IOException {
        /*
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new UniversalTypeAdapterFactory());
        Gson gson = gsonBuilder
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .create();
        */

        this.serializedObject = "";
        this.tracked_classnames = new HashSet<>();

        if (this.object_ != null){
            Gson gson = UniversalTypeAdapterFactory.buildGson(this.object_, this.tracked_classnames);
            this.serializedObject = gson.toJson(this.object_);
        }

        this.class_string = this.class_ == null ? "null" : this.class_.getName() ;
        this.paramType_string = this.paramType == null ? "null" : this.paramType.getName();

        out.writeObject(this.tracked_classnames);
        out.writeObject(this.serializedObject);
        out.writeObject(this.class_string);
        out.writeObject(this.paramType_string);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
        this.tracked_classnames = (HashSet<String>) in.readObject();
        this.serializedObject = (String)in.readObject();
        this.class_string = (String)in.readObject();
        this.paramType_string = (String)in.readObject();
        this.paramType = this.paramType_string.equals("null") ?  null : Class.forName(this.paramType_string);
        this.class_ = this.class_string.equals("null") ? null : Class.forName(this.class_string);
        this.object_ = null;

        try {
            /*
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapterFactory(new UniversalTypeAdapterFactory());
            Gson gson = gsonBuilder
                    .serializeNulls()
                    .enableComplexMapKeySerialization()
                    .serializeSpecialFloatingPointValues()
                    .create();
                    */
            Gson gson = UniversalTypeAdapterFactory.buildGson();
            if (this.class_!=null){
                this.object_ = UniversalTypeAdapterFactory.deserialize(serializedObject, this.class_, gson);
            }
        } catch (RuntimeException e){
            System.err.format("[PONZU Exception]: ! unable to deserialize object of type [%s] for signature [%s] for the following object:\n" +
                    "--------\n %s\n--------\n", this.class_, this.paramType, serializedObject);
            e.printStackTrace();
        }
    }

    private static void __examine_obj__ (Object o){
        // cyclic references will cause program to not terminate
        /*Queue<Object> q = new LinkedList<>();
        q.add(o);
        while(!q.isEmpty()){
            Object node = q.poll();
            System.out.format("%s (Type: %s)\n", node, (node!=null) ? node.getClass() : null);
            if (node!=null){
                for(Field f : node.getClass().getDeclaredFields()){
                    try {
                        f.setAccessible(true);
                        q.add(f.get(node));

                    } catch (IllegalAccessException iae){
                        System.out.format("ERROR! " + iae.getMessage());
                    }
                }
            }
        }*/
        if (o!=null && o.getClass().getName().equals("org.jfree.data.category.DefaultCategoryDataset")){
            System.out.println("\n$$$ START Ex. Obj $$$\n");
            Class o_c = o.getClass();
            try {


                Field data_f = o_c.getDeclaredField("data");
//                Field group_f = o_c.getDeclaredField("group");
                data_f.setAccessible(true);
//                group_f.setAccessible(true);
                Object data_o = data_f.get(o);
//                Object group_o = group_f.get(o);

                if(data_o == null) {
                    System.out.println("Field data = null");
                    return;
                }
                if(data_o == null) {
                    System.out.println("Field data = null");
                    return;
                }
//                if (group_o == null){
//                    System.out.println("Field group = null");
//                    return;
//                }

                Field rowKeys_f = data_o.getClass().getDeclaredField("rowKeys");
                Field colKeys_f = data_o.getClass().getDeclaredField("columnKeys");
                Field rows_f = data_o.getClass().getDeclaredField("rows");
                rowKeys_f.setAccessible(true);
                colKeys_f.setAccessible(true);
                rows_f.setAccessible(true);
                Object o1 = rowKeys_f.get(data_o);
                Object o2 = colKeys_f.get(data_o);
                Object o3 = rows_f.get(data_o);
                String x = "";
                x+=String.format("%s (Type=%s)\n", o1, (o1 == null) ? null : o1.getClass());
                x+=String.format("%s (Type=%s)\n", o2, (o2 == null) ? null : o2.getClass());
                x+=String.format("%s (Type=%s)\n", o3, (o3 == null) ? null : o3.getClass());
                System.out.println(x);

            } catch (NoSuchFieldException nsfe){
                nsfe.printStackTrace();
            } catch (IllegalAccessException iae){
                iae.printStackTrace();
            } finally {
                System.out.println("\n$$$ END Ex. Obj $$$\n");
            }
        }
    }


    private void readObjectNoData() throws ObjectStreamException {
        throw new NotImplementedException();
    }

    @Override
    public boolean equals(Object obj){
        ArgumentObjectInfo aoi = (ArgumentObjectInfo)obj;
        return aoi.class_==this.class_ && aoi.paramType==this.paramType && aoi.object_==this.object_;
    }

    @Override
    public int hashCode(){

        int result = 1;
        int clsHashCode = 0;
        if (this.object_ != null){
            result = this.object_.hashCode();
            clsHashCode = this.class_.hashCode();
        }
        result = 37*result + this.class_.hashCode();
        result = 37*result + this.paramType.hashCode();
        return result;
    }

    @Override
    public String toString(){
        String obj = null;
        String cls = null;
        if (this.object_!=null) {
            obj = this.object_.toString();
            cls = this.class_.getSimpleName();
        }
        return String.format("(%s,%s)", cls, obj);
    }

    public Object getObject_() {
        return object_;
    }

    public void setObject_(Object object_) {
        this.object_ = object_;
    }

    public Class getClass_() {
        return class_;
    }

    public void setClass_(Class class_) {
        this.class_ = class_;
    }

    public Class getParamType() {
        return paramType;
    }

    public void setParamType(Class paramType) {
        this.paramType = paramType;
    }

    public String getSerializedObject() {
        return serializedObject;
    }

    public void setSerializedObject(String serializedObject) {
        this.serializedObject = serializedObject;
    }

    public String getClass_string() {
        return class_string;
    }

    public void setClass_string(String class_string) {
        this.class_string = class_string;
    }

    public String getParamType_string() {
        return paramType_string;
    }

    public void setParamType_string(String paramType_string) {
        this.paramType_string = paramType_string;
    }
}
