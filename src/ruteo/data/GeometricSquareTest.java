package ruteo.data;

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

import java.util.ArrayList;
import java.util.Random;

public class GeometricSquareTest extends ProblemData{

    /**
     * Generates coordinates in the square [0,r] times [0,r].
     * @param side is the side r of the square.
     * @return A random coordinate in the square [0,r] times [0,r].
     */
    private static Coordinate coordinateGenerator(double side){
        Random random = new Random();
        return new Coordinate(random.nextDouble()*side,random.nextDouble()*side);
    }

    /**
     * Generates a geometric instance (problemData) in the square of the VehicleRoutingProblem with integer number of trucks from a randomly placed depot and an integer number of clients to serve placed randomly.
     * @param side is the length of the side of the square.
     * @param trucks is the number of trucks that can start from the depot.
     * @param services is the number of clients to be served.
     */
    public GeometricSquareTest(double side, int trucks, int services){
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        ArrayList<Coordinate> coors = new ArrayList<>();
        int cnt =0;
        //Vehicle Type Declaration
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("Vehicle").setProfile("car");
        VehicleType vehicleType = vehicleTypeBuilder.build();

        //Vehicles Declaration
        Coordinate depot = coordinateGenerator(side);
        for (int i = 0; i <trucks ; i++) {
            coors.add(depot);
            Location location = Location.Builder.newInstance().setCoordinate(depot).setId("Depot").setIndex(cnt).build();
            VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(String.format("Vehicle %d", cnt)).setStartLocation(location).setEndLocation(location);
            vehicleBuilder.setType(vehicleType);
            vrpBuilder.addVehicle(vehicleBuilder.build());
            cnt++;
        }

        //Service declaration
        for (int i = 0; i <services ; i++) {
            Coordinate coordinate = coordinateGenerator(side);
            coors.add(coordinate);
            Location location = Location.Builder.newInstance().setCoordinate(coordinate).setId(String.format("Service %d", cnt)).setIndex(cnt).build();
            Service.Builder jobBuilder = Service.Builder.newInstance(String.format("Service %d",cnt));
            jobBuilder.setLocation(location);
            vrpBuilder.addJob(jobBuilder.build());
            cnt++;
        }

        //Distance calculation
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
