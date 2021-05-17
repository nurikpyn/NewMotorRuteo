package ruteo;

import com.graphhopper.jsprit.core.problem.constraint.HardRouteConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AllowedVehiclesConstraint implements HardRouteConstraint {
    private final Map<String, Set<String>> allowedVehiclesMap = new HashMap<>();

    public AllowedVehiclesConstraint(){
    }

    public void addSet(String jobId, Set<String> vehiclesAllowedToServeJob){
        this.allowedVehiclesMap.put(jobId, vehiclesAllowedToServeJob);
    }

    public void addVehicle(String jobId, String vehicleAllowedToServeJob){
        if(allowedVehiclesMap.containsKey(jobId)){
            Set<String> allowedVehicles = allowedVehiclesMap.get(jobId);
            allowedVehicles.add(vehicleAllowedToServeJob);
            allowedVehiclesMap.put(jobId, allowedVehicles);
        }
        else{
            Set<String> allowedVehicles = new HashSet<>();
            allowedVehicles.add(vehicleAllowedToServeJob);
            allowedVehiclesMap.put(jobId, allowedVehicles);
        }
    }

    @Override
    public boolean fulfilled(JobInsertionContext iFact) {
        String jobId = iFact.getJob().getId();
        String vehicleId = iFact.getRoute().getVehicle().getId();
        return allowedVehiclesMap.get(jobId).contains(vehicleId);
    }
}
