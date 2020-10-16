package android.example.findlocation.objects.server;

import java.util.List;
import java.util.Map;

public class ServerPosition {

    private final String algorithm;
    private final String filter;
    private final List<String> dataTypes;
    private final Map<String, Integer> aps;
    private final Map<String, Integer> beacons;
    private final Map<String, float[]> deviceData;

    public ServerPosition(String algorithm, String filter, Map<String, Integer> aps, Map<String, Integer> beacons, Map<String, float[]> deviceData, List<String> dataTypes) {
        this.algorithm = algorithm;
        this.filter = filter;
        this.aps = aps;
        this.dataTypes = dataTypes;
        this.beacons = beacons;
        this.deviceData = deviceData;
    }
}
