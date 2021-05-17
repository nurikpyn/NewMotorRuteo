package cl.cmm.ruteo.solvers;

import cl.cmm.ruteo.*;
import cl.cmm.ruteo.constraints.MaxRouteDurationConstraint;
import cl.cmm.ruteo.objectiveFunction.MaxTourTime;
import cl.cmm.ruteo.objectiveFunction.SimpleAnalyzer;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.algorithm.termination.TimeTermination;
import com.graphhopper.jsprit.core.algorithm.termination.VariationCoefficientTermination;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.UnassignedJobReasonTracker;

import java.util.Collection;

public class BSMinMaxSolver extends AbstractJspritSolver {
    private int bsIterationLimit = 20;
    private long bsTimeLimit = 2000;
    private double scalingFactor = 0.;
    private int precision = 300;
    private double low;
    private double high;
    private boolean loadDelay = false;

    public BSMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix){
        this.problem=problem;
        this.matrix=matrix;
    }
    public BSMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix, Integer iterationLimit, Integer bsIterationLimit){
        this.problem=problem;
        this.matrix=matrix;
        this.iterationLimit=iterationLimit;
        this.bsIterationLimit=bsIterationLimit;
    }
    public BSMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix, long timeLimit, long bsTimeLimit){
        this.problem=problem;
        this.matrix=matrix;
        this.timeLimit=timeLimit;
        this.bsTimeLimit=bsTimeLimit;
    }
    public BSMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix, long timeLimit, long bsTimeLimit, int iterationLimit, int bsIterationLimit ){
        this.problem=problem;
        this.matrix=matrix;
        this.timeLimit=timeLimit;
        this.iterationLimit=iterationLimit;
        this.bsIterationLimit=bsIterationLimit;
        this.bsTimeLimit=bsTimeLimit;
    }

    public void setBsTimeLimit(long bsTimeLimit) {
        this.bsTimeLimit = bsTimeLimit;
    }

    public void setBsIterationLimit(int bsIterationLimit) {
        this.bsIterationLimit = bsIterationLimit;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public void setLoadDelay(boolean loadDelay){ this.loadDelay = loadDelay;}

    public static class FixJobToVehicle implements  HardActivityConstraint {
        private final String vehicleId;
        private final Job job;
        public FixJobToVehicle(Job job, String vehicleId){
            this.vehicleId=vehicleId;
            this.job = job;

        }

        @Override
        public ConstraintsStatus fulfilled(JobInsertionContext jobInsertionContext, TourActivity tourActivity, TourActivity tourActivity1, TourActivity tourActivity2, double v) {
            if (!jobInsertionContext.getNewVehicle().getId().equals(this.vehicleId) && jobInsertionContext.getRoute().getTourActivities().servesJob(this.job)){
                return ConstraintsStatus.NOT_FULFILLED;
            }
            else{
                return  ConstraintsStatus.FULFILLED;
            }
        }
    }
    private Jsprit.Builder builderMaxRouteConstraint(VehicleRoutingProblem problem, Double scalingFactor, Double maxRouteDuration) {
        Jsprit.Builder jspritTempBuilder = Jsprit.Builder.newInstance(problem);
        jspritTempBuilder.setObjectiveFunction(new MaxTourTime(scalingFactor,loadDelay));
        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem,stateManager);
        HardActivityConstraint maxRoute = new MaxRouteDurationConstraint(maxRouteDuration, stateManager, problem.getTransportCosts());
        constraintManager.addConstraint(maxRoute,ConstraintManager.Priority.HIGH);
        jspritTempBuilder.setStateAndConstraintManager(stateManager,constraintManager);
        return  jspritTempBuilder;
    }
    @Override
    public VehicleRoutingProblemSolution solve() {
        long startTime = System.nanoTime();
        /* First solution of the problem to set upper limit in the binarySearch*/
        Jsprit.Builder firstBuild = Jsprit.Builder.newInstance(this.problem);
        firstBuild.setProperty(Jsprit.Parameter.THREADS, "5");
        firstBuild.setObjectiveFunction(new MaxTourTime(0.,loadDelay));
        VehicleRoutingAlgorithm firstAlg = firstBuild.buildAlgorithm();
        firstAlg.setMaxIterations(this.iterationLimit);
        if (this.timeLimit > 0) {
            TimeTermination timeTermination = new TimeTermination(this.timeLimit);
            firstAlg.setPrematureAlgorithmTermination(timeTermination);
            firstAlg.addListener(timeTermination);
        }
        this.reasonTracker = new UnassignedJobReasonTracker();
        firstAlg.addListener(reasonTracker);
        firstAlg.setPrematureAlgorithmTermination(new FeasibleTermination());
        Collection<VehicleRoutingProblemSolution> solutions = firstAlg.searchSolutions();
        VehicleRoutingProblemSolution firstSolution = Solutions.bestOf(solutions);
        this.high = SimpleAnalyzer.getMaxOperationTime(firstSolution,loadDelay);
        this.low =this.high/2;
        Integer iterations = (int) Math.ceil(Math.log(this.high / this.precision) / Math.log(2)) + 1;
        this.solution = firstSolution;
        if (firstSolution.getUnassignedJobs().size() > 0) {
            this.elapsedTime = (System.nanoTime()-startTime)/1000000;
            System.out.println("WARNING: Couldn't assign all jobs");
        }
        /* If all services assigned, employ binary search */
        else {
            System.out.printf("%d ITERATIONS LEFT. All services assigned. \n\t    attempt: %f\n\t Current best: %f \n", iterations, this.low, this.high);
            while (iterations > 0) {
                Jsprit.Builder tempBuilder = builderMaxRouteConstraint(this.problem, this.scalingFactor, (low + high) / 2);
                tempBuilder.setProperty(Jsprit.Parameter.THREADS, "5");
                VehicleRoutingAlgorithm tempAlg = tempBuilder.buildAlgorithm();
                tempAlg.setMaxIterations(this.bsIterationLimit);
                if (this.bsTimeLimit > 0) {
                    TimeTermination timeTermination = new TimeTermination(this.bsTimeLimit);
                    tempAlg.setPrematureAlgorithmTermination(timeTermination);
                    tempAlg.addListener(timeTermination);
                }
                tempAlg.setPrematureAlgorithmTermination(new FeasibleTermination());
                Collection<VehicleRoutingProblemSolution> tempSolutions = tempAlg.searchSolutions();
                VehicleRoutingProblemSolution tempSolution = Solutions.bestOf(tempSolutions);
                double currentMaxRoute = SimpleAnalyzer.getMaxOperationTime(tempSolution,loadDelay);
                iterations--;
                if ((tempSolution.getUnassignedJobs().size() > 0)) {
                    this.low = (this.low + this.high) / 2;
                    System.out.printf("%d ITERATIONS LEFT. Failed to assign all services. \n\t  Next attempt: %f\n\t Current best: %f \n", iterations, this.low, this.high);
                }
                else if (currentMaxRoute>this.low ){
                    this.low = (this.low + this.high) / 2;
                    System.out.printf("%d ITERATIONS LEFT. Longest route wasn't reduced enough \n\t  Next attempt: %f\n\t Current best: %f \n", iterations, this.low, this.high);
                }
                else {
                    this.high = SimpleAnalyzer.getMaxOperationTime(tempSolution,loadDelay);
                    this.solution = tempSolution;
                    System.out.printf("%d ITERATIONS LEFT. All services assigned. \n\t  Next attempt: %f\n\t Current best: %f \n", iterations, (this.high + this.low) / 2, this.high);
                }

            }
            /* Refine solution found */
            Jsprit.Builder finalBuilder = Jsprit.Builder.newInstance(this.problem);
            finalBuilder.setProperty(Jsprit.Parameter.THREADS, "5");
            StateManager stateManager = new StateManager(this.problem);
            ConstraintManager constraintManager = new ConstraintManager(this.problem,stateManager);
            double oldLength = SimpleAnalyzer.getTotalOperationTime(this.solution,loadDelay);
            HardActivityConstraint maxRoute = new MaxRouteDurationConstraint(SimpleAnalyzer.getMaxOperationTime(this.solution,loadDelay), stateManager, problem.getTransportCosts());
            constraintManager.addConstraint(maxRoute,ConstraintManager.Priority.HIGH);
            finalBuilder.setStateAndConstraintManager(stateManager,constraintManager);
            finalBuilder.setObjectiveFunction(new Extra.TotalTourTime());
            VehicleRoutingAlgorithm finalAlg = finalBuilder.buildAlgorithm();
            finalAlg.setMaxIterations(this.iterationLimit);
            if (this.timeLimit > 0) {
                TimeTermination timeTermination = new TimeTermination(this.timeLimit);
                finalAlg.setPrematureAlgorithmTermination(timeTermination);
                finalAlg.addListener(timeTermination);
            }
            VariationCoefficientTermination coefficientTermination = new VariationCoefficientTermination(this.iterationsTermination, this.variationCoefficientThreshold);
            finalAlg.setPrematureAlgorithmTermination(coefficientTermination);
            finalAlg.addListener(coefficientTermination);
            Collection<VehicleRoutingProblemSolution> finalSolutions =  finalAlg.searchSolutions();
            VehicleRoutingProblemSolution newSolution = Solutions.bestOf(finalSolutions);
            this.elapsedTime = (System.nanoTime()-startTime)/1000000;
            double newLength = SimpleAnalyzer.getTotalOperationTime(newSolution,loadDelay);
            if (newLength<oldLength && newSolution.getUnassignedJobs().size()==0){
                System.out.printf("SOLUTION IMPROVED BY %f \n", oldLength-newLength);
                this.solution = newSolution;
            }
        }
        return this.solution;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }
}