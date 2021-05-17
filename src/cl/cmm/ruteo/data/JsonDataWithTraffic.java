package cl.cmm.ruteo.data;

import cl.cmm.ruteo.UnknownValueException;
import cl.cmm.ruteo.distanceFetcher.*;
import cl.cmm.ruteo.jsonProcessing.*;
import cl.cmm.ruteo.util.Pair;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.*;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;

import java.util.*;

public class JsonDataWithTraffic extends ProblemData {


    /**
     * Constructs problemData (problem, objectives, transportCosts, some constraints etc...) from a Json file.
     * @param jsonFile contains all the data from the Json file.
     * @param folderName is the folder where the Json file is located.
     * @param fileName is the name of the output file. Needed to save the distance information from fetchers.
     * @param costSource is the source of the distance/time information.
     * @param key is the key that (if needed) allows access to the costSource.
     */
    public JsonDataWithTraffic(JsonFile jsonFile, String folderName, String fileName, String costSource, String key, double newService, double[] timeSlots, double[] scalingFactors){
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
                    if (jsonService.time_windows!=null){
                        for (JsonService.Window window :jsonService.time_windows){
                            jobBuilder.addTimeWindow(window.earliest,window.latest);
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
                // If the service is a delivery
                else if (jsonService.type.equals("delivery")){
                    Coordinate coordinate = Coordinate.newInstance(jsonService.address.lon, jsonService.address.lat);
                    System.out.printf("Loading delivery %d: %s\n", cnt ,jsonService.id);
                    jsonService.index=cnt;
                    Location location = Location.Builder.newInstance().setId(jsonService.address.location_id).setIndex(cnt).setCoordinate(coordinate).build();
                    points.add(new Pair<>(location.getCoordinate().getX(),location.getCoordinate().getY()));
                    Delivery.Builder jobBuilder = Delivery.Builder.newInstance(jsonService.id);
                    if (jsonService.duration!=null){
                        jobBuilder.setServiceTime(jsonService.duration);
                    }
                    if (jsonService.time_windows!=null){
                        for (JsonService.Window window :jsonService.time_windows){
                            jobBuilder.addTimeWindow(window.earliest,window.latest);
                        }
                    }
                    if (jsonService.size!=null){
                        for (int i=0 ; i<jsonService.size.size() ; i++){
                            jobBuilder.addSizeDimension(i,jsonService.size.get(i));
                        }
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
                // If the service is a pickup
                else if (jsonService.type.equals("pickup")){
                    Coordinate coordinate = Coordinate.newInstance(jsonService.address.lon, jsonService.address.lat);
                    System.out.printf("Loading pickup %d: %s\n", cnt ,jsonService.id);
                    jsonService.index=cnt;
                    Location location = Location.Builder.newInstance().setId(jsonService.address.location_id).setIndex(cnt).setCoordinate(coordinate).build();
                    points.add(new Pair<>(location.getCoordinate().getX(),location.getCoordinate().getY()));
                    Pickup.Builder jobBuilder = Pickup.Builder.newInstance(jsonService.id);
                    if (jsonService.duration!=null){
                        jobBuilder.setServiceTime(jsonService.duration);
                    }
                    if (jsonService.time_windows!=null){
                        for (JsonService.Window window :jsonService.time_windows){
                            jobBuilder.addTimeWindow(window.earliest,window.latest);
                        }
                    }
                    if (jsonService.size!=null){
                        for (int i=0 ; i<jsonService.size.size() ; i++){
                            jobBuilder.addSizeDimension(i,jsonService.size.get(i));
                        }
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
                // If the service is a shipment
                else if (jsonService.type.equals("shipment")){
                    System.out.printf("Loading shipment %d: %s\n\tWARNING: Shipments only allow for one depot currently.\n", cnt ,jsonService.id);
                    jsonService.index=cnt;
                    //Pickup Location
                    Coordinate coordinate = Coordinate.newInstance(jsonService.pickup.address.lon, jsonService.pickup.address.lat);
                    Location location = Location.Builder.newInstance().setId(jsonService.pickup.address.location_id).setIndex(cnt).setCoordinate(coordinate).build();
                    points.add(new Pair<>(location.getCoordinate().getX(),location.getCoordinate().getY()));
                    Shipment.Builder jobBuilder = Shipment.Builder.newInstance(jsonService.id);
                    if (jsonService.pickup.duration!=null){
                        jobBuilder.setDeliveryServiceTime(jsonService.pickup.duration);
                    }
                    if (jsonService.pickup.time_windows!=null){
                        for (JsonService.Window window :jsonService.pickup.time_windows){
                            TimeWindow timeWindow = new TimeWindow(window.earliest,window.latest);
                            jobBuilder.setDeliveryTimeWindow(timeWindow);
                        }
                    }
                    if (jsonService.size!=null){
                        for (int i=0 ; i<jsonService.size.size() ; i++){
                            jobBuilder.addSizeDimension(i,jsonService.size.get(i));
                        }
                    }
                    //Delivery Location
                    coordinate = Coordinate.newInstance(jsonService.delivery.address.lon, jsonService.delivery.address.lat);
                    location = Location.Builder.newInstance().setId(jsonService.pickup.address.location_id).setIndex(cnt).setCoordinate(coordinate).build();
                    points.add(new Pair<>(location.getCoordinate().getX(),location.getCoordinate().getY()));
                    jobBuilder = Shipment.Builder.newInstance(jsonService.id);
                    if (jsonService.pickup.duration!=null){
                        jobBuilder.setDeliveryServiceTime(jsonService.delivery.duration);
                    }
                    if (jsonService.pickup.time_windows!=null){
                        for (JsonService.Window window :jsonService.delivery.time_windows){
                            TimeWindow timeWindow = new TimeWindow(window.earliest,window.latest);
                            jobBuilder.setDeliveryTimeWindow(timeWindow);
                        }
                    }
                    jobBuilder.setPickupLocation(location);
                    jobBuilder.setDeliveryLocation(location);
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
            DistanceFetcherWithTraffic fetcher;

            fetcher = new RuteoFetcherWithTraffic(points,profiles, folderName, fileName, timeSlots, scalingFactors);

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

