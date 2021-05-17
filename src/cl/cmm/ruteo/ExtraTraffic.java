package cl.cmm.ruteo;


import cl.cmm.ruteo.UnexpectedArgumentException;
import cl.cmm.ruteo.data.*;
import cl.cmm.ruteo.jsonProcessing.JsonFile;
import cl.cmm.ruteo.jsonProcessing.JsonObjective;
import cl.cmm.ruteo.jsonProcessing.JsonTraffic;
import cl.cmm.ruteo.objectiveFunction.SimpleAnalyzer;
import cl.cmm.ruteo.solvers.AbstractSolver;
import cl.cmm.ruteo.solvers.BSMinMaxSolver;
import cl.cmm.ruteo.solvers.PenaltyMinMaxSolver;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

public class ExtraTraffic {


    public static String idFromActivity(TourActivity activity){
        return ((TourActivity.JobActivity) activity).getJob().getId();
    }
    public static class TotalTourTime implements  SolutionCostCalculator{
        public TotalTourTime(){
            super();
        }
        boolean loadDelay = false;
        public void setLoadDelay(boolean loadDelay){ this.loadDelay = loadDelay;}
        public double getCosts(VehicleRoutingProblemSolution solution){
            double maxCosts= SimpleAnalyzer.getTotalOperationTime(solution,loadDelay);
            double penaltyUnassigned =0;
            for (Job job : solution.getUnassignedJobs()){
                penaltyUnassigned +=maxCosts*2*(11-job.getPriority());
            }
            return maxCosts+ penaltyUnassigned;
        }
    }

    static void easySolver(String folderName,String fileName, Map<String, String> jarParameters) throws Exception {
        String costSource =jarParameters.get("-s");
        String key=jarParameters.get("-k");
        int possiblePrecision = Integer.parseInt(jarParameters.get("-p"));
        long timeLimit = Long.parseLong(jarParameters.get("-t"));
        int iterationLimit = Integer.parseInt(jarParameters.get("-i"));
        String customName = jarParameters.get("-cn");
        boolean informerEnabled = Boolean.parseBoolean(jarParameters.get("-inf"));
        boolean loadDelay = Boolean.parseBoolean(jarParameters.get("-dly"));
        double newService = Double.parseDouble(jarParameters.get("-depot-service-time"));
        //String StrtimeSlots = jarParameters.get("-time-slots");
        //String StrScalingFactors = jarParameters.get("-scaling-factors");

        JsonFile jsonFile;

        ProblemData data;

        jsonFile = JsonFile.fromJson(new FileReader(String.format("%s/input/%s", folderName, fileName)));

        // Getting the time slots and scaling factors
        JsonTraffic jsonTraffic = jsonFile.traffic.get(0);
        double[] timeSlots = jsonTraffic.getTimeSlots();
        double[] scalingFactors = jsonTraffic.getScalingFactors();

        data = new JsonDataWithTraffic(jsonFile, folderName, fileName, costSource, key, newService, timeSlots, scalingFactors);

        ArrayList<JsonObjective> jsonObjectives = data.objectives;
        VehicleRoutingProblem problem = data.problem;
        for (JsonObjective objective : jsonObjectives){
            new File(String.format("%s/output/", folderName)).mkdirs();
            AbstractSolver solver;
            if (objective.type.equals("min-max") && objective.value.equals("completion_time")){
                if (jarParameters.get("-sol").equals("BS")){
                    solver = new BSMinMaxSolver(problem, data.fastMatrix);
                    ((BSMinMaxSolver) solver).setPrecision(possiblePrecision);
                    ((BSMinMaxSolver) solver).setTimeLimit(timeLimit);
                    ((BSMinMaxSolver) solver).setIterationLimit(iterationLimit);
                    ((BSMinMaxSolver) solver).setUseInformer(informerEnabled);
                    ((BSMinMaxSolver) solver).setLoadDelay(loadDelay);
                }
                else if (jarParameters.get("-sol").equals("Penalty")){
                    solver = new PenaltyMinMaxSolver(problem, data.fastMatrix);
                    ((PenaltyMinMaxSolver) solver).setTimeLimit(timeLimit);
                    ((PenaltyMinMaxSolver) solver).setIterationLimit(iterationLimit);
                    ((PenaltyMinMaxSolver) solver).setUseInformer(informerEnabled);
                    ((PenaltyMinMaxSolver) solver).setLoadDelay(loadDelay);
                }
                else{
                    throw new RuntimeException("Unknown or incompatible solver");
                }
            }
            else{
                throw new RuntimeException("Unknown objective function");
            }
            String fileNameNoExtension = fileName.substring(0, fileName.lastIndexOf("."));
            String outputName = String.format("%s/output/%s-solution", folderName, fileNameNoExtension);
            VehicleRoutingProblemSolution solution;
            solution = solver.solve();
            solver.toJson(outputName+customName+".json");
            if(loadDelay){
                double capacity,load,serviceDemand,extraTime;
                for (VehicleRoute route : solution.getRoutes()) {
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
                }
            }
            SolutionPrinter.print(problem, solution ,SolutionPrinter.Print.VERBOSE);
        }
    }
}
