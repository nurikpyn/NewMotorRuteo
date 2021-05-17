package cl.cmm.ruteo;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

class Main {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("us", "US"));
        // Loads default parameters.
        Map<String, String> jarParameters = new HashMap<>();
        jarParameters.put("-sol","BS");
        jarParameters.put("-s", "ruteo");
        jarParameters.put("-k", "No Key");
        jarParameters.put("-p", "300");
        jarParameters.put("-t", "60000");
        jarParameters.put("-i", "2000");
        jarParameters.put("-cn", "");
        jarParameters.put("-inf", "false");
        jarParameters.put("-data-source", "json");
        jarParameters.put("-dly","false");
        jarParameters.put("-depot-service-time", "0");

        // Loads custom parameters
        if (args.length%2==0) {
            for (int i = 1; i <args.length/2  ; i++) {
                jarParameters.put(args[2 * i], args[2 * i + 1]);
            }
            cl.cmm.ruteo.Extra.easySolver(args[0], args[1], jarParameters);
        }
        else{
            throw new cl.cmm.ruteo.UnexpectedArgumentException("Unexpected number of arguments");
        }
        System.out.println("DONE");
    }
}
