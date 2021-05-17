package ruteo;

import ruteo.Extra;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

class InDirectSequenceConstraint implements HardActivityConstraint {

    private final String id1;
    private final String id2;

    public InDirectSequenceConstraint(String id1, String id2){
        this.id1=id1;
        this.id2=id2;
    }
    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext jobInsertionContext, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double v) {
        String idPrevAct = Extra.idFromActivity(prevAct);
        String idNewAct = Extra.idFromActivity(newAct);
        String idNextAct = Extra.idFromActivity(nextAct);
        if ((idNewAct.equals(this.id1) && idNextAct.equals(this.id2)) || (idPrevAct.equals(this.id1) && idNewAct.equals(this.id2)) ){
            return ConstraintsStatus.FULFILLED;
        }
        else{
            return ConstraintsStatus.NOT_FULFILLED;
        }
    }
}
