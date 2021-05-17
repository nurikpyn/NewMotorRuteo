package ruteo;

import com.graphhopper.jsprit.core.algorithm.SearchStrategy;
import com.graphhopper.jsprit.core.algorithm.termination.PrematureAlgorithmTermination;

public class FeasibleTermination implements PrematureAlgorithmTermination {

    @Override
    public boolean isPrematureBreak(SearchStrategy.DiscoveredSolution discoveredSolution) {
        return discoveredSolution.getSolution().getUnassignedJobs().size()==0 && discoveredSolution.isAccepted();
    }
}
