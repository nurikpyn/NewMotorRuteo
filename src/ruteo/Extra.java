package ruteo;


import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import ruteo.data.GeometricSquareTest;
import ruteo.data.JsonData;
import ruteo.data.ProblemData;
import ruteo.data.TspData;
import ruteo.jsonProcessing.JsonFile;
import ruteo.jsonProcessing.JsonObjective;
import ruteo.objectiveFunction.SimpleAnalyzer;
import ruteo.solvers.AbstractSolver;
import ruteo.solvers.BSMinMaxSolver;
import ruteo.solvers.PenaltyMinMaxSolver;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

public class Extra {


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
        String dataSource = jarParameters.get("-data-source");
        double newService = Double.parseDouble(jarParameters.get("-depot-service-time"));
        JsonFile jsonFile;

        ProblemData data;

        switch (dataSource) {
            case "json":
                jsonFile = JsonFile.fromJson(new FileReader(String.format("%s/input/%s", folderName, fileName)));
                data = new JsonData(jsonFile, folderName, fileName, costSource, key, newService);
                break;
            case "TSP":
                if (!jarParameters.keySet().contains("-trucks")){
                    throw new UnexpectedArgumentException("Number of trucks is missing");
                }
                int noTrucksTSP = Integer.parseInt(jarParameters.get("-trucks"));
                data = new TspData(folderName, fileName, noTrucksTSP);
                break;
            case "GeometricSquareTest":
                if (!jarParameters.keySet().contains("-trucks") ){
                    throw new UnexpectedArgumentException("Number of trucks is missing");
                }
                else if (!jarParameters.keySet().contains("-services") ){
                    throw new UnexpectedArgumentException("Number of services is missing");
                }
                else if (!jarParameters.keySet().contains("-size") ){
                    throw new UnexpectedArgumentException("Size of the square is missing");
                }
                int noTrucksGST = Integer.parseInt(jarParameters.get("-trucks"));
                int noServicesGST = Integer.parseInt(jarParameters.get("-services"));
                double size = Double.parseDouble(jarParameters.get("-size"));
                data = new GeometricSquareTest(size, noTrucksGST, noServicesGST);
                break;
            default:
                throw new UnexpectedArgumentException("Unknown -data-source argument");
        }

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
