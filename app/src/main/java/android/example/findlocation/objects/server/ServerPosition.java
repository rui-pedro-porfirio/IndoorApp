package android.example.findlocation.objects.server;

import java.util.List;
import java.util.Map;

public class ServerPosition {

    private String algorithm;
    private String filter;
    private List<String> dataTypes;
    private Map<String,Integer> aps;


    public ServerPosition(String algorithm,String filter,Map<String,Integer> aps, List<String> dataTypes){
        this.algorithm = algorithm;
        this.filter = filter;
        this.aps = aps;
        this.dataTypes = dataTypes;
    }
}
