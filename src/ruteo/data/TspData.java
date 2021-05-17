package ruteo.data;

import ruteo.IncorrectTspException;
import ruteo.jsonProcessing.JsonObjective;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.EuclideanDistanceCalculator;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TspData extends ProblemData {

    public TspData(String folderName, String fileName, int numberOfVehicles) {
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        int cnt=0;
        ArrayList<Coordinate> coors = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(String.format("%s/input/%s", folderName, fileName))))) {
            String[] correctnessCheck = new String[] {"NAME", "COMMENT", "TYPE", "DIMENSION", "EDGE_WEIGHT_TYPE", "NODE_COORD_SECTION"};
            Set<String> set = new HashSet<>(Arrays.asList(correctnessCheck));
            String field = "";
            while (!field.equals("NODE_COORD_SECTION")) {
                String currentLine = br.readLine();
                field = currentLine.split(":")[0].replace(" ", "");
                if (!set.contains(field)){
                    throw new IncorrectTspException("Unexpected TSP Field");
                }
            }
            String currentLine = br.readLine();
            String[] coordinates = currentLine.trim().split("\\s+");
            Coordinate coordinate = Coordinate.newInstance(Double.parseDouble(coordinates[1]),Double.parseDouble(coordinates[2]));
            Location location = Location.Builder.newInstance().setId(String.format("Service %d",Integer.parseInt(coordinates[0]))).setIndex(cnt).setCoordinate(coordinate).build();
            VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("Vehicle").setProfile("car");
            VehicleType vehicleType = vehicleTypeBuilder.build();
            for (int i = 0; i <numberOfVehicles; i++) {
                coors.add(coordinate);

                VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(String.format("Vehicle %d", cnt)).setStartLocation(location).setEndLocation(location);
                vehicleBuilder.setType(vehicleType);
                vrpBuilder.addVehicle(vehicleBuilder.build());
                cnt++;
            }
            while (!(currentLine = br.readLine()).equals("EOF")){
                coordinates = currentLine.trim().split("\\s+");
                coordinate = Coordinate.newInstance(Double.parseDouble(coordinates[1]),Double.parseDouble(coordinates[2]));
                coors.add(coordinate);
                location = Location.Builder.newInstance().setId(String.format("Service %d",Integer.parseInt(coordinates[0]))).setIndex(cnt).setCoordinate(coordinate).build();
                Service.Builder jobBuilder = Service.Builder.newInstance(String.format("Service %d",Integer.parseInt(coordinates[0])));
                jobBuilder.setLocation(location);
                vrpBuilder.addJob(jobBuilder.build());
                cnt++;
            }
        } catch (IOException | IncorrectTspException e) {
            e.printStackTrace();
        }
        FastVehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = FastVehicleRoutingTransportCostsMatrix.Builder.newInstance(cnt, Boolean.FALSE);
        for (int i = 0; i <cnt; i++) {
            for (int j = 0; j <cnt; j++) {
                double distance = EuclideanDistanceCalculator.calculateDistance(coors.get(i),coors.get(j));
                costMatrixBuilder.addTransportTimeAndDistance(i,j,distance, distance);
            }
        }
        this.fastMatrix = costMatrixBuilder.build();
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.setRoutingCost(this.fastMatrix);
        this.problem = vrpBuilder.build();
        ArrayList<JsonObjective> objectives = new ArrayList<>();
        objectives.add(new JsonObjective("min-max","completion_time"));
        this.objectives = objectives;
    }
}
