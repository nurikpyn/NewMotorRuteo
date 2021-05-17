package cl.cmm.ruteo;

//// ATTENTION: To use it, change ExtraTraffic for Congestion in MainTraffic line 31.

import cl.cmm.ruteo.data.GeometricSquareTest;
import cl.cmm.ruteo.data.TrafficData;
import cl.cmm.ruteo.data.ProblemData;
import cl.cmm.ruteo.data.TspData;
import cl.cmm.ruteo.jsonProcessing.JsonFile;
import cl.cmm.ruteo.jsonProcessing.JsonObjective;
import cl.cmm.ruteo.jsonProcessing.JsonTraffic;
import cl.cmm.ruteo.jsonProcessing.JsonService;
import cl.cmm.ruteo.solvers.AbstractSolver;
import cl.cmm.ruteo.solvers.BSMinMaxSolver;
import cl.cmm.ruteo.solvers.PenaltyMinMaxSolver;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

public class Congestion {


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
                data = new TrafficData(jsonFile, folderName, fileName, costSource, key, newService);
                break;
            default:
                throw new UnexpectedArgumentException("Unknown -data-source argument");
        }

        JsonTraffic jsonTraffic = jsonFile.traffic.get(0);
        double[] timeSlots = jsonTraffic.getTimeSlots();
        double[] scalingFactors = jsonTraffic.getScalingFactors();

        int numbTimeSlots = timeSlots.length;
        double[] newTimeSlots = new double[numbTimeSlots];
        long totalTime = 0;
        newTimeSlots[0] = 0;
        for (int i = 0; i < numbTimeSlots - 1; i++){
            totalTime += (timeSlots[i+1] - timeSlots[i])/scalingFactors[i];
            newTimeSlots[i+1] = totalTime;
        } //Total 'contracted' time and new time slots



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
            
            for (VehicleRoute route : solution.getRoutes()){
                double tTime1 = timeSlots[0], sFactor1 = scalingFactors[0], tSlot1 = newTimeSlots[0];
                double tTime2 = timeSlots[0], sFactor2 = scalingFactors[0], tSlot2 = newTimeSlots[0];

                for (int i = 0; i < route.getActivities().size(); i++) {
                    TourActivity activity = route.getActivities().get(i);
                    //Original times
                    double tKeep, tAnt, multAct, multAnt, TotalTime;
                    tKeep = newTimeSlots[1];
                    tAnt = newTimeSlots[0];
                    multAct = scalingFactors[1];
                    multAnt = scalingFactors[0];
                    TotalTime = timeSlots[0];
                    int j = 1, c = 0, k = 0, p = 0;

                    /*if (tKeep > activity.getEndTime()) {
                        activity.setArrTime(newTimeSlots[0] + (activity.getArrTime() - newTimeSlots[0]) * multAnt);
                    }*/

                    while (tKeep <= activity.getEndTime() || tKeep <= route.getDepartureTime() || tKeep <= route.getEnd().getArrTime()) {

                        if (c == 0 & tKeep > activity.getArrTime()) {
                            activity.setArrTime(TotalTime + (activity.getArrTime() - tAnt) * multAnt);
                            c = 1;
                        }

                        if (k == 0 & tKeep > route.getDepartureTime()) {
                            tTime1 = TotalTime;
                            tSlot1 = tAnt;
                            sFactor1 = multAnt;
                            k = 1;
                        }

                        if (p == 0 & tKeep > activity.getEndTime()) {
                            activity.setEndTime(TotalTime + (activity.getEndTime() - tAnt) * multAnt);
                            p = 1;
                        }


                        TotalTime = timeSlots[j];
                        j += 1;
                        tAnt = tKeep;
                        multAnt = multAct;
                        if (j == timeSlots.length) {
                            break;
                        }
                        tKeep = newTimeSlots[j];
                        multAct = scalingFactors[j];
                    }

                    tTime2 = TotalTime;
                    sFactor2 = multAnt;
                    tSlot2 = tAnt;

                    if (c == 0) {
                        activity.setArrTime(TotalTime + (activity.getArrTime() - tAnt) * multAnt);
                    }
                    if (k == 0) {
                        tTime1 = TotalTime;
                        tSlot1 = tAnt;
                        sFactor1 = multAnt;
                    }
                    if (p == 0) {
                        activity.setEndTime(TotalTime + (activity.getEndTime() - tAnt) * multAnt);
                    }
                }
                route.setVehicleAndDepartureTime(route.getVehicle(), tTime1 + (route.getDepartureTime() - tSlot1) * sFactor1);
                route.getEnd().setArrTime(tTime2 + (route.getEnd().getArrTime() - tSlot2) * sFactor2);
            }
            
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
