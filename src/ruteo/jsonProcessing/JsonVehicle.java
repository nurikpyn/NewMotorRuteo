package ruteo.jsonProcessing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;

public class JsonVehicle {
    public String vehicle_id;
    public JsonAddress start_address;
    public JsonAddress end_address;
    public String type_id;
    public Integer earliest_start;
    public static JsonVehicle fromJson(FileReader in)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(in, JsonVehicle.class);
    }

    public String getType() {
        return type_id;
    }
}
