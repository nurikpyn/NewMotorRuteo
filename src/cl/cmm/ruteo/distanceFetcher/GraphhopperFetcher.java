package cl.cmm.ruteo.distanceFetcher;

import com.google.gson.Gson;
import cl.cmm.ruteo.util.Pair;
import cl.cmm.ruteo.distanceFetcher.DistanceFetcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

public class GraphhopperFetcher extends DistanceFetcher {
    private int quotaLimit = 80;

    public GraphhopperFetcher(ArrayList<Pair<Double,Double>> points , Set<String> profiles, String folderName, String fileName, String key) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        this.numberOfPoints = points.size();
        for (Pair<Double,Double> point : points) {
            Double xCoor = point.getKey();
            Double yCoor = point.getValue();
            stringBuilder.append(String.format("point=%f,%f&", yCoor, xCoor));
        }
        if (points.size()>quotaLimit){
            throw new RuntimeException("Graphhopper quota exceeded");
        }
        String pointsAsString = stringBuilder.toString();
        for (String profile : profiles) {
            String url = String.format("https://graphhopper.com/api/1/matrix?%svehicle=%s&out_array=distances&out_array=times&debug=true&key=%s", pointsAsString, profile, key);
            System.out.println(String.format("Reading from: %s", url));
            String jsonQuery = readUrl(url);
            new File(String.format("%s/distances/%s/", folderName, profile)).mkdirs();
            try (PrintStream out = new PrintStream(new FileOutputStream(String.format("%s/distances/%s/%s", folderName, profile ,fileName )))) {
                out.print(jsonQuery);
            }
            Gson gson = new Gson();
            Matrix matrix = gson.fromJson(jsonQuery, Matrix.class);
            this.multiTypeMatrix.putMatrix(profile, matrix);
        }
    }

    public void setQuotaLimit(int quotaLimit) {
        this.quotaLimit = quotaLimit;
    }
}
