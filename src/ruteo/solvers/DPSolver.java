package ruteo.solvers;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import com.graphhopper.jsprit.core.util.Solutions;

import java.util.Collection;
import java.util.Iterator;

import static java.lang.Double.max;

public class DPSolver extends AbstractJspritSolver {
    public DPSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix){
        this.problem=problem;
        this.matrix=matrix;
    }

    @Override
    public VehicleRoutingProblemSolution solve() {
        //TSP resolution
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addAllJobs(problem.getJobs().values());
        Coordinate oldCoordinate = null;
        Coordinate newCoordinate;

        for (Vehicle vehicle : problem.getVehicles()) {
            newCoordinate = vehicle.getStartLocation().getCoordinate();
            if (oldCoordinate !=null && !oldCoordinate.equals(newCoordinate)){
                throw new RuntimeException("Multi-Depot problem, TSP only allows Single-Depot problems");
            }
            oldCoordinate = newCoordinate;
        }
        Location start = Location.Builder.newInstance().setId("0").setIndex(0).setCoordinate(oldCoordinate).build();
        Location arrival = Location.Builder.newInstance().setId("0").setIndex(0).setCoordinate(oldCoordinate).build();
        VehicleType vehicleType = VehicleTypeImpl.Builder.newInstance("0").addCapacityDimension(0,Integer.MAX_VALUE).build();
        VehicleImpl vehicle = VehicleImpl.Builder.newInstance("0").setStartLocation(start).setEndLocation(arrival).setType(vehicleType).build();
        vrpBuilder.addVehicle(vehicle);
        VehicleRoutingProblem vrp = vrpBuilder.build();
        VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        // Loading dynamic programming data
        int n = problem.getJobs().size();
        double[] delta = new double[n];
        double[] toDepot = new double[n];
        double[] fromDepot = new double[n];
        VehicleRoute tsp = bestSolution.getRoutes().iterator().next();
        Iterator<TourActivity> activityIterator = tsp.getTourActivities().iterator();
        for (int i = 0; activityIterator.hasNext(); i++) {
            Location location = activityIterator.next().getLocation();
            fromDepot[i]=problem.getTransportCosts().getTransportCost(start,location, 0.0, null,null );
        }
        activityIterator = tsp.getTourActivities().iterator();
        for (int i = 0; activityIterator.hasNext() ; i++) {
            Location location = activityIterator.next().getLocation();
            toDepot[i]=problem.getTransportCosts().getTransportCost(location, start, 0.0, null,null );
        }
        activityIterator = tsp.getTourActivities().iterator();
        Location oldLocation = activityIterator.next().getLocation();
        Location newLocation;
        for (int i=0; activityIterator.hasNext() ; i++) {
            TourActivity activity = activityIterator.next();
            newLocation = activity.getLocation();
            delta[i] = problem.getTransportCosts().getTransportCost(oldLocation, newLocation, 0.0, null, null);
            oldLocation = newLocation;
        }
        int m = problem.getVehicles().size();
        double[] distAc = new double[n];
        Integer[][] opt = new Integer[n][m];
        for (int i = 0; i <n ; i++) {
            for (int j = 0; j <m ; j++) {
                opt[i][j]=0;
            }
        }
        distAc[0]=0;
        for (int i = 1; i <n ; i++) {
            distAc[i]=distAc[i-1]+delta[i];
        }
        double candidate;
        double largo;
        double[][] dinProgramming = new double[n][m];
        for (int k = 1; k <m ; k++) {
            for (int i = n-k-1; i >-1 ; i--) {
                double best = Double.POSITIVE_INFINITY;
                for (int l = i; l <n-k ; l++) {
                    largo = fromDepot[i]+toDepot[i]+(distAc[l]-distAc[i]);
                    candidate = max(largo, dinProgramming[l+1][k-1]);
                    if (candidate <best){
                        best = candidate;
                        opt[i][k]=l;
                    }
                    dinProgramming[i][k]=best;
                }
            }
        }
        Integer[] splitArc = new Integer[m];
        int nextSplitIndex = 0;
        Integer splitTrip;
        for (int i = 0; i <m ; i++) {
            splitTrip = opt[nextSplitIndex][m-i-1];
            splitArc[i] = splitTrip+1;
            nextSplitIndex = splitTrip +2;
        }
        System.out.printf("MinMax cost: %f\n"  , dinProgramming[0][m-1]);
        for (int i = 0; i <n ; i++) {
            for (int j = 0; j <m ; j++) {
                System.out.printf("%f", dinProgramming[i][j]);
            }
            System.out.println();
        }
        int cntTour = 1;
        int cntVehicle = 1;
        activityIterator = tsp.getTourActivities().iterator();
        for (Vehicle vehicle1 : problem.getVehicles()) {
            System.out.printf("\nVehicle route: %s\n", vehicle1.getId());
            while(cntTour!=splitArc[cntVehicle] && activityIterator.hasNext()){
                System.out.printf("\tJob: %s\n", activityIterator.next().getLocation().getId());
                cntTour++;
            }
            cntVehicle++;
            if (cntVehicle==m) break;
        }
        activityIterator = tsp.getTourActivities().iterator();
        System.out.printf("\tJob: %s\n", activityIterator.next().getLocation().getId());
        return null;
    }
}
