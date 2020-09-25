package android.example.findlocation.ui.activities.sensors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.example.findlocation.R;
import android.example.findlocation.ui.adapters.BluetoothAdapterRC;
import android.example.findlocation.ui.adapters.SensorAdapter;
import android.example.findlocation.ui.adapters.WiFiAdapter;
import android.example.findlocation.objects.client.SensorObject;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SensorInformationActivity extends AppCompatActivity implements SensorEventListener, BeaconConsumer {

    private long startTimeNs;

    //iBeacon unique identifier for Alt-Beacon
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    //Sensor Managers
    private BeaconManager beaconManager;
    private SensorManager mSensorManager;
    private WifiManager wifiManager;

    //Lists for Recycle Views
    private LinkedList<SensorObject> mSensorInformationList;
    private LinkedList<Beacon> mBeaconsList;
    private List<ScanResult> mAccessPoints;

    //Recycle Views
    private RecyclerView mRecyclerView;
    private RecyclerView bluetoothRecyclerView;
    private RecyclerView wifiRecyclerView;

    //Adapters
    private SensorAdapter mAdapter;
    private BluetoothAdapterRC bluetoothAdapter;
    private WiFiAdapter wifiAdapter;

    private boolean isLongScanning;

    //Map with long scan results
    protected Map<String, List<Integer>> mWiFiScanResults;
    protected Map<String, List<List<Float>>> mDeviceScanResults;
    protected Map<String, List<Integer>> mBluetoothScanResults;

    protected static final String TAG = "MonitoringActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar

        //DEVICE SENSORS
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorInformationList = new LinkedList<>();

        //BLUETOOTH SENSOR
        mBeaconsList = new LinkedList<>();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.bind(this);
        verifyBluetooth();

        startTimeNs = System.nanoTime();
        //WI-FI SENSOR
        mAccessPoints = new LinkedList<>();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiScanReceiver, intentFilter);

        if (ActivityCompat.checkSelfPermission(SensorInformationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE},1);
        }
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            buildAlertMessageNoGps();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
        //LONG SCAN VARIABLES
        isLongScanning = false;
        mWiFiScanResults = new HashMap<>();
        mDeviceScanResults = new HashMap<>();
        mBluetoothScanResults = new HashMap<>();
        handler.post(locationUpdate);

    }

    final Handler handler = new Handler();
    final Runnable locationUpdate = new Runnable() {
        @Override
        public void run() {
            wifiManager.startScan();
            Log.d("START SCAN CALLED", "");
            handler.postDelayed(locationUpdate, 1000);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mSensorInformationList = getAvailableDeviceSensors();

        initDeviceSensorRecycleView();

        initBluetoothSensorRecycleView();

        initWifiSensorRecycleView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    public LinkedList<SensorObject> getAvailableDeviceSensors() {
        float[] defaultValues = new float[3];
        defaultValues[0] = 0f;
        defaultValues[1] = 0f;
        defaultValues[2] = 0f;
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor currentSensor : sensorList) {
            if (currentSensor != null)
                mSensorManager.registerListener(this, currentSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            SensorObject sensorInfo = new SensorObject(currentSensor.getName(), defaultValues);
            mSensorInformationList.add(sensorInfo);
        }
        return mSensorInformationList;
    }

    public void initDeviceSensorRecycleView() {

        mRecyclerView = findViewById(R.id.recyclerview);
        mAdapter = new SensorAdapter(this, mSensorInformationList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void initBluetoothSensorRecycleView() {
        bluetoothRecyclerView = findViewById(R.id.recyclerviewBluetooth);
        bluetoothAdapter = new BluetoothAdapterRC(this, mBeaconsList);
        bluetoothRecyclerView.setAdapter(bluetoothAdapter);
        bluetoothRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void initWifiSensorRecycleView() {
        wifiRecyclerView = findViewById(R.id.recyclerviewWiFi);
        wifiAdapter = new WiFiAdapter(this, mAccessPoints);
        wifiRecyclerView.setAdapter(wifiAdapter);
        wifiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * DEVICE SENSOR METHODS
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensorDetected = event.sensor;
        for (int i = 0; i < mSensorInformationList.size(); i++) {
            SensorObject sensorInList = mSensorInformationList.get(i);
            if (sensorDetected.getName().equals(sensorInList.getName())) {
                sensorInList.setValue(event.values);
                mAdapter.notifyDataSetChanged();
            }
        }

        if (isLongScanning) {
            if (!mDeviceScanResults.containsKey(sensorDetected.getName())) {
                mDeviceScanResults.put(sensorDetected.getName(), new ArrayList<List<Float>>());
            }
            List<Float> valuesToAdd = new ArrayList<Float>();
            if(event.values.length >= 3){
                Float xValue = event.values[0];
                Float yValue = event.values[1];
                Float zValue = event.values[2];
                valuesToAdd.add(xValue);
                valuesToAdd.add(yValue);
                valuesToAdd.add(zValue);
            }
            else if(event.values.length == 2){
                Float xValue = event.values[0];
                Float yValue = event.values[1];
                valuesToAdd.add(xValue);
                valuesToAdd.add(yValue);
            }
            else{
                Float xValue = event.values[0];
                valuesToAdd.add(xValue);

            }
                mDeviceScanResults.get(sensorDetected.getName()).add(valuesToAdd);
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * BLUETOOTH SENSOR MEHTODS
     */

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //finish();
                        //System.exit(0);
                    }
                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    //finish();
                    //System.exit(0);
                }

            });
            builder.show();

        }

    }

    public LinkedList<Beacon> getmBeaconsList() {
        return mBeaconsList;
    }

    public List<ScanResult> getmAccessPoints() {
        return mAccessPoints;
    }

    public LinkedList<SensorObject> getmSensorInformationList() {
        return mSensorInformationList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
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
                        Beacon beaconfound = mBeaconsList.get(i);
                        if (beaconfound.getBluetoothAddress().equals(beaconScanned.getBluetoothAddress())) {
                            beaconfound.setRssi(rss);
                            bluetoothAdapter.notifyDataSetChanged();
                            found = true;
                            break;
                        }
                    }

                    if (found == false) {
                        mBeaconsList.add(beaconScanned);
                        bluetoothAdapter.notifyDataSetChanged();
                    }

                    if (isLongScanning) {
                        if (!mBluetoothScanResults.containsKey(beaconScanned.getBluetoothAddress())) {
                            mBluetoothScanResults.put(beaconScanned.getBluetoothAddress(), new ArrayList<Integer>());
                        }
                        mBluetoothScanResults.get(beaconScanned.getBluetoothAddress()).add(rss);
                    }

                    Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().getDistance() + " meters away.");
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }
    }

    /**
     * Wi-Fi Sensor Methods
     */
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            long elapsedTimeNs = System.nanoTime() - startTimeNs;
            Log.d("WIFI", "Advertising time: " + TimeUnit.MILLISECONDS.convert(elapsedTimeNs, TimeUnit.NANOSECONDS));
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                // scan failure handling
                scanFailure();
            }
        }
    };

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        Log.d("WIFI","Scanned of Wi-Fi Access Points failed. Consider using old scan results but for now just this log");
    }

     private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Location seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult result : results
        ) {
            long elapsedTimeNs = System.nanoTime() - startTimeNs;
            Log.d("WIFI", "Advertising time: " + TimeUnit.MILLISECONDS.convert(elapsedTimeNs, TimeUnit.NANOSECONDS));
            int rssi = result.level;
            boolean found = false;
            for (int i = 0; i < mAccessPoints.size(); i++) {
                ScanResult resultInList = mAccessPoints.get(i);
                if (resultInList.BSSID.equals(result.BSSID)) {
                    resultInList.level = rssi;
                    wifiAdapter.notifyDataSetChanged();
                    found = true;
                    break;
                }
            }

            if (found == false) {
                mAccessPoints.add(result);
                wifiAdapter.notifyDataSetChanged();
            }
            if (isLongScanning) {
                if (!mWiFiScanResults.containsKey(result.BSSID)) {
                    mWiFiScanResults.put(result.BSSID, new ArrayList<Integer>());
                }
                mWiFiScanResults.get(result.BSSID).add(rssi);
            }
        }
    }


    /**
     * SETTINGS MENU
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.longscan:
                computeLongScan();
                return true;
            default:
                //Do nothing
        }
        return super.onOptionsItemSelected(item);
    }

    public void computeLongScan() {
        Toast.makeText(getApplicationContext(), "Scan started.", Toast.LENGTH_SHORT).show();

        isLongScanning = true;
        CountDownTimer waitTimer;
        waitTimer = new CountDownTimer(10000, 300) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if(mBluetoothScanResults.size() == 0){
                    for(Beacon beacon: mBeaconsList){
                        mBluetoothScanResults.put(beacon.getBluetoothAddress(),new LinkedList<Integer>());
                        mBluetoothScanResults.get(beacon.getBluetoothAddress()).add(beacon.getRssi());
                    }
                }
                if(mWiFiScanResults.size() == 0){
                    for(ScanResult accesspoint: mAccessPoints){
                        mWiFiScanResults.put(accesspoint.BSSID,new LinkedList<Integer>());
                        mWiFiScanResults.get(accesspoint.BSSID).add(accesspoint.level);
                    }
                }
                if(mDeviceScanResults.size() == 0){
                    for(SensorObject sensor:mSensorInformationList){
                        mDeviceScanResults.put(sensor.getName(),new ArrayList<List<Float>>());
                        Float xValue = sensor.getX_value();
                        Float yValue = sensor.getY_value();
                        Float zValue = sensor.getZ_value();
                        List<Float> valuesToAdd = new ArrayList<Float>(3);
                        valuesToAdd.add(xValue);
                        valuesToAdd.add(yValue);
                        valuesToAdd.add(zValue);
                        mDeviceScanResults.get(sensor.getName()).add(valuesToAdd);
                    }
                }
                isLongScanning = false;
                Toast.makeText(getApplicationContext(), "Scan ended", Toast.LENGTH_SHORT).show();
                //Populate the graphic with results;
                //SEND RAW DATA TO FOURTH ACTIVITY TO PROCESS THE GRAPHICAL REPRESENTATION OF THE DATA
                Intent dataIntent = new Intent(getApplicationContext(), GraphicalSensorInformationActivity.class);
                dataIntent.putExtra("Device Data",(Serializable)mDeviceScanResults);
                dataIntent.putExtra("Bluetooth Data",(Serializable)mBluetoothScanResults);
                dataIntent.putExtra("WiFi Data",(Serializable)mWiFiScanResults);
                dataIntent.putExtra("Type","Scan");
                startActivity(dataIntent);
            }
        }.start();
    }
}

