package cl.cmm.ruteo.objectiveFunction;

import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;

public class MaxTourTime implements SolutionCostCalculator {


    private final Double scalingParameter;
    private final Boolean loadDelay;

    /**
     * MaxTourTime is a calculator for weighted average between the longest route and the total (sum) of the routes.
     * @param scalingParameter scales the weight of the total of the routes (>>1, means that it weights more).
     */
    public MaxTourTime(Double scalingParameter, Boolean loadDelay){
        super();
        this.scalingParameter=scalingParameter;
        this.loadDelay = loadDelay;
    }

    /**
     * @param solution is any solution returned by a VehicleRouting Problem
     * @return Weighted average between the longest route and the total (sum) of the routes.
     */
    public double getCosts(VehicleRoutingProblemSolution solution) {
        double maxCosts= SimpleAnalyzer.getMaxOperationTime(solution,loadDelay);
        double penaltyUnassigned =0;
        for (Job job : solution.getUnassignedJobs()){
            penaltyUnassigned +=maxCosts*2*(11-job.getPriority());
        }
        return maxCosts+ penaltyUnassigned +scalingParameter*SimpleAnalyzer.getTotalOperationTime(solution,loadDelay);
    }
}