package ruteo.distanceFetcher;

import ruteo.UnexpectedProfileException;
import com.google.gson.Gson;
import ruteo.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import ruteo.distanceFetcher.DistanceFetcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

public class OneAtATimeFetcher extends DistanceFetcher {
    public OneAtATimeFetcher(ArrayList<Pair<Double,Double>> points , Set<String> profiles, String folderName, String fileName) throws Exception {

        if (profiles.size()>1 || !profiles.contains("car")){
            throw new UnexpectedProfileException("RuteoFetcher only allows \"car\" as vehicle profile.");
        }
        Gson gson = new Gson();
        this.numberOfPoints = points.size();
        double[][] times = new double[points.size()][points.size()];
        double[][] distances = new double[points.size()][points.size()];
        int i=0; int j=0;
        for (Pair<Double,Double> point1 : points) {
            for (Pair<Double, Double> point2 : points) {
                String pointsAsString= String.format("%f,%f;%f,%f", point1.getKey(),point1.getValue(),point1.getKey(),point2.getValue());

                String url = String.format("http://ruteo.cmm.uchile.cl/table_full/v1/driving/%s?annotations=duration,distance&generate_hints=false", pointsAsString);
                String jsonQuery = readUrl(url);
                JSONObject data=(JSONObject)new JSONTokener(jsonQuery).nextValue();
                JSONArray jsonDurations = data.getJSONArray("durations");
                JSONArray jsonDistances = data.getJSONArray("distances");
                double[][] timesPair = gson.fromJson(jsonDurations.toString(), double[][].class);
                double[][] distancesPair = gson.fromJson(jsonDistances.toString(), double[][].class);
                times[i][j]=timesPair[0][1];
                distances[i][j]=distancesPair[0][1];
                j++;
            }
            System.out.println(i);
            i++;
            j=0;
        }
        this.multiTypeMatrix.putMatrix("car", new Matrix(times,distances));
        String timesJson =  gson.toJson(times);
        String distancesJson = gson.toJson(distances);
        new File(String.format("%s/distances/%s/", folderName, "carDistance")).mkdirs();
        try (PrintStream out = new PrintStream(new FileOutputStream(String.format("%s/distances/%s/%s", folderName, "carDistance" ,fileName )))) {
            out.print(distancesJson);
        }
        new File(String.format("%s/distances/%s/", folderName, "carTimes")).mkdirs();
        try (PrintStream out = new PrintStream(new FileOutputStream(String.format("%s/distances/%s/%s", folderName, "carTimes" ,fileName )))) {
            out.print(timesJson);
        }
    }
}