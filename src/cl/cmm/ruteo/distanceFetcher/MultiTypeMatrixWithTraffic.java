package cl.cmm.ruteo.distanceFetcher;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class MultiTypeMatrixWithTraffic implements VehicleRoutingTransportCosts{
    private final Map<String,Matrix> matrices= new HashMap<>();
    private int noTimeSlots;
    private double[] timeSlots;

    private double[] scalingFactors;
    void putMatrix(String profile, Matrix matrix){
        this.matrices.put(profile, matrix);
    }

    /** Constructor for a MultiTypeMatrix, which considers multiple types of vehicles plus traffic.
     * @param timeSlots Sequence of numbers 0=t_0<t_1<...<t_n.
     * @param scalingFactors Sequence of numbers a_0,a_1,...,a_n such that at the timeSlot [t_i,t_{i+1}) the speed of each vehicle is scaled by a_i. Here t_{n+1}=infinity.
     * @throws TimeSlotException whenever the conditions of the parameters aren't met. That is, same length t_0=0 and the sequence t_0,t_1,...,t_n is strictly increasing.
     */
    MultiTypeMatrixWithTraffic(double[] timeSlots, double[] scalingFactors) throws TimeSlotException {
        if (timeSlots.length!=scalingFactors.length) {
            throw new TimeSlotException("Time slots and Scaling Factors must have the same length");
        }
        this.noTimeSlots=timeSlots.length;
        this.timeSlots = timeSlots;
        this.scalingFactors = scalingFactors;
        for (int i = 0; i < this.noTimeSlots-1; i++) {
            if (this.timeSlots[i]>this.timeSlots[i+1]){
                throw new TimeSlotException("Time slots must be an strictly increasing sequence");
            }
        }
        if(this.timeSlots[0]!=0){
            throw new TimeSlotException("First element in TimeSlots must be 0");
        }
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
        double time;  // Time between from and to
        double distance; //Distance between from and to
        if (vehicle==null){
            time = this.getDefaultMatrix().getTimes()[to.getIndex()][from.getIndex()];
            distance = this.getDefaultMatrix().getDistances()[to.getIndex()][from.getIndex()];
        }
        else{
            time = this.matrices.get(vehicle.getType().getProfile()).getTimes()[to.getIndex()][from.getIndex()];
            distance = this.matrices.get(vehicle.getType().getProfile()).getDistances()[to.getIndex()][from.getIndex()];
        }
        int index=0;

        if (arrivalTime >= this.timeSlots[this.noTimeSlots - 1]){
            index = this.noTimeSlots - 1;
        }
        else {
            for (int i = 0; i < this.noTimeSlots; i++) {
                if (arrivalTime < this.timeSlots[i]) {
                    index = i - 1;
                    break;
                }
            }
        }

        double remainingDistance = distance;
        double velocity = distance/time;
        double elapsedTime = arrivalTime;
        double deltaT;
        while (index<this.noTimeSlots-1){
            deltaT = (this.timeSlots[index+1]-elapsedTime);
            if (velocity*this.scalingFactors[index]*deltaT<=remainingDistance){
                remainingDistance = remainingDistance - velocity*this.scalingFactors[index]*deltaT;
                elapsedTime = elapsedTime + deltaT;
                index++;
            }
            else{
                return elapsedTime + remainingDistance/(velocity*this.scalingFactors[index]) - arrivalTime;
            }
        }
        return elapsedTime + remainingDistance/(velocity*this.scalingFactors[index]) - arrivalTime;
    }

    @Override
    public double getTransportCost(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) {
        return getTransportTime(from,to,arrivalTime,driver,vehicle);
    }

    /** Obtains traffic-adjusted transport time between from and to.
     * @param from origin location
     * @param to destination location
     * @param arrivalTime time at which we arrived at from
     * @param driver driver of the truck
     * @param vehicle vehicle used
     * @return traffic-adjusted transport time.
     */
    @Override
    public double getTransportTime(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) {
        double time;  // Time between from and to
        double distance; //Distance between from and to
        if (vehicle==null){
            time = this.getDefaultMatrix().getTimes()[from.getIndex()][to.getIndex()];
            distance = this.getDefaultMatrix().getDistances()[from.getIndex()][to.getIndex()];
        }
        else{
            time = this.matrices.get(vehicle.getType().getProfile()).getTimes()[from.getIndex()][to.getIndex()];
            distance = this.matrices.get(vehicle.getType().getProfile()).getDistances()[from.getIndex()][to.getIndex()];
        }
        int index = 0;

        if (arrivalTime >= this.timeSlots[this.noTimeSlots - 1]){
            index = this.noTimeSlots - 1;
        }
        else {
            for (int i = 0; i < this.noTimeSlots; i++) {
                if (arrivalTime < this.timeSlots[i]) {
                    index = i - 1;
                    break;
                }
            }
        }

        double remainingDistance = distance;
        double velocity = distance/time;
        double elapsedTime = arrivalTime;
        double deltaT;
        while (index<this.noTimeSlots-1){
            deltaT = (this.timeSlots[index+1]-elapsedTime);
            if (velocity*this.scalingFactors[index]*deltaT<=remainingDistance){
                remainingDistance = remainingDistance - velocity*this.scalingFactors[index]*deltaT;
                elapsedTime = elapsedTime + deltaT;
                index++;
            }
            else{
                return elapsedTime + remainingDistance/(velocity*this.scalingFactors[index]) - arrivalTime;
            }
        }
        return elapsedTime + remainingDistance/(velocity*this.scalingFactors[index]) - arrivalTime;
    }

    public double getDistance(int fromIndex, int toIndex) {
        return this.getDefaultMatrix().getDistances()[fromIndex][toIndex];
    }

    private Set<String> getProfiles(){
        return this.matrices.keySet();
    }
}