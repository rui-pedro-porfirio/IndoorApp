package android.example.findlocation.objects.server;

public class ServerFingerprint {

    private final float coordinate_X;
    private final float coordinate_Y;
    private final String zone;

    public ServerFingerprint(float x_coordinate, float y_coordinate, String zone) {
        this.coordinate_X = x_coordinate;
        this.coordinate_Y = y_coordinate;
        this.zone = zone;
    }

    public float getY_coordinate() {
        return coordinate_Y;
    }

    public float getX_coordinate() {
        return coordinate_X;
    }

    public String getZone() {
        return zone;
    }
}
