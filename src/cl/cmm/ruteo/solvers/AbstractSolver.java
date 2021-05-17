package cl.cmm.ruteo.solvers;

import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;

public abstract class AbstractSolver implements Solver{
    @Override
    public VehicleRoutingProblemSolution solve() {
        return null;
    }
    public void toJson(String outputName) {
    }
}
