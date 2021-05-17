package cl.cmm.ruteo.distanceFetcher;

import cl.cmm.ruteo.UnknownValueException;
import cl.cmm.ruteo.jsonProcessing.JsonRelation;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public abstract class DistanceFetcher {

    final MultiTypeMatrix multiTypeMatrix = new MultiTypeMatrix();
    Integer numberOfPoints;

    DistanceFetcher(){}

    static String readUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);
            return buffer.toString();
        }
    }

    public MultiTypeMatrix fetchDistance(){
        return this.multiTypeMatrix;
    }

    public FastVehicleRoutingTransportCostsMatrix getFastMatrix(){
        Matrix defaultCostsMatrix = multiTypeMatrix.getDefaultMatrix();
        double[][] distances = defaultCostsMatrix.getDistances();
        double[][] times = defaultCostsMatrix.getTimes();
        System.out.print(defaultCostsMatrix);
        FastVehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = FastVehicleRoutingTransportCostsMatrix.Builder.newInstance(this.numberOfPoints, Boolean.FALSE);
        for (int i = 0; i <this.numberOfPoints; i++) {
            for (int j = 0; j <this.numberOfPoints; j++) {
                costMatrixBuilder.addTransportTimeAndDistance(i,j,times[i][j],distances[i][j]);
            }
        }
        return costMatrixBuilder.build();
    }

    public void handleRelations(ArrayList<JsonRelation> relations, Map<String, Integer> idsToIndex, Integer vehicleThreshold) throws UnknownValueException {
        for (JsonRelation relation : relations){
            if (relation.type.equals("in_direct_sequence")){
                for (int i = 0; i < relation.ids.size()-1 ; i++) {
                    String from = relation.ids.get(i);
                    String to = relation.ids.get(i+1);
                    if (from.equals("start")){
                        this.multiTypeMatrix.forbidAllButVehiclesIn(idsToIndex.get(to),vehicleThreshold);
                    }
                    else{
                        this.multiTypeMatrix.forbidAllButOneIn(idsToIndex.get(from),idsToIndex.get(to));
                    }
                }
            }
            else{
                throw new UnknownValueException("Unknown relation type");
            }
        }
    }
}
