package cl.cmm.ruteo.data;

import cl.cmm.ruteo.AllowedVehiclesConstraint;
import cl.cmm.ruteo.DisallowedVehiclesConstraint;
import cl.cmm.ruteo.jsonProcessing.JsonObjective;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;

import java.util.ArrayList;

public abstract class ProblemData {
    public VehicleRoutingProblem problem;
    public ArrayList<JsonObjective> objectives;
    public FastVehicleRoutingTransportCostsMatrix fastMatrix;
    final AllowedVehiclesConstraint allowedVehiclesConstraint = new AllowedVehiclesConstraint();
    final DisallowedVehiclesConstraint disallowedVehiclesConstraint = new DisallowedVehiclesConstraint();

    public FastVehicleRoutingTransportCostsMatrix getFastMatrix() {
        return fastMatrix;
    }
}
