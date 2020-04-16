package android.example.findlocation.objects;


import org.w3c.dom.CDATASection;

import java.util.ArrayList;
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

    public Fingerprint(){
        mSensorInformationList = new ArrayList<>();
        mBeaconsList = new ArrayList<>();
        mAccessPoints = new ArrayList<>();
    }

    public Fingerprint(Fingerprint other){
        this.mSensorInformationList = other.mSensorInformationList;
        this.mBeaconsList = other.mBeaconsList;
        this.mAccessPoints = other.mAccessPoints;
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

    public void setmAccessPoints(List<WifiObject> mAccessPoints) {
        for(WifiObject ap: mAccessPoints){
            WifiObject newAP = new WifiObject(ap.getName(),ap.getSingleValue());
            this.mAccessPoints.add(newAP);
        }
    }

    public void setmBeaconsList(List<BluetoothObject> mBeaconsList) {
        for(BluetoothObject beacon: mBeaconsList){
            BluetoothObject newBeacon = new BluetoothObject(beacon.getName(),beacon.getSingleValue());
            this.mBeaconsList.add(newBeacon);
        }
    }

    public void setmSensorInformationList(List<SensorObject> mSensorInformationList) {
        for(SensorObject sensor: mSensorInformationList){
            SensorObject newSensor = new SensorObject(sensor.getName(),sensor.getValues());
            this.mSensorInformationList.add(newSensor);
        }
    }
}
