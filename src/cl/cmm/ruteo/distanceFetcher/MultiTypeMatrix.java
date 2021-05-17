package cl.cmm.ruteo.distanceFetcher;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MultiTypeMatrix implements VehicleRoutingTransportCosts {

    private final Map<String,Matrix> matrices= new HashMap<>();

    void putMatrix(String profile, Matrix matrix){
        this.matrices.put(profile, matrix);
    }

    Matrix getDefaultMatrix(){
        if (this.matrices.containsKey("truck")){
            return this.matrices.get("truck");
        }
        else{
            return this.matrices.values().iterator().next();
        }
    }

    void forbidAllButOneIn(int start, int end){
        for (String profile : this.getProfiles()){
            Matrix matrix = this.matrices.get(profile);

            double[][] times = matrix.getTimes();
            double[][] distances = matrix.getDistances();
            int n = times.length;

            for (int i = 0; i <n ; i++) {
                if(i!=start){
                    times[i][end]=Double.MAX_VALUE/1000;
                    distances[i][end]=Double.MAX_VALUE/1000;
                }
            }

            this.matrices.put(profile, new Matrix(times,distances,null));
        }
    }

    void forbidAllButVehiclesIn(int end, int vehicleThreshold){
        for (String profile : this.getProfiles()){
            Matrix matrix = this.matrices.get(profile);

            double[][] times = matrix.getTimes();
            double[][] distances = matrix.getDistances();
            int n = times.length;

            for (int i = vehicleThreshold+1; i <n ; i++) {
                times[i][end]=Double.MAX_VALUE/2;
                distances[i][end]=Double.MAX_VALUE/2;
            }
            this.matrices.put(profile, new Matrix(times,distances, null));
        }
    }


    @Override
    public double getBackwardTransportCost(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) {
        return getBackwardTransportTime(from,to,arrivalTime,driver,vehicle);
    }

    @Override
    public double getBackwardTransportTime(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) {
        if (vehicle==null){
            return this.getDefaultMatrix().getTimes()[to.getIndex()][from.getIndex()];
        }
        return this.matrices.get(vehicle.getType().getProfile()).getTimes()[to.getIndex()][from.getIndex()];
    }

    @Override
    public double getTransportCost(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) {
        return getTransportTime(from,to,arrivalTime,driver,vehicle);
    }

    @Override
    public double getTransportTime(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) {
        if (vehicle==null){
            return this.getDefaultMatrix().getTimes()[from.getIndex()][to.getIndex()];
        }
        return this.matrices.get(vehicle.getType().getProfile()).getTimes()[from.getIndex()][to.getIndex()];
    }

    public double getDistance(int fromIndex, int toIndex) {
        return this.getDefaultMatrix().getDistances()[fromIndex][toIndex];
    }

    private Set<String> getProfiles(){
        return matrices.keySet();
    }
}
