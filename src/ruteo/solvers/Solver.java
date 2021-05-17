package ruteo.solvers;

import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;

interface Solver {
    VehicleRoutingProblemSolution solve();
}
