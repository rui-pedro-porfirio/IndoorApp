package android.example.findlocation.objects.server;

import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;

import java.util.List;

public class ScanningObject {

    private String username;
    private String accessToken;
    private List<WifiObject> mAccessPoints;
    private List<BluetoothObject> mBeaconsList;
    private List<SensorObject> mSensorInformationList;

    public ScanningObject(String username, String token, List<WifiObject> mAccessPoints, List<BluetoothObject> mBeaconsList, List<SensorObject> mSensorInformationList){
        this.username = username;
        this.accessToken = token;
        this.mAccessPoints = mAccessPoints;
        this.mBeaconsList = mBeaconsList;
        this.mSensorInformationList = mSensorInformationList;
    }

}
