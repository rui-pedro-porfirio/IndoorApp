package android.example.findlocation.objects.server;

public class ServerFingerprint {

    private float coordinate_X;
    private float coordinate_Y;

    public ServerFingerprint(float x_coordinate, float y_coordinate){
        this.coordinate_X = x_coordinate;
        this.coordinate_Y = y_coordinate;
    }

    public float getY_coordinate() {
        return coordinate_Y;
    }

    public float getX_coordinate() {
        return coordinate_X;
    }
}
