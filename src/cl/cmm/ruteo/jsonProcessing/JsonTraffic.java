package cl.cmm.ruteo.jsonProcessing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;

public class JsonTraffic {
    public String times;
    public String factors;

    public static JsonTraffic fromJson(FileReader in) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(in, JsonTraffic.class);
    }


    public double[] getScalingFactors() {
            String[] sf = factors.split(",");
            double[] scalingFactors = new double[sf.length];
            for (int i = 0; i < sf.length; i++) {
                scalingFactors[i] = Double.parseDouble(sf[i]);
            }
            return scalingFactors;
        }


    public double[] getTimeSlots() {
            String[] ts = times.split(",");
            double[] timeSlots = new double[ts.length];
            for (int i = 0; i < ts.length; i++) {
                timeSlots[i] = Double.parseDouble(ts[i]);
            }
            return timeSlots;
        }
    }

