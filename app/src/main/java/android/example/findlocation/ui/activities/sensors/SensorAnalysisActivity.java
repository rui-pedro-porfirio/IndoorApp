package android.example.findlocation.ui.activities.sensors;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.example.findlocation.BuildConfig;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;
import android.example.findlocation.ui.adapters.BluetoothAdapterRC;
import android.example.findlocation.ui.adapters.SensorAdapter;
import android.example.findlocation.ui.adapters.WiFiAdapter;
import android.example.findlocation.ui.common.BluetoothVerifier;
import android.example.findlocation.ui.common.DevicePermissions;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

public class SensorAnalysisActivity extends AppCompatActivity {

    private static final String TAG = SensorAnalysisActivity.class.getSimpleName();

    //iBeacon unique identifier for Alt-Beacon
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    //Sensor Managers
    private BeaconManager beaconManager;
    private SensorManager mSensorManager;
    private WifiManager wifiManager;

    //Lists for Recycle Views
    private List<WifiObject> mAccessPointsList;
    private List<BluetoothObject> mBeaconsList;
    private List<SensorObject> mSensorInformationList;

    //Adapters
    private SensorAdapter mDeviceSensorAdapter;
    private BluetoothAdapterRC mBluetoothAdapter;
    private WiFiAdapter mWifiAdapter;

