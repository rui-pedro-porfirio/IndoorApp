package android.example.findlocation.objects.server;

public class ServerDeviceData {

    private final String sensor_type;
    private final float x_value;
    private final float y_value;
    private final float z_value;
    private String fingerprint;

    public ServerDeviceData(String sensor_type, float x_value, float y_value, float z_value) {
        this.sensor_type = sensor_type;
        this.x_value = x_value;
        this.y_value = y_value;
        this.z_value = z_value;
        this.fingerprint = "";
    }

    public void setFingerprintId(String fingerprintId) {
        this.fingerprint = fingerprintId;
    }
}
