package android.example.findlocation.objects;


import org.w3c.dom.CDATASection;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Fingerprint {

    private List<SensorObject> mSensorInformationList;
    private List<BluetoothObject> mBeaconsList;
    private List<WifiObject> mAccessPoints;

    public Fingerprint(List<SensorObject> mSensorInformationList,List<BluetoothObject> mBeaconsList,List<WifiObject> mAccessPoints){
        this.mSensorInformationList = mSensorInformationList;
        this.mBeaconsList = mBeaconsList;
        this.mAccessPoints = mAccessPoints;
    }

    public List<SensorObject> getmSensorInformationList() {
        return mSensorInformationList;
    }

    public List<BluetoothObject> getmBeaconsList() {
        return mBeaconsList;
    }

    public List<WifiObject> getmAccessPoints() {
        return mAccessPoints;
    }
}
