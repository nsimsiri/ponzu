package TestGenerator.ArgumentCache;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Created by NatchaS on 10/11/16.
 */
public class UniversalClassAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken){
        if (Class.class.isAssignableFrom(typeToken.getRawType())){
            return (TypeAdapter<T>)new UniversalClassAdapter();
        }
        return null;
    }

    public static class UniversalClassAdapter extends TypeAdapter<Class<?>>{
        @Override
        public void write(JsonWriter out, Class<?> value) throws IOException{
            if (value == null){
                out.nullValue();
                return;
            }
            out.value(value.getName());
        }

        @Override
        public Class<?> read(JsonReader in) throws IOException {
            if (in.peek()== JsonToken.NULL){
                in.nextNull();
                return null;
            }
            try {
                Class<?> c = Class.forName(in.nextString());
                return c;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static void main(String[] args){
        Class<?> i = Integer.class;
        System.out.println(i.getName());
    }
}
