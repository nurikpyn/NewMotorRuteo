package ruteo.jsonProcessing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class JsonType {
    public String type_id;
    public String profile;
    public ArrayList<Integer> capacity;

    public static class Costs{
        public Integer fixed = 0;
        public Integer distance = 1;
        public Integer time = 0;
    }
    public Costs costs = new Costs();
    public static JsonType fromJson(FileReader in)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(in, JsonType.class);
    }
    @Override
    public String toString() {
        String listString = this.capacity.stream().map(Object::toString).collect(Collectors.joining(", "));
        return this.type_id+"; "+this.profile+"; "+listString;
    }

    public ArrayList<Integer> getCapacity() {
        return capacity;
    }

    public String getId() {
        return type_id;
    }
}
