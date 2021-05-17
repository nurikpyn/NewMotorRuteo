package cl.cmm.ruteo.distanceFetcher;

import com.google.gson.Gson;
import cl.cmm.ruteo.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import cl.cmm.ruteo.distanceFetcher.DistanceFetcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

public class RuteoFetcher extends DistanceFetcher {

    public RuteoFetcher(ArrayList<Pair<Double,Double>> points , Set<String> profiles, String folderName, String fileName) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        this.numberOfPoints = points.size();
        for (Pair<Double,Double> point : points) {
            Double xCoordinate = point.getKey();
            Double yCoordinate = point.getValue();
            stringBuilder.append(String.format("%f,%f;", xCoordinate, yCoordinate));
        }
        stringBuilder.setLength(stringBuilder.length()-1);
        String pointsAsString = stringBuilder.toString();
        //if (profiles.size()>1 || !profiles.contains("car")){
        //    throw new UnexpectedProfileException("RuteoFetcher only allows \"car\" as vehicle profile.");
        //}
        //String url = String.format("http://ruteo.cmm.uchile.cl/table/v1/driving/%s?annotations=duration,distance&generate_hints=false", pointsAsString);
        /*String url = String.format("http://localhost:5000/table/v1/driving/%s?annotations=duration,distance&generate_hints=false", pointsAsString);
        System.out.println(String.format("Reading from: %s", url));
        String jsonQuery = readUrl(url);
        new File(String.format("%s/distances/%s/", folderName, "car")).mkdirs();
        try (PrintStream out = new PrintStream(new FileOutputStream(String.format("%s/distances/%s/%s", folderName, "car" ,fileName )))) {
            out.print(jsonQuery);
        }*/
        for (String profile : profiles) {
            String url = String.format("http://ruteo.cmm.uchile.cl/table/v1/driving/%s?annotations=duration,distance&generate_hints=false", pointsAsString);
            //String url = String.format("http://localhost:5000/table/v1/driving/%s?annotations=duration,distance&generate_hints=false", pointsAsString);
            System.out.println(String.format("Reading from: %s", url));
            String jsonQuery = readUrl(url);
            new File(String.format("%s/distances/%s/", folderName, profile)).mkdirs();
            try (PrintStream out = new PrintStream(new FileOutputStream(String.format("%s/distances/%s/%s", folderName, profile ,fileName )))) {
                out.print(jsonQuery);
            }

            JSONObject data=(JSONObject)new JSONTokener(jsonQuery).nextValue();
            JSONArray jsonDurations = data.getJSONArray("durations");
            JSONArray jsonDistances = data.getJSONArray("distances");

            Gson gson = new Gson();
            double[][] times = gson.fromJson(jsonDurations.toString(), double[][].class);
            double[][] distances = gson.fromJson(jsonDistances.toString(), double[][].class);

            this.multiTypeMatrix.putMatrix(profile, new Matrix(times,distances));
        }

        /*JSONObject data=(JSONObject)new JSONTokener(jsonQuery).nextValue();
        JSONArray jsonDurations = data.getJSONArray("durations");
        JSONArray jsonDistances = data.getJSONArray("distances");

        Gson gson = new Gson();
        double[][] times = gson.fromJson(jsonDurations.toString(), double[][].class);
        double[][] distances = gson.fromJson(jsonDistances.toString(), double[][].class);

        this.multiTypeMatrix.putMatrix("car", new Matrix(times,distances));*/
    }
}
