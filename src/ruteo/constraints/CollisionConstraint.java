package ruteo.constraints;

import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

class CollisionConstraint implements HardActivityConstraint {


    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext jobInsertionContext, TourActivity tourActivity, TourActivity tourActivity1, TourActivity tourActivity2, double v) {
        return null;
    }
}