    private DevicePermissions mDevicePermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_analysis);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar
        verifyBluetooth();
        requestPermissions();
        startBackgroundScanningService();
    }


    protected void requestPermissions() {
        mDevicePermissions = new DevicePermissions(this);
        Log.i(TAG, "Requesting Permissions to run in background.");
        mDevicePermissions.requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mDevicePermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void verifyBluetooth() {
        Log.i(TAG, "Verifying Bluetooth's state.");
        BluetoothVerifier mBluetoothVerifier = new BluetoothVerifier(this);
        mBluetoothVerifier.verifyBluetooth();
    }

    protected void startBackgroundScanningService() {
        Log.i(TAG, "Starting Sensor Analysis Background Service");
        new ScanningTask(this).execute();
    }

    private class ScanningTask extends AsyncTask<Void, Void, String> implements SensorEventListener, BeaconConsumer {

        final Handler handler = new Handler();
        final Runnable locationUpdate = new Runnable() {
            @Override
            public void run() {
                wifiManager.startScan();
                handler.postDelayed(locationUpdate, 3000);
            }
        };
        private final String TAG = ScanningTask.class.getSimpleName();
        private final Activity mContext;
        private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
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

        public ScanningTask(Activity mContext) {
            this.mContext = mContext;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(Void... voids) {
            Log.i(TAG, "Activating Sensor Scan");
            mSensorInformationList = new ArrayList<>();
            mBeaconsList = new ArrayList<>();
            mAccessPointsList = new ArrayList<>();
            initializeRecyclerViews();
            activateSensorScan();
            return "";
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
        }

        protected void activateSensorScan() {
            initializeDeviceSensor();
            initializeBluetoothSensor();
            initializeWifiSensor();
        }

        private void initializeDeviceSensor() {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            mSensorInformationList = listenForDeviceSensors();
            Log.i(TAG, "Successfully initialized device sensor information");
        }

        private List<SensorObject> listenForDeviceSensors() {
            float[] mDefaultValues = new float[3];
            mDefaultValues[0] = 0f;
            mDefaultValues[1] = 0f;
            mDefaultValues[2] = 0f;
            List<Sensor> mAvailableSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor mSensor : mAvailableSensors) {
                if (mSensor != null)
                    mSensorManager.registerListener(this, mSensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                SensorObject mNewSensor = new SensorObject(mSensor.getName(), mDefaultValues);
                mSensorInformationList.add(mNewSensor);
                Log.i(TAG, "Added Sensor information to list.");
            }
            return mSensorInformationList;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            try {
                Sensor mSensorDetected = event.sensor;
                for (SensorObject mKnownSensor : mSensorInformationList) {
                    if (mSensorDetected.getName().equals(mKnownSensor.getName())) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "New values for orientation: " + Arrays.toString(event.values));
                        mKnownSensor.setValue(event.values);
                        mDeviceSensorAdapter.notifyDataSetChanged();
                    }
                }
            } catch (ConcurrentModificationException e) {
                Log.e(TAG, "Concurrency problem");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        private void initializeBluetoothSensor() {
            beaconManager = BeaconManager.getInstanceForApplication(mContext);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
            setBeaconScanningSettings();
            beaconManager.bind(this);
        }

        private void setBeaconScanningSettings() {
            beaconManager.setEnableScheduledScanJobs(false);
            beaconManager.setBackgroundMode(false);
            beaconManager.setForegroundScanPeriod(150);
            beaconManager.setForegroundBetweenScanPeriod(0);
            beaconManager.setBackgroundScanPeriod(150);
            beaconManager.setBackgroundBetweenScanPeriod(0);
            Log.i(TAG, "Successfully set Alt-Beacon settings.");
        }

        @Override
        public void onBeaconServiceConnect() {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.addRangeNotifier(new RangeNotifier() {
                @Override
                public void didRangeBeaconsInRegion(Collection<Beacon> beacons, org.altbeacon.beacon.Region region) {
                    for (Beacon mBeaconScanned : beacons) {

                        int mRssi = mBeaconScanned.getRssi();
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "New values for beacon: " + mBeaconScanned.getBluetoothAddress() + " | RSSI: " + mRssi);
                        boolean mBeaconExists = false;
                        for (int i = 0; i < mBeaconsList.size(); i++) {
                            BluetoothObject mKnownBeacon = mBeaconsList.get(i);
                            if (mKnownBeacon.getName().equals(mBeaconScanned.getBluetoothAddress())) {
                                mKnownBeacon.setSingleValue(mRssi);
                                mBluetoothAdapter.notifyDataSetChanged();
                                mBeaconExists = true;
                                break;
                            }
                        }

                        if (!mBeaconExists) {
                            BluetoothObject mNewBeaconFound = new BluetoothObject(mBeaconScanned.getBluetoothAddress(), mRssi);
                            mBeaconsList.add(mNewBeaconFound);
                            mBluetoothAdapter.notifyDataSetChanged();
                            Log.i(TAG, "Added beacon " + mNewBeaconFound.getName() + " to the known list.");
                        }
                    }
                }
            });
            try {
                beaconManager.startRangingBeaconsInRegion(new Region("uniqueIdRegion", null, null, null));
            } catch (
                    RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Context getApplicationContext() {
            return mContext.getApplicationContext();
        }

        @Override
        public void unbindService(ServiceConnection serviceConnection) {
            mContext.unbindService(serviceConnection);
        }

        @Override
        public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
            return mContext.bindService(intent, serviceConnection, i);
        }

        private void initializeWifiSensor() {
            wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(wifiScanReceiver, intentFilter);
            handler.post(locationUpdate);
            Log.i(TAG, "Successfully initialized wifi settings for scanning.");
        }

        private void scanFailure() {
            // handle failure: new scan did NOT succeed
            // consider using old scan results: these are the OLD results!
            Log.e(TAG, "Scanned of Wi-Fi Access Points failed. Consider using old scan results but for now just this log");
        }

        private void scanSuccess() {
            List<ScanResult> mAvailableResults = wifiManager.getScanResults();
            for (ScanResult mScanResult : mAvailableResults
            ) {
                int mRssi = mScanResult.level;
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "New values for access point: " + mScanResult.BSSID + " | RSSI: " + mRssi);
                boolean mAccessPointExists = false;
                for (int i = 0; i < mAccessPointsList.size(); i++) {
                    WifiObject mKnownAccessPoint = mAccessPointsList.get(i);
                    if (mKnownAccessPoint.getName().equals(mScanResult.BSSID)) {
                        mKnownAccessPoint.setSingleValue(mRssi);
                        mWifiAdapter.notifyDataSetChanged();
                        mAccessPointExists = true;
                        break;
                    }
                }

                if (!mAccessPointExists) {
                    WifiObject mNewAccessPointFound = new WifiObject(mScanResult.BSSID, mRssi);
                    Log.i(TAG, "Added access point " + mNewAccessPointFound.getName() + " to the known list.");
                    mWifiAdapter.notifyDataSetChanged();
                    mAccessPointsList.add(mNewAccessPointFound);
                }
            }
        }

        private void initializeRecyclerViews() {
            Log.i(TAG, "Starting Recycler Views");
            initDeviceSensorRecycleView();
            initBluetoothSensorRecycleView();
            initWifiSensorRecycleView();
        }


        public void initDeviceSensorRecycleView() {
            //Recycle Views
            RecyclerView mDeviceSensorsRecyclerView = findViewById(R.id.recycler_deviceRecyclerView);
            mDeviceSensorAdapter = new SensorAdapter(mContext, mSensorInformationList);
            mDeviceSensorsRecyclerView.setAdapter(mDeviceSensorAdapter);
            mDeviceSensorsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            Log.i(TAG, "Successfully set device sensor recycler view.");
        }

        public void initBluetoothSensorRecycleView() {
            RecyclerView mBluetoothRecyclerView = findViewById(R.id.recycler_bluetoothRecyclerView);
            mBluetoothAdapter = new BluetoothAdapterRC(mContext, mBeaconsList);
            mBluetoothRecyclerView.setAdapter(mBluetoothAdapter);
            mBluetoothRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            Log.i(TAG, "Successfully set BLE recycler view.");
        }

        public void initWifiSensorRecycleView() {
            RecyclerView mWifiRecyclerView = findViewById(R.id.recycler_wifiRecyclerView);
            mWifiAdapter = new WiFiAdapter(mContext, mAccessPointsList);
            mWifiRecyclerView.setAdapter(mWifiAdapter);
            mWifiRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            Log.i(TAG, "Successfully set Wi-Fi recycler view.");
        }
    }
}

