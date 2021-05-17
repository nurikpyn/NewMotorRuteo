package cl.cmm.ruteo.data;

import cl.cmm.ruteo.distanceFetcher.*;
import cl.cmm.ruteo.UnknownValueException;

import cl.cmm.ruteo.jsonProcessing.*;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.*;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import cl.cmm.ruteo.util.Pair;
import cl.cmm.ruteo.distanceFetcher.DistanceFetcher;

import java.util.*;

public class TrafficData extends ProblemData {


    /**
     * Constructs problemData (problem, objectives, transportCosts, some constraints etc...) from a Json file.
     * @param jsonFile contains all the data from the Json file.
     * @param folderName is the folder where the Json file is located.
     * @param fileName is the name of the output file. Needed to save the distance information from fetchers.
     * @param costSource is the source of the distance/time information.
     * @param key is the key that (if needed) allows access to the costSource.
     */
    public TrafficData(JsonFile jsonFile, String folderName, String fileName, String costSource, String key, double newService){
        VehicleRoutingProblem problem;
        try {
            VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
            Map<String,ArrayList<Integer>> typeToCapacities = new HashMap<>();
            Map<String,String> typeToProfile = new HashMap<>();
            Map<String,Integer> idsToIndex = new HashMap<>();
            Set<String> profiles = new HashSet<>();
            Integer vehicleThreshold;
            Location lastDepot = null;
            // Loading vehicle types
            for (JsonType jsonType : jsonFile.vehicle_types){
                // Infinite Capacity if not specified
                if (jsonType.capacity == null)
                {
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(Integer.MAX_VALUE);
                    jsonType.capacity = temp;
                }
                typeToCapacities.put(jsonType.type_id,jsonType.capacity);
                System.out.printf("Loading type: %s\n", jsonType.type_id);
                profiles.add(jsonType.profile);
                typeToProfile.put(jsonType.type_id,jsonType.profile);
            }

            JsonTraffic jsonTraffic = jsonFile.traffic.get(0);
            double[] timeSlots = jsonTraffic.getTimeSlots();
            double[] scalingFactors = jsonTraffic.getScalingFactors();


            int cnt = 0;

            // Loading vehicles
            ArrayList<Pair<Double, Double>> points = new ArrayList<>();
            for (JsonVehicle jsonVehicle : jsonFile.vehicles){
                Coordinate startCoordinate = Coordinate.newInstance(jsonVehicle.start_address.lon, jsonVehicle.start_address.lat);
                if (jsonVehicle.end_address==null) {
                    jsonVehicle.end_address = jsonVehicle.start_address;
                }
                System.out.printf("Loading vehicle: %s\n", jsonVehicle.vehicle_id);
                Coordinate arrivalCoordinate = Coordinate.newInstance(jsonVehicle.end_address.lon, jsonVehicle.end_address.lat);
                Location start = Location.Builder.newInstance().setId(jsonVehicle.start_address.location_id).setIndex(cnt).setCoordinate(startCoordinate).setName(jsonVehicle.start_address.name).build();
                lastDepot = start;
                System.out.printf("\tStart: %d\n", cnt);
                points.add(new Pair<>(start.getCoordinate().getX(), start.getCoordinate().getY()));
                cnt++;
                Location arrival = Location.Builder.newInstance().setId(jsonVehicle.end_address.location_id).setIndex(cnt).setCoordinate(arrivalCoordinate).setName(jsonVehicle.end_address.name).build();
                System.out.printf("\tEnd: %d\n", cnt);
                points.add(new Pair<>(arrival.getCoordinate().getX(), arrival.getCoordinate().getY()));

                //VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance(jsonVehicle.type_id).setProfile(typeToProfile.get(jsonVehicle.type_id));
                VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance(jsonVehicle.type_id).setProfile("car");
                int dimIndex = 0;
                for(Integer capacity : typeToCapacities.get(jsonVehicle.type_id)){
                    vehicleTypeBuilder.addCapacityDimension(dimIndex, capacity);
                }
                VehicleType vehicleType = vehicleTypeBuilder.build();

                VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(jsonVehicle.vehicle_id).setStartLocation(start).setEndLocation(arrival);
                if (vehicleType!=null){
                    vehicleBuilder.setType(vehicleType);
                }
                if (jsonVehicle.earliest_start!=null){
                    //Update the vehicles' earliest_start with the scaling factors
                    double tKeep, tAnt, multAct, multAnt, TotalTime;
                    tKeep = timeSlots[1];
                    tAnt = timeSlots[0];
                    multAct = scalingFactors[1];
                    multAnt = scalingFactors[0];
                    TotalTime = timeSlots[0];
                    int j = 1;

                    while (tKeep <= jsonVehicle.earliest_start) {
                        TotalTime = TotalTime + (tKeep - tAnt) / multAnt;
                        j += 1;
                        tAnt = tKeep;
                        multAnt = multAct;
                        if (j == timeSlots.length) {
                            break;
                        }
                        tKeep = timeSlots[j];
                        multAct = scalingFactors[j];
                    }

                    jsonVehicle.earliest_start = (int) Math.ceil(TotalTime + (jsonVehicle.earliest_start - tAnt) / multAnt);
                    vehicleBuilder.setEarliestStart(jsonVehicle.earliest_start);
                }
                vrpBuilder.addVehicle(vehicleBuilder.build());
                cnt++;
            }
            vehicleThreshold = cnt-1;



            // Loading services
            for (JsonService jsonService : jsonFile.services){
                // If the service is a general service
                if (jsonService.type==null || jsonService.type.equals("service")){
                    Coordinate coordinate = Coordinate.newInstance(jsonService.address.lon, jsonService.address.lat);
                    System.out.printf("Loading service %d: %s\n", cnt ,jsonService.id);
                    jsonService.index=cnt;
                    Location location = Location.Builder.newInstance().setName(jsonService.address.name).setId(jsonService.address.location_id).setIndex(cnt).setCoordinate(coordinate).build();
                    points.add(new Pair<>(location.getCoordinate().getX(),location.getCoordinate().getY()));
                    Service.Builder jobBuilder = Service.Builder.newInstance(jsonService.id);
                    if (jsonService.duration!=null){
                        jobBuilder.setServiceTime(jsonService.duration);
                    }
                    if (jsonService.time_windows!=null) {
                        // Modifying Time Windows
                        double tKeep, tAnt, multAct, multAnt, TotalTime;
                        for (JsonService.Window window : jsonService.time_windows) {
                            tKeep = timeSlots[1];
                            tAnt = timeSlots[0];
                            multAct = scalingFactors[1];
                            multAnt = scalingFactors[0];
                            TotalTime = timeSlots[0];
                            int j = 1, c = 0;

                            if (tKeep > window.latest) {
                                window.earliest = (int) Math.ceil(timeSlots[0] + (window.earliest - timeSlots[0]) / multAnt);
                            }

                            while (tKeep <= window.latest) {
                                if (c == 0 & tKeep > window.earliest) {
                                    window.earliest = (int) Math.ceil(TotalTime + (window.earliest - tAnt) / multAnt);
                                    c = 1;
                                }
                                TotalTime = TotalTime + (tKeep - tAnt) / multAnt;
                                j += 1;
                                tAnt = tKeep;
                                multAnt = multAct;
                                if (j == timeSlots.length) {
                                    break;
                                }
                                tKeep = timeSlots[j];
                                multAct = scalingFactors[j];
                            }

                            if (c == 0) {
                                window.earliest = (int) Math.ceil(TotalTime + (window.earliest - tAnt) / multAnt);
                            }

                            window.latest = (int) Math.floor(TotalTime + (window.latest - tAnt) / multAnt);
                            jobBuilder.addTimeWindow(window.earliest, window.latest);
                        }
                    }
                    if (jsonService.size!=null){
                        for (int i=0 ; i<jsonService.size.size() ; i++) jobBuilder.addSizeDimension(i,jsonService.size.get(i));
                    }
                    jobBuilder.setLocation(location);
                    Job job = jobBuilder.build();
                    vrpBuilder.addJob(job);
                    if (jsonService.allowed_vehicles!=null && jsonService.allowed_vehicles.size()>0){
                        this.allowedVehiclesConstraint.addSet(jsonService.id, jsonService.allowed_vehicles);
                    }
                    if (jsonService.disallowed_vehicles!=null && jsonService.disallowed_vehicles.size()>0){
                        this.disallowedVehiclesConstraint.add(jsonService.id, jsonService.disallowed_vehicles);
                    }
                    idsToIndex.put(jsonService.id, cnt);
                    cnt++;
                }
                else {
                    throw new UnknownValueException("Unknown type of service");
                }
            }
            // Fetching distance information from a DistanceFetcher
            DistanceFetcher fetcher;
            switch (costSource) {
                case "local":
                    fetcher = new LocalFetcher(points.size(), profiles, folderName, fileName);
                    break;
                case "graphhopper":
                    fetcher = new GraphhopperFetcher(points,profiles, folderName, fileName, key);
                    break;
                case "ruteo":
                    fetcher = new RuteoFetcher(points,profiles, folderName, fileName);
                    break;
                case "oneAtATime":
                    fetcher = new OneAtATimeFetcher(points,profiles, folderName, fileName);
                    break;
                case "localruteo":
                    fetcher = new LocalRuteoFetcher(points,profiles, folderName, fileName, vehicleThreshold -1 , newService, false );
                    break;
                case "localruteoModify":
                    fetcher = new LocalRuteoFetcher(points,profiles, folderName, fileName, vehicleThreshold -1 , newService, true );
                    break;
                default:
                    throw new UnknownValueException("Cost source is not known.");
            }
            // Adding forcing relation (if needed)
            if (jsonFile.relations!=null){
                fetcher.handleRelations(jsonFile.relations, idsToIndex, vehicleThreshold);
                for (JsonRelation relation : jsonFile.relations){
                    if (relation.vehicle_id!=null){
                        this.allowedVehiclesConstraint.addVehicle(relation.ids.get(0), relation.vehicle_id);
                    }
                }
            }
            // Wraping the problem up
            vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
            vrpBuilder.setRoutingCost(fetcher.fetchDistance());
            problem = vrpBuilder.build();
            this.problem= problem;
            this.fastMatrix = fetcher.getFastMatrix();
            this.objectives= jsonFile.objectives;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

