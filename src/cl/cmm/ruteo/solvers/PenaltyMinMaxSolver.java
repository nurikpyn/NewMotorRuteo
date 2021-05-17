package cl.cmm.ruteo.solvers;

import cl.cmm.ruteo.Informer;
import cl.cmm.ruteo.objectiveFunction.MaxTourTime;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.algorithm.state.StateUpdater;
import com.graphhopper.jsprit.core.algorithm.termination.TimeTermination;
import com.graphhopper.jsprit.core.algorithm.termination.VariationCoefficientTermination;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.SoftActivityConstraint;
import com.graphhopper.jsprit.core.problem.cost.ForwardTransportTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.ActivityTimeTracker;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import com.graphhopper.jsprit.core.util.Solutions;

import java.io.IOException;
import java.util.Collection;

public class PenaltyMinMaxSolver extends AbstractJspritSolver {

    private double scalingFactor = 0.;

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public PenaltyMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix){
        this.problem=problem;
        this.matrix=matrix;
    }
    public PenaltyMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix, Long timeLimit){
        this.problem=problem;
        this.matrix=matrix;
        this.timeLimit=timeLimit;
    }
    public PenaltyMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix, Long timeLimit, Integer iterationLimit){
        this.problem=problem;
        this.matrix=matrix;
        this.timeLimit=timeLimit;
        this.iterationLimit=iterationLimit;
    }
    public PenaltyMinMaxSolver(VehicleRoutingProblem problem, FastVehicleRoutingTransportCostsMatrix matrix, Integer iterationLimit){
        this.problem=problem;
        this.matrix=matrix;
        this.iterationLimit=iterationLimit;
    }
    static class PenalizeIncreasedLongestTour implements SoftActivityConstraint {
        private final VehicleRoutingTransportCosts routingCosts;
        private final StateManager stateManager;
        private double penaltyFactor = 3.;


        PenalizeIncreasedLongestTour(StateManager stateManager, VehicleRoutingTransportCosts routingCosts){
            super();
            this.routingCosts=routingCosts;
            this.stateManager=stateManager;
        }
        @Override
        public double getCosts(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double depTimAtPrevAct){
            double maxTour = stateManager.getProblemState(stateManager.createStateId("maxTour"), Double.class);
            double timeBetweenPrevActNewAct = routingCosts.getTransportCost(prevAct.getLocation(), newAct.getLocation(),depTimAtPrevAct,iFacts.getNewDriver(), iFacts.getNewVehicle());
            double newActArrivalTime =  prevAct.getOperationTime()+depTimAtPrevAct + timeBetweenPrevActNewAct;
            double newActEndTime = newAct.getOperationTime()+newActArrivalTime;
            // If the route ends at nextAct
            if (nextAct instanceof End){
                if(!iFacts.getNewVehicle().isReturnToDepot()){
                    double newRoutesTransportTime = iFacts.getRoute().getEnd().getArrTime() - iFacts.getRoute().getStart().getEndTime() + timeBetweenPrevActNewAct;
                    return penaltyFactor*Math.max(0,newRoutesTransportTime-maxTour);
                }
            }
            double timeBetweenNewActNextAct = routingCosts.getTransportCost(newAct.getLocation(), nextAct.getLocation(),newActEndTime,iFacts.getNewDriver(),iFacts.getNewVehicle());
            double nextActArrivalTime = newActEndTime + timeBetweenNewActNextAct;
            double oldTime;
            if(iFacts.getRoute().isEmpty()){
                oldTime = (nextAct.getArrTime()-depTimAtPrevAct);
            }
            else {
                oldTime = (nextAct.getArrTime()-iFacts.getRoute().getDepartureTime());
            }
            double additionalTime = (nextActArrivalTime-iFacts.getNewDepTime())-oldTime;
            double tpTime = iFacts.getRoute().getEnd().getArrTime() - iFacts.getRoute().getStart().getEndTime() + additionalTime;
            return penaltyFactor*Math.max(0, tpTime-maxTour);
        }
        void setPenaltyFactor(double penaltyFactor) {
            this.penaltyFactor = penaltyFactor;
        }
    }
    static class UpdateMaxTransportTime implements ActivityVisitor, StateUpdater {
        private final StateManager stateManager;
        private final ActivityTimeTracker timeTracker;
        UpdateMaxTransportTime(StateManager stateManager, ForwardTransportTime transportTime, VehicleRoutingActivityCosts vehicleRoutingActivityCosts) {
            super();
            this.stateManager = stateManager;
            this.timeTracker = new ActivityTimeTracker(transportTime, vehicleRoutingActivityCosts);
        }
        public void begin(VehicleRoute route) {
            timeTracker.begin(route);
        }
        public void visit(TourActivity activity) {
            timeTracker.visit(activity);
        }
        public void finish() {
            timeTracker.finish();
            double newRouteEndTime = timeTracker.getActArrTime();
            double currentMaxTransportTime;
            try {
                currentMaxTransportTime = stateManager.getProblemState(
                        stateManager.createStateId("maxTour"), Double.class);
            }
            catch (NullPointerException ne) {
                currentMaxTransportTime = 0.0;
            }
            if(newRouteEndTime > currentMaxTransportTime){
                stateManager.putProblemState(stateManager.createStateId("maxTour"), Double.class, newRouteEndTime);
            }
        }
    }
    @Override
    public VehicleRoutingProblemSolution solve() {

        Jsprit.Builder jspritBuilder = Jsprit.Builder.newInstance(problem);
        jspritBuilder.setProperty(Jsprit.Parameter.THREADS,"5");
        long startTime= System.nanoTime();
        boolean loadDelay = false;
        jspritBuilder.setObjectiveFunction(new MaxTourTime(scalingFactor, loadDelay));
        StateManager stateManager = new StateManager(problem);
        stateManager.createStateId("maxTour");
        stateManager.putProblemState(stateManager.createStateId("maxTour"), Double.class , 0.);
        stateManager.addStateUpdater(new UpdateMaxTransportTime(stateManager, problem.getTransportCosts(), problem.getActivityCosts()));
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        PenalizeIncreasedLongestTour penalty = new PenalizeIncreasedLongestTour(stateManager, problem.getTransportCosts() );
        double penaltyFactor = 3.;
        penalty.setPenaltyFactor(penaltyFactor);
        constraintManager.addConstraint(penalty);
        jspritBuilder.setStateAndConstraintManager(stateManager, constraintManager);
        VehicleRoutingAlgorithm algorithm = jspritBuilder.buildAlgorithm();

        algorithm.setMaxIterations(iterationLimit);
        if (timeLimit>0) {
            TimeTermination timeTermination = new TimeTermination(timeLimit);
            algorithm.setPrematureAlgorithmTermination(timeTermination);
            algorithm.addListener(timeTermination);
        }
        VariationCoefficientTermination coefficientTermination = new VariationCoefficientTermination(this.iterationsTermination, this.variationCoefficientThreshold);
        algorithm.setPrematureAlgorithmTermination(coefficientTermination);
        algorithm.addListener(coefficientTermination);
        if (this.useInformer) {
            Informer informer = new Informer(true, true, true);
            algorithm.addListener(informer);
            Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
            solution = Solutions.bestOf(solutions);
            this.elapsedTime = (System.nanoTime()-startTime)/1000000;
            try {
                informer.plot(1000,600);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
            solution = Solutions.bestOf(solutions);
        }
        return solution;
        }
}

