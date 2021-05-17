package ruteo.jsonProcessing;

import java.util.ArrayList;

public class JsonUnassigned {
    class JsonDetail{
        final String id;
        final String code;
        final String reason;
        JsonDetail(String id, String code, String reason){
            this.id=id;
            this.code=code;
            this.reason=reason;
        }
    }
    private final ArrayList<JsonDetail> details = new ArrayList<>();

    public void setDetail(String id, String code, String reason) {
        this.details.add(new JsonDetail(id, code, reason));
    }
}
