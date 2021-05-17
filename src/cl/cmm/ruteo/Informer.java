package cl.cmm.ruteo;

import com.graphhopper.jsprit.core.algorithm.listener.IterationEndsListener;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class Informer implements IterationEndsListener {
    private final boolean getObjective;
    private final boolean getVehicles;
    private final boolean getUnassigned;
    private boolean verbose = false;

    private final ArrayList<Double> objectives = new ArrayList<>();
    private final ArrayList<Integer> vehicles = new ArrayList<>();
    private final ArrayList<Integer> unassigned = new ArrayList<>();

    public Informer(boolean getObjective, boolean getVehicles, boolean getUnassigned){
        this.getObjective = getObjective;
        this.getVehicles = getVehicles;
        this.getUnassigned = getUnassigned;
    }
    public void informIterationEnds(int i, VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions){
        if (this.verbose) {
            System.out.printf("Iteration %d:\n", i);
        }
        VehicleRoutingProblemSolution currentBest = Solutions.bestOf(solutions);
        if (this.getObjective){
            double cost = currentBest.getCost();
            this.objectives.add(cost);
            if (this.verbose) {
                System.out.printf("\tObjective = %f\n", cost);
            }
        }
        if (this.getVehicles){
            int vehicle = currentBest.getRoutes().size();
            this.vehicles.add(vehicle);
            if (this.verbose) {
                System.out.printf("\tVehicles = %d\n", vehicle);
            }
        }
        if (this.getUnassigned){
            int unassigned = currentBest.getUnassignedJobs().size();
            this.unassigned.add(unassigned);
            if (this.verbose) {
                System.out.printf("\tUnassigned Services = %d\n", unassigned);
            }
        }
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void plot(int width, int height) throws IOException {
        if (this.getObjective){
            XYSeries seriesObjective = new XYSeries("Objective Function");
            int x = 1;
            for (Double objective : this.objectives){
                seriesObjective.add(x,objective);
                x=x+1;
            }
            XYSeriesCollection dataObjective = new XYSeriesCollection(seriesObjective);
            JFreeChart chartObjective = ChartFactory.createXYLineChart("Objective Function vs Iteration Number", "Iteration" , "Objective Function", dataObjective , PlotOrientation.VERTICAL, true, true, false);
            ChartUtilities.saveChartAsPNG(new File("objective.png"), chartObjective, width, height);
        }
        if (this.getVehicles){
            XYSeries seriesVehicles = new XYSeries("Number of Vehicles");
            int x = 1;
            for (int vehicle : this.vehicles){
                seriesVehicles.add(x,vehicle);
                x=x+1;
            }
            XYSeriesCollection dataVehicle = new XYSeriesCollection(seriesVehicles);
            JFreeChart chartVehicles = ChartFactory.createXYLineChart("Number of vehicles vs Iteration Number", "Iteration" , "Objective Function", dataVehicle , PlotOrientation.VERTICAL, true, true, false);
            ChartUtilities.saveChartAsPNG(new File("vehicle.png"), chartVehicles, width, height);
        }
        if (this.getUnassigned){
            XYSeries seriesUnassigned = new XYSeries("Unassigned Services");
            int x = 1;
            for (int unassigned : this.unassigned){
                seriesUnassigned.add(x, unassigned);
                x=x+1;
            }
            XYSeriesCollection dataUnassigned = new XYSeriesCollection(seriesUnassigned);
            JFreeChart chartUnassigned = ChartFactory.createXYLineChart("Unassigned Services vs Iteration Number", "Iteration" , "Unassigned Services", dataUnassigned , PlotOrientation.VERTICAL, true, true, false);
            ChartUtilities.saveChartAsPNG(new File("unassigned.png"), chartUnassigned, width, height);
        }

    }
}
