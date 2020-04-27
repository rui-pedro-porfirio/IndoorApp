package android.example.findlocation.objects.server;

public class ServerWifiData {

    private String name;
    private int rssi;
    private String fingerprint;

    public ServerWifiData (String name, int rssi){
        this.name = name;
        this.rssi = rssi;
        this.fingerprint = "";
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
