package ruteo.solvers;


import ruteo.Extra;
import ruteo.jsonProcessing.*;
import ruteo.objectiveFunction.SimpleAnalyzer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphhopper.jsprit.core.analysis.SolutionAnalyser;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import com.graphhopper.jsprit.core.util.UnassignedJobReasonTracker;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public abstract class AbstractJspritSolver extends AbstractSolver {
    VehicleRoutingProblem problem;
    long timeLimit = 60000;
    int iterationLimit = 2000;
    FastVehicleRoutingTransportCostsMatrix matrix;
    VehicleRoutingProblemSolution solution;
    long elapsedTime = 0;
    int iterationsTermination = 100;
    double variationCoefficientThreshold = 0.01;
    private boolean loadDelay = false;
    boolean useInformer = false;
    UnassignedJobReasonTracker reasonTracker = null;
    public void toJson(String outputName){
        /* general routes data*/
        SolutionAnalyser analyser = new SolutionAnalyser(this.problem, this.solution,this.matrix);
        JsonSolution jsonSolution = new JsonSolution();
        jsonSolution.setDistance(analyser.getDistance());
        jsonSolution.setCosts(analyser.getTotalCosts());
        jsonSolution.setCompletion_time(SimpleAnalyzer.getTotalOperationTime(this.solution,this.loadDelay));
        jsonSolution.setMax_operation_time(SimpleAnalyzer.getMaxOperationTime(this.solution,this.loadDelay));
        jsonSolution.setNo_unassigned(this.solution.getUnassignedJobs().size());
        jsonSolution.setNo_vehicles(this.solution.getRoutes().size());
        jsonSolution.setTransport_time(analyser.getTransportTime());
        jsonSolution.setWaiting_time(analyser.getWaitingTime());
        jsonSolution.setService_time(analyser.getServiceTime());
        jsonSolution.setProcessing_Time(this.elapsedTime);
        /* jSon data for each route */
        for( VehicleRoute route   : this.solution.getRoutes() ){
            JsonRoute tempRoute = new JsonRoute();
            tempRoute.setCompletion_time(analyser.getOperationTime(route));
            tempRoute.setService_duration(analyser.getServiceTime(route));
            tempRoute.setTransport_time(analyser.getTransportTime(route));
            tempRoute.setVehicle_id(route.getVehicle().getId());
            tempRoute.setWaiting_time(analyser.getWaitingTime(route));
            tempRoute.setDistance(analyser.getDistanceAtActivity(route.getEnd(),route));
            /* jSon data for start activity */
            JsonAddress startAddress = new JsonAddress();
            JsonActivity startActivity = new JsonActivity();
            startAddress.location_id= route.getStart().getLocation().getId();
            startAddress.lon= route.getStart().getLocation().getCoordinate().getX();
            startAddress.lat= route.getStart().getLocation().getCoordinate().getY();
            startAddress.name=route.getStart().getLocation().getName();
            startActivity.setAddress(startAddress);
            startActivity.setDistance(0.);
            startActivity.setDriving_time(0.);
            startActivity.setArr_time(route.getStart().getArrTime());
            startActivity.setEnd_time(route.getStart().getEndTime());
            startActivity.setLocation_id(route.getStart().getLocation().getId());
            startActivity.setType(route.getStart().getName());
            startActivity.setWaiting_time(analyser.getWaitingTimeAtActivity(route.getStart(),route));
            tempRoute.addActivity(startActivity);
            /* jSon data for jobs that are not start nor end*/
            for (TourActivity activity : route.getActivities()){
                JsonAddress tempAddress = new JsonAddress();
                JsonActivity tempActivity = new JsonActivity();
                tempAddress.location_id= activity.getLocation().getId();
                tempAddress.lon= activity.getLocation().getCoordinate().getX();
                tempAddress.lat= activity.getLocation().getCoordinate().getY();
                tempAddress.name = activity.getLocation().getName();
                tempActivity.setAddress(tempAddress);
                tempActivity.setId(Extra.idFromActivity(activity));
                tempActivity.setDistance(analyser.getDistanceAtActivity(activity, route));
                tempActivity.setDriving_time(analyser.getTransportTimeAtActivity(activity,route));
                tempActivity.setArr_time(activity.getArrTime());
                tempActivity.setEnd_time(activity.getEndTime());
                tempActivity.setLocation_id(activity.getLocation().getId());
                tempActivity.setType(activity.getName());
                tempActivity.setWaiting_time(analyser.getWaitingTimeAtActivity(activity,route));
                tempRoute.addActivity(tempActivity);
            }
            /* jsonData for the end activity */
            JsonAddress endAddress = new JsonAddress();
            JsonActivity endActivity = new JsonActivity();
            endAddress.location_id= route.getEnd().getLocation().getId();
            endAddress.lon= route.getEnd().getLocation().getCoordinate().getX();
            endAddress.lat= route.getEnd().getLocation().getCoordinate().getY();
            endAddress.name=route.getEnd().getLocation().getName();
            endActivity.setAddress(endAddress);
            endActivity.setDistance(analyser.getDistanceAtActivity(route.getEnd(),route));
            endActivity.setDriving_time(analyser.getTransportTimeAtActivity(route.getEnd(),route));
            endActivity.setArr_time(route.getEnd().getArrTime());
            endActivity.setEnd_time(route.getEnd().getEndTime());
            endActivity.setLocation_id(route.getEnd().getLocation().getId());
            endActivity.setType(route.getEnd().getName());
            endActivity.setWaiting_time(analyser.getWaitingTimeAtActivity(route.getEnd(),route));
            tempRoute.addActivity(endActivity);
            jsonSolution.addRoute(tempRoute);
        }
        /* data for unassigned services */
        if (this.reasonTracker!=null){
            for (Job job: this.solution.getUnassignedJobs()) {
                String id = job.getId();
                String code = String.valueOf(this.reasonTracker.getMostLikelyReasonCode(id));
                String reason = this.reasonTracker.getMostLikelyReason(id);
                jsonSolution.setUnassignedService(id, code,  reason);
            }
        }
        for (String key : problem.getJobs().keySet()){
            System.out.println(key);
        }
        /* Writing json */
        try (Writer writer = new FileWriter(outputName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonSolution, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setIterationLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setIterationsTermination(int iterationsTermination) { this.iterationsTermination = iterationsTermination; }

    public void setVariationCoefficientThreshold(double variationCoefficientThreshold) { this.variationCoefficientThreshold = variationCoefficientThreshold; }

    public long getElapsedTime() {
        return this.elapsedTime;
    }

    public VehicleRoutingProblemSolution getSolution() {
        return solution;
    }

    public void setUseInformer(boolean useInformer) {
        this.useInformer = useInformer;
    }
    public void setLoadDelay(boolean loadDelay) { this.loadDelay = loadDelay;
    }
}
