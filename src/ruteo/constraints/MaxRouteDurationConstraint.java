package ruteo.constraints;

import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

public class MaxRouteDurationConstraint implements HardActivityConstraint {
    private final StateManager stateManager;
    private final double maxRouteDuration;
    private final VehicleRoutingTransportCosts routingCosts;
    public MaxRouteDurationConstraint(double maxRouteDuration, StateManager stateManager, VehicleRoutingTransportCosts routingCosts) {
        this.maxRouteDuration = maxRouteDuration;
        this.routingCosts = routingCosts;
        this.stateManager = stateManager;
    }
    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double depTimeAtPrevAct) {
        double oldDuration = iFacts.getRoute().getEnd().getArrTime() - iFacts.getRoute().getStart().getEndTime();
        if (oldDuration > this.maxRouteDuration){
            return ConstraintsStatus.NOT_FULFILLED_BREAK;
        }
        double tp_time_prevAct_newAct = this.routingCosts.getTransportTime(prevAct.getLocation(), newAct.getLocation(), depTimeAtPrevAct, iFacts.getNewDriver(), iFacts.getNewVehicle());
        double newAct_arrTime = depTimeAtPrevAct + tp_time_prevAct_newAct;
        double newAct_endTime = newAct_arrTime+newAct.getOperationTime();
        double routeDurationIncrease;
        if (nextAct instanceof End && !iFacts.getNewVehicle().isReturnToDepot()) {
            routeDurationIncrease = newAct_endTime - depTimeAtPrevAct;
        }
        else {
            double tp_time_newAct_nextAct = this.routingCosts.getTransportTime(newAct.getLocation(), nextAct.getLocation(), newAct_endTime, iFacts.getNewDriver(), iFacts.getNewVehicle());
            double nextAct_arrTime = newAct_endTime + tp_time_newAct_nextAct;
            double endTime_nextAct_new = nextAct_arrTime +  nextAct.getOperationTime();
            double arrTime_nextAct = depTimeAtPrevAct + this.routingCosts.getTransportTime(prevAct.getLocation(), nextAct.getLocation(), prevAct.getEndTime(), iFacts.getRoute().getDriver(), iFacts.getRoute().getVehicle());
            double endTime_nextAct_old = arrTime_nextAct + nextAct.getOperationTime();
            double endTimeDelay_nextAct = Math.max(0.0D, endTime_nextAct_new - endTime_nextAct_old);
            Double futureWaiting = this.stateManager.getActivityState(nextAct, iFacts.getRoute().getVehicle(), InternalStates.FUTURE_WAITING, Double.class);
            if(futureWaiting == null) {
                futureWaiting = 0.0D;
            }
            routeDurationIncrease = Math.max(0, endTimeDelay_nextAct - futureWaiting);
        }
        double newDuration = oldDuration + routeDurationIncrease;
        if (newDuration > this.maxRouteDuration)
            return ConstraintsStatus.NOT_FULFILLED;
        else
            return ConstraintsStatus.FULFILLED;
    }
}