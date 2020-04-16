package android.example.findlocation.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.example.findlocation.R;
import android.example.findlocation.adapters.BluetoothAdapterRC;
import android.example.findlocation.objects.BluetoothObject;
import android.example.findlocation.objects.Fingerprint;
import android.example.findlocation.objects.SensorObject;
import android.example.findlocation.objects.WifiObject;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.example.findlocation.ui.main.SectionsPagerAdapter;
import android.widget.CheckBox;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class OfflineTabedActivity extends AppCompatActivity {

    private List<String> dataTypes;
    private Map<String, Integer> preferences;
    private RetrieveSensorDataTask downloadSensorData;

    public static final String FINGERPRINT_FILE = "fingeprint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_tabed);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        tabs.getTabAt(0).setIcon(R.drawable.fingerprinticon);
        tabs.getTabAt(1).setIcon(R.drawable.radiomapicon);
        tabs.getTabAt(2).setIcon(R.drawable.preferencesicon);
        dataTypes = new ArrayList<String>();
        preferences = new HashMap<String, Integer>();
        downloadSensorData = new RetrieveSensorDataTask(this);
        downloadSensorData.doInBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadSensorData.destroy();
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();


        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.checkbox_wifi:
                if (checked) {
                    if (!dataTypes.contains("Wi-Fi"))
                        dataTypes.add("Wi-fi");
                }
                else{
                    if(dataTypes.contains("Wi-Fi"))
                        dataTypes.remove("Wi-Fi");
                }
                break;
            case R.id.checkbox_bluetooth:
                if (checked) {
                    if (!dataTypes.contains("Bluetooth"))
                        dataTypes.add("Bluetooth");
                }else{
                    if(dataTypes.contains("Bluetooth"))
                        dataTypes.remove("Bluetooth");
                }
                break;
            case R.id.checkbox_device_sensors:
                if (checked) {
                    if (!dataTypes.contains("DeviceData"))
                        dataTypes.add("DeviceData");
                }else{
                    if(dataTypes.contains("DeviceData"))
                        dataTypes.remove("DeviceData");
                }
                break;
        }
    }

    public void addFingerprintListener(View view){
        if (preferences.size() != 0) {
            int numberOfFingerprints = preferences.get("Number of Fingerprints");
            int interval = preferences.get("Time between Fingerprints");
            for (int i = 0; i < numberOfFingerprints; i++) {
                Toast.makeText(this, "Scanning Fingerprint", Toast.LENGTH_SHORT).show();
                startBackgroundService(); //STARTS FINGERPRINT COLLECTION AND SEND
            }
        } else {
            Toast.makeText(this, "Check preferences", Toast.LENGTH_SHORT).show();
        }
    }

    public void startBackgroundService() {
        downloadSensorData.scanData();
    }

    public void computeFingerprint(Fingerprint fingerprint) {
        //TODO:send fingerprint to radio map tab
        //TODO: write to json file
        try {
            File targetFile = writeToFile(FINGERPRINT_FILE);
            writeJsonStreamSensorData(new FileOutputStream(targetFile, true), fingerprint.getmSensorInformationList());
            writeJsonStreamBLE(new FileOutputStream(targetFile, true), fingerprint.getmBeaconsList());
            writeJsonStreamWiFi(new FileOutputStream(targetFile, true), fingerprint.getmAccessPoints());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: Add HTTP REQUESTS
        //TODO: Create final Toast
        Toast.makeText(this, "Fingerprint Created and Sent to Server", Toast.LENGTH_SHORT).show();
    }


    public void setPreferences(Map<String, Integer> preferences) {
        this.preferences = preferences;
    }

    public Map<String, Integer> getPreferences() {
        return this.preferences;
    }

    public List<String> getSelectedTypes() {
        return this.dataTypes;
    }

    //WRITE TO JSON FILE

    public void writeJsonStreamSensorData(OutputStream out, List<SensorObject> values) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArraySensorData(writer, values);
        writer.close();
    }

    public void writeMessagesArraySensorData(JsonWriter writer, List<SensorObject> values) throws IOException {
        writer.beginObject();
        writer.name("Device Sensors");
        writer.beginArray();
        for (SensorObject sensor : values
        ) {
            writeMessageSensorData(writer, sensor);
        }
        writer.endArray();
        writer.endObject();
    }

    public void writeMessageSensorData(JsonWriter writer, SensorObject sensor) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(sensor.getName());
        writer.name("samples");
        writeListArraySensorData(writer, sensor.getValues());
        writer.endObject();
    }


    public void writeListArraySensorData(JsonWriter writer, float[] scannedValues) throws IOException {
            writer.beginObject();
            writer.name("Sample").value(0);
            writer.name("values");
            writer.beginArray();
            for (Float f : scannedValues
            ) {
                if (!Float.isNaN(f))
                    writer.value(f);
            }
            writer.endArray();
            writer.endObject();

    }

    public void writeJsonStreamBLE(OutputStream out, List<BluetoothObject> values) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArrayBLE(writer, values);
        writer.close();
    }

    public void writeMessagesArrayBLE(JsonWriter writer, List<BluetoothObject> values) throws IOException {
        writer.beginObject();
        writer.name("BLE");
        writer.beginArray();
        for (BluetoothObject sensor : values
        ) {
            writeMessageBLE(writer, sensor);
        }
        writer.endArray();
        writer.endObject();
    }

    public void writeMessageBLE(JsonWriter writer, BluetoothObject sensor) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(sensor.getName());
        writer.name("samples");
        writeListArrayBLE(writer, sensor.getSingleValue());
        writer.endObject();
    }


    public void writeListArrayBLE(JsonWriter writer, Integer scannedValue) throws IOException {
            writer.beginObject();
            writer.name("RSSI").value(scannedValue);
            writer.endObject();
    }

    public void writeJsonStreamWiFi(OutputStream out, List<WifiObject> values) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArrayWiFi(writer, values);
        writer.close();
    }

    public void writeMessagesArrayWiFi(JsonWriter writer, List<WifiObject> values) throws IOException {
        writer.beginObject();
        writer.name("Wi-Fi");
        writer.beginArray();
        for (WifiObject sensor : values
        ) {
            writeMessageWiFi(writer, sensor);
        }
        writer.endArray();
        writer.endObject();
    }

    public void writeMessageWiFi(JsonWriter writer, WifiObject sensor) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(sensor.getName());
        writer.name("samples");
        writeListArrayWiFi(writer, sensor.getSingleValue());
        writer.endObject();
    }


    public void writeListArrayWiFi(JsonWriter writer, Integer scannedValue) throws IOException {
            writer.beginObject();
            writer.name("RSSI").value(scannedValue);
            writer.endObject();
    }

    public File writeToFile(String sFileName) {

        File root = new File(Environment.getExternalStorageDirectory(), "Sensor Data");
        // if external memory exists and folder with name Notes
        if (!root.exists()) {
            root.mkdirs(); // this will create folder.
        }
        File filepath = new File(root, sFileName + ".json");  // file path to save
        if(filepath.exists()) {
            filepath.delete();
            filepath = new File(root, sFileName + ".json");
        }
        return filepath;

    }


    public class RetrieveSensorDataTask implements SensorEventListener, BeaconConsumer {


        private Context mContext;
        private SensorManager mSensorManager;
        private WifiManager wifiManager;
        //iBeacon unique identifier for Alt-Beacon
        private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

        //Sensor Managers
        private BeaconManager beaconManager;
        private List<WifiObject> mAccessPoints;
        private List<BluetoothObject> mBeaconsList;
        private List<SensorObject> mSensorInformationList;


        public RetrieveSensorDataTask(Context context) {
            mContext = context;
        }

        protected void doInBackground() {
            mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);
            mSensorInformationList = new LinkedList<>();
            getAvailableDeviceSensors();

            //BLUETOOTH SENSOR
            mBeaconsList = new LinkedList<>();
            beaconManager = BeaconManager.getInstanceForApplication(mContext);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
            beaconManager.bind(this);

            //WI-FI SENSOR
            mAccessPoints = new LinkedList<>();
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);

            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifiManager.startScan();
        }

        public void scanData(){


            CountDownTimer waitTimer;
            waitTimer = new CountDownTimer(10000, 300) {

                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (!dataTypes.contains("Wi-fi")) {
                        mAccessPoints = new LinkedList<>();
                    }
                    if (!dataTypes.contains("Bluetooth")) {
                        mBeaconsList = new LinkedList<>();
                    }
                    if (!dataTypes.contains("DeviceData")) {
                        mSensorInformationList = new LinkedList<>();
                    }

                    onPostExecute(new Fingerprint(mSensorInformationList, mBeaconsList, mAccessPoints));
                }
            }.start();
        }

        public void getAvailableDeviceSensors() {
            float[] defaultValues = new float[3];
            defaultValues[0] = 0f;
            defaultValues[1] = 0f;
            defaultValues[2] = 0f;
            Sensor orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            mSensorManager.registerListener(this, orientation,
                    SensorManager.SENSOR_DELAY_NORMAL);
            SensorObject sensorInfo = new SensorObject(orientation.getName(), defaultValues);
            mSensorInformationList.add(sensorInfo);
        }


        protected void onPostExecute(Fingerprint fingerprint) {
            //SEND FINGERPRINT
            computeFingerprint(fingerprint);
            System.out.println("Device Data Done");
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensorDetected = event.sensor;
            SensorObject sensorInList = mSensorInformationList.get(0);
            if (sensorDetected.getName().equals(sensorInList.getName())) {
                sensorInList.setValue(event.values);
                sensorInList.setChecked(true);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    scanSuccess();
                }
            }
        };

        private void scanSuccess() {
            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult result : results
            ) {
                int rssi = result.level;
                boolean found = false;
                for (int i = 0; i < mAccessPoints.size(); i++) {
                    WifiObject resultInList = mAccessPoints.get(i);
                    if (resultInList.getName().equals(result.BSSID)) {
                        resultInList.setSingleValue(rssi);
                        found = true;
                        break;
                    }
                }

                if (found == false) {
                    WifiObject ap = new WifiObject(result.BSSID, result.level);
                    mAccessPoints.add(ap);
                }
            }
        }


        @Override
        public void onBeaconServiceConnect() {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.addRangeNotifier(new RangeNotifier() {
                @Override
                public void didRangeBeaconsInRegion(Collection<Beacon> beacons, org.altbeacon.beacon.Region region) {
                    if (beacons.size() > 0) {

                        Beacon beaconScanned = beacons.iterator().next();
                        int rss = beaconScanned.getRssi();
                        boolean found = false;
                        for (int i = 0; i < mBeaconsList.size(); i++) {
                            BluetoothObject beaconfound = mBeaconsList.get(i);
                            if (beaconfound.getName().equals(beaconScanned.getBluetoothAddress())) {
                                beaconfound.setSingleValue(rss);
                                found = true;
                                break;
                            }
                        }

                        if (found == false) {
                            BluetoothObject beacon = new BluetoothObject(beaconScanned.getBluetoothAddress(), rss);
                            mBeaconsList.add(beacon);
                        }
                    }
                }
            });
            try {
                beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            } catch (
                    RemoteException e) {
            }
        }

        @Override
        public Context getApplicationContext() {
            return mContext;
        }

        @Override
        public void unbindService(ServiceConnection serviceConnection) {
            beaconManager.unbind(this);
        }

        @Override
        public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
            return false;
        }

        public void destroy(){
            unregisterReceiver(wifiScanReceiver);
            mSensorManager.unregisterListener(this);
        }

    }
}