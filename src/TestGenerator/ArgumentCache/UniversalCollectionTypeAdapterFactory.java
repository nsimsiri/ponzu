package TestGenerator.ArgumentCache;

/**
 * Created by NatchaS on 10/8/16.
 */
/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Adapt a homogeneous collection of objects.
 */
public final class UniversalCollectionTypeAdapterFactory implements TypeAdapterFactory {
    private ConstructorConstructor constructorConstructor;

    public UniversalCollectionTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
        this.constructorConstructor = constructorConstructor;
    }

    public UniversalCollectionTypeAdapterFactory(){}

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Type type = typeToken.getType();

        /// hack to obtain hidden constructorConstructor from gson - guarantees that gson has already initialized
        // and the original CollectionTypeAdapterFactory has been assigned the same constructorConstructor.
        if (constructorConstructor == null){
            try {
                Field conf = gson.getClass().getDeclaredField("constructorConstructor");
                conf.setAccessible(true);
                constructorConstructor =  (ConstructorConstructor) conf.get(gson);
            } catch (NoSuchFieldException e){
                e.printStackTrace();
            } catch (IllegalAccessException e){
                e.printStackTrace();
            }

        }
        Class<? super T> rawType = typeToken.getRawType();
        if (!Collection.class.isAssignableFrom(rawType)) {
            return null;
        }

        Type elementType = $Gson$Types.getCollectionElementType(type, rawType);
        TypeAdapter<?> elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));
        ObjectConstructor<T> constructor = constructorConstructor.get(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"}) // create() doesn't define a type parameter
                TypeAdapter<T> result = new Adapter(gson, elementType, elementTypeAdapter, constructor);
        return result;
    }

    private static final class Adapter<E> extends TypeAdapter<Collection<E>> {
        private TypeAdapter<E> elementTypeAdapter;
        private final ObjectConstructor<? extends Collection<E>> constructor;
        private TypeAdapter<JsonElement> jsonElementTypeAdapter;
        private final Gson gson;

        public Adapter(Gson context, Type elementType,
                       TypeAdapter<E> elementTypeAdapter,
                       ObjectConstructor<? extends Collection<E>> constructor) {

            this.elementTypeAdapter =
                    new UniversalTypeAdapterRuntimeTypeWrapper<E>(context, elementTypeAdapter, elementType);
            this.constructor = constructor;
            this.jsonElementTypeAdapter = context.getAdapter(JsonElement.class);
            this.gson = context;
        }

        @Override public void write(JsonWriter out, Collection<E> collection) throws IOException {
            out.setLenient(true);
            if (collection == null) {
                out.nullValue();
                return;
            }
            JsonObject jsonArrayWrapper = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            JsonArray jsonArrayType = new JsonArray();
            for (E element : collection) {
                JsonElement jsonElement = elementTypeAdapter.toJsonTree(element);
                jsonArray.add(jsonElement);
                jsonArrayType.add(jsonElement.isJsonNull() ? null : element.getClass().getName());
            }
            jsonArrayWrapper.add(UniversalTypeAdapterFactory.array_token, jsonArray);
            jsonArrayWrapper.add(UniversalTypeAdapterFactory.array_type, jsonArrayType);
            jsonArrayWrapper.add(UniversalTypeAdapterFactory.class_token, new JsonPrimitive(collection.getClass().getName()));
            jsonElementTypeAdapter.write(out, jsonArrayWrapper);

            /*
            // ORIGINAL CODE
            out.beginArray();
            for (E element : collection) {
                elementTypeAdapter.write(out, element);
            }
            out.endArray();
            */
        }

        @Override public Collection<E> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            JsonElement jsonElement = jsonElementTypeAdapter.read(in);
            Collection<E> collection = constructor.construct();
            if (jsonElement.isJsonObject()){
                JsonObject collectionWrapper = jsonElement.getAsJsonObject();
                JsonArray elementArray = collectionWrapper.get(UniversalTypeAdapterFactory.array_token).getAsJsonArray();
                JsonArray elementArrayType = collectionWrapper.get(UniversalTypeAdapterFactory.array_type).getAsJsonArray();

                for(int i =0 ; i < elementArray.size(); i++){
                    try {
                        E o = null;
                        if (!elementArray.get(i).isJsonNull()){
                            Class elementType = Class.forName(elementArrayType.get(i).getAsString());
                            o = (E)gson.fromJson(elementArray.get(i), Class.forName(elementArrayType.get(i).getAsString()));
                        }
                        collection.add(o);
                    } catch (ClassNotFoundException e){
                        e.printStackTrace();
                    }
                }
            } else {
                throw new IllegalStateException("[UniversalCollectionTypeAdapter] cannot deserialize "
                        + jsonElement.getClass().getSimpleName() + " \n" + jsonElement);
            }

            /*
            // ORIGINAL CODE
            Collection<E> collection = constructor.construct();
            in.beginArray();
            while (in.hasNext()) {
                E instance = elementTypeAdapter.read(in);
                collection.add(instance);
            }
            in.endArray();
            */

            return collection;
        }
    }
}