package ruteo.jsonProcessing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Set;

public class JsonService {
    public String id;
    public String type;
    public JsonAddress address;
    public Double duration;
    public Integer index;
    public ArrayList<Integer> size;
    public Integer preparation_time;

    public static class Window {
        public Integer earliest;
        public Integer latest;
    }

    public static class PickupDelivery{
        public Double duration;
        public JsonAddress address;
        public ArrayList<Window> time_windows;
    }
    public PickupDelivery pickup;
    public PickupDelivery delivery;
    public ArrayList<Window> time_windows;
    public ArrayList<Window> time_windows_pickup;
    public Set<String> allowed_vehicles;
    public Set<String> disallowed_vehicles;
    public static JsonService fromJson(FileReader in)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(in, JsonService.class);
    }
    public ArrayList<Integer> getSize() {
        return size;
    }
}
