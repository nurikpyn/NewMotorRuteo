package cl.cmm.ruteo.jsonProcessing;

import java.util.ArrayList;
import java.util.List;

public class JsonRoute {
    private String vehicle_id;
    private Double transport_time;
    private Double completion_time;
    private Double waiting_time;
    private Double service_duration;
    private Double distance;
    private final List<JsonActivity> activities = new ArrayList<>();

    public void setDistance(Double distance) {
        this.distance = distance;
    }
    public void setTransport_time(Double transport_time) {
        this.transport_time = transport_time;
    }

    public void setWaiting_time(Double waiting_time) {
        this.waiting_time = waiting_time;
    }

    public void setCompletion_time(Double completion_time) {
        this.completion_time = completion_time;
    }

    public void addActivity(JsonActivity anActivity) {
        this.activities.add(anActivity);
    }

    public void setService_duration(Double service_duration) {
        this.service_duration = service_duration;
    }

    public void setVehicle_id(String vehicle_id) {
        this.vehicle_id = vehicle_id;
    }
}
