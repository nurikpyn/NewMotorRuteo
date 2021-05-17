package ruteo.jsonProcessing;

import java.util.ArrayList;

public class JsonSolution{
    private Long processing_time = (long) 0;
    public void setProcessing_Time(Long processing_time) {
        this.processing_time = processing_time;
    }
    private Double costs;
    private Double distance;
    private Double transport_time;
    private Double completion_time;
    private Double max_operation_time;
    private Double waiting_time;
    private Integer no_vehicles;
    private Integer no_unassigned;
    private Double service_time;
    private final ArrayList<JsonRoute> routes = new ArrayList<>();
    private final JsonUnassigned unassigned = new JsonUnassigned();

    public void setService_time(Double service_time) {
        this.service_time = service_time;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setCompletion_time(Double completion_time) {
        this.completion_time = completion_time;
    }

    public void setCosts(Double costs) {
        this.costs = costs;
    }

    public void setMax_operation_time(Double max_operation_time) {
        this.max_operation_time = max_operation_time;
    }

    public void setNo_unassigned(Integer no_unassigned) {
        this.no_unassigned = no_unassigned;
    }

    public void setNo_vehicles(Integer no_vehicles) {
        this.no_vehicles = no_vehicles;
    }

    public void addRoute(JsonRoute aRoute) {
        this.routes.add(aRoute);
    }

    public void setTransport_time(Double transport_time) {
        this.transport_time = transport_time;
    }

    public void setWaiting_time(Double waiting_time) {
        this.waiting_time = waiting_time;
    }

    public void setUnassignedService(String id, String code, String reason) {
        this.unassigned.setDetail(id,code,reason);
    }
}
