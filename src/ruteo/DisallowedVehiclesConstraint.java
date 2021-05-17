package ruteo;

import com.graphhopper.jsprit.core.problem.constraint.HardRouteConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DisallowedVehiclesConstraint implements HardRouteConstraint {
    private final Map<String, Set<String>> disallowedVehicles = new HashMap<>();

    public DisallowedVehiclesConstraint(){
    }

    public void add(String jobId, Set<String> vehiclesAllowedToServeJob){
        this.disallowedVehicles.put(jobId, vehiclesAllowedToServeJob);
    }
    @Override
    public boolean fulfilled(JobInsertionContext iFact) {
        String jobId = iFact.getJob().getId();
        String vehicleId = iFact.getRoute().getVehicle().getId();
        return !disallowedVehicles.get(jobId).contains(vehicleId);
    }
}

