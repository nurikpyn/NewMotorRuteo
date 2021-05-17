package ruteo.objectiveFunction;

import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

public class SimpleAnalyzer {

    /**
     * @param solution is a solution for a VehicleRoutingProblem.
     * @return the time of the longest route.
     */
    public static double getMaxOperationTime(VehicleRoutingProblemSolution solution, boolean loadDelay){
        double maxOpTime = 0;
        for (VehicleRoute route : solution.getRoutes()){
            maxOpTime=Math.max(maxOpTime, getOperationTime(route,loadDelay));
        }
        return  maxOpTime;
    }

    /**
     * @param route is a route in a VehicleRoutingProblemSolution.
     * @return the time this route takes.
     */
    private static double getOperationTime(VehicleRoute route, boolean loadDelay){
        if(loadDelay) {
            double capacity, load, serviceDemand, extraTime;
            capacity = route.getVehicle().getType().getCapacityDimensions().get(0);
            load = capacity;
            extraTime = 0;
            for (int i = 0; i < route.getActivities().size(); i++) {
                TourActivity activity = route.getActivities().get(i);
                serviceDemand = activity.getSize().get(0);
                activity.setArrTime(activity.getArrTime() - extraTime);
                if (load < capacity / 2) {
                    extraTime += route.getActivities().get(i).getOperationTime() / 2;
                }
                activity.setEndTime(activity.getEndTime() - extraTime);
                load -= serviceDemand;
            }
            return route.getEnd().getArrTime() - route.getStart().getEndTime() - extraTime;
        }
        else {
            return route.getEnd().getArrTime() - route.getStart().getEndTime();
        }
    }

    /**
     * @param solution is a solution for a VehicleRoutingProblem.
     * @return the sum of the times each route takes.
     */
    public static double getTotalOperationTime(VehicleRoutingProblemSolution solution, boolean loadDelay){
        double totalOpTime = 0;
        for (VehicleRoute route : solution.getRoutes()){
            totalOpTime+=getOperationTime(route,loadDelay);
        }
        return totalOpTime;
    }

}
