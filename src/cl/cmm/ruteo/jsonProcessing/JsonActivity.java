package cl.cmm.ruteo.jsonProcessing;

public class JsonActivity {
    private String type;
    private String location_id;
    private String id;
    private JsonAddress address;
    private Double arr_time;
    private Double distance;
    private Double driving_time;
    private Double waiting_time;
    private Double end_time;

    public void setWaiting_time(Double waiting_time) {
        this.waiting_time = waiting_time;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setAddress(JsonAddress address) {
        this.address = address;
    }

    public void setArr_time(Double arr_time) {
        this.arr_time = arr_time;
    }

    public void setEnd_time(Double end_time) { this.end_time = end_time; }

    public void setDriving_time(Double driving_time) {
        this.driving_time = driving_time;
    }

    public void setLocation_id(String location_id) {
        this.location_id = location_id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(String id) {
        this.id = id;
    }
}