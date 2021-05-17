package ruteo.jsonProcessing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.util.ArrayList;

public class JsonFile {
    public ArrayList<JsonObjective> objectives;
    public ArrayList<JsonService> services;
    public ArrayList<JsonType> vehicle_types;
    public ArrayList<JsonVehicle> vehicles;
    public ArrayList<JsonRelation> relations;
    public static JsonFile fromJson(FileReader in)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(in, JsonFile.class);
    }

    public ArrayList<JsonObjective> getObjectives() {
        return objectives;
    }

    public ArrayList<JsonService> getServices() {
        return services;
    }

    public ArrayList<JsonRelation> getRelations() {
        return relations;
    }

    public ArrayList<JsonType> getVehicle_types() {
        return vehicle_types;
    }

    public ArrayList<JsonVehicle> getVehicles() {
        return vehicles;
    }
}
