package android.example.findlocation.objects.client;


import java.util.ArrayList;
import java.util.List;

public class Fingerprint {

    private final List<SensorObject> mSensorInformationList;
    private final List<BluetoothObject> mBeaconsList;
    private final List<WifiObject> mAccessPoints;
    private float x_coordinate;
    private float y_coordinate;
    private String zone;

    public Fingerprint(float x_coordinate, float y_coordinate, String zone, List<SensorObject> mSensorInformationList, List<BluetoothObject> mBeaconsList, List<WifiObject> mAccessPoints) {
        this.mSensorInformationList = mSensorInformationList;
        this.mBeaconsList = mBeaconsList;
        this.mAccessPoints = mAccessPoints;
        this.y_coordinate = y_coordinate;
        this.x_coordinate = x_coordinate;
        this.zone = zone;
    }

    public Fingerprint(List<SensorObject> mSensorInformationList, List<BluetoothObject> mBeaconsList, List<WifiObject> mAccessPoints) {
        this.mSensorInformationList = mSensorInformationList;
        this.mBeaconsList = mBeaconsList;
        this.mAccessPoints = mAccessPoints;
        this.y_coordinate = 0;
        this.x_coordinate = 0;
        this.zone = "None";
    }

    public Fingerprint() {
        mSensorInformationList = new ArrayList<>();
        mBeaconsList = new ArrayList<>();
        mAccessPoints = new ArrayList<>();
        this.x_coordinate = 0f;
        this.y_coordinate = 0f;
        this.zone = "None";
    }

    public List<SensorObject> getmSensorInformationList() {
        return mSensorInformationList;
    }

    public void setmSensorInformationList(List<SensorObject> mSensorInformationList) {
        for (SensorObject sensor : mSensorInformationList) {
            SensorObject newSensor = new SensorObject(sensor.getName(), sensor.getValues());
            this.mSensorInformationList.add(newSensor);
        }
    }

    public List<BluetoothObject> getmBeaconsList() {
        return mBeaconsList;
    }

    public void setmBeaconsList(List<BluetoothObject> mBeaconsList) {
        for (BluetoothObject beacon : mBeaconsList) {
            BluetoothObject newBeacon = new BluetoothObject(beacon.getName(), beacon.getSingleValue());
            this.mBeaconsList.add(newBeacon);
        }
    }

    public List<WifiObject> getmAccessPoints() {
        return mAccessPoints;
    }

    public void setmAccessPoints(List<WifiObject> mAccessPoints) {
        for (WifiObject ap : mAccessPoints) {
            WifiObject newAP = new WifiObject(ap.getName(), ap.getSingleValue());
            this.mAccessPoints.add(newAP);
        }
    }

    public float getX_coordinate() {
        return x_coordinate;
    }

    public void setX_coordinate(float x_coordinate) {
        this.x_coordinate = x_coordinate;
    }

    public float getY_coordinate() {
        return y_coordinate;
    }

    public void setY_coordinate(float y_coordinate) {
        this.y_coordinate = y_coordinate;
    }

    public String getZone() {
        return this.zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
