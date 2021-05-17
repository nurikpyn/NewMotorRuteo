package ruteo.distanceFetcher;

import com.google.gson.Gson;
import ruteo.distanceFetcher.DistanceFetcher;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Set;

public class LocalFetcher extends DistanceFetcher {

    public LocalFetcher(int numberOfPoints,Set<String> profiles, String folderName, String fileName) throws FileNotFoundException {
        this.numberOfPoints = numberOfPoints;
        Gson gson = new Gson();
        for (String profile : profiles){
            Matrix matrix = gson.fromJson(new FileReader(String.format("%s/distances/%s/%s", folderName, profile ,fileName )), Matrix.class);
            this.multiTypeMatrix.putMatrix(profile,matrix);
        }
    }
}
