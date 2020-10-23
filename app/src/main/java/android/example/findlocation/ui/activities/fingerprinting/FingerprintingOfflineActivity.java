package android.example.findlocation.ui.activities.fingerprinting;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.Fingerprint;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;
import android.example.findlocation.objects.server.ServerBluetoothData;
import android.example.findlocation.objects.server.ServerDeviceData;
import android.example.findlocation.objects.server.ServerFingerprint;
import android.example.findlocation.objects.server.ServerWifiData;
import android.example.findlocation.ui.adapters.FingerprintAdapter;
import android.example.findlocation.ui.adapters.SectionsPagerAdapter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FingerprintingOfflineActivity extends AppCompatActivity implements SensorEventListener, BeaconConsumer {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String FINGERPRINT_FILE = "radio_map";
    // OVERALL CONSTANTS
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String SERVER_ADDRESS_LOCAL = "http://192.168.42.55:8000/";
    private static final String SERVER_ADDRESS_HEROKU = "http://indoorlocationapp.herokuapp.com/";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;

    // LOG RELATED CONSTANTS
    private static final String TAG = "TIMER";
    private static final String BLE = "BEACON";
    private static final String LOG = "LOG";
    private static final String WIFI = "WIFI";
    private static final String SMARTPHONE_SENSOR = "SMARTPHONE_SENSOR";
    final Handler handler = new Handler();
    // SMARTPHONE SENSOR RELATED
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    final Runnable locationUpdate = new Runnable() {
        @Override
        public void run() {
            wifiManager.startScan();
            Log.d("START SCAN CALLED", "");
            handler.postDelayed(locationUpdate, 1000);
        }
    };
    // HTTP CONNECTION
    private boolean isScanning;
    private OkHttpClient client;
    //SENSOR MANAGERS
    private SensorManager mSensorManager;
    private WifiManager wifiManager;
    private final float[] orientationAngles = new float[3];
    private BeaconManager beaconManager;
    //SENSOR DATA STRUCTURES
    private List<WifiObject> mAccessPoints;
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
    private List<BluetoothObject> mBeaconsList;
    private List<SensorObject> mSensorInformationList;
    //DATA STORAGE VARIABLES
    private File targetFile;
    private Writer writer;
    // FINGERPRINT INFORMATION
    private List<Fingerprint> fingerprints;
    private String fingerprintId;
    private int numberOfFingerprints;
    private int interval;
    private String zoneClassifier;
    // UI RELATED
    private RecyclerView mFingerprintRecyclerView;
    private FingerprintAdapter mFingerprintAdapter;
    // VARIABLES DEPENDING ON OTHER FRAGMENTS/ACTIVITIES
    private List<String> dataTypes;
    private Map<String, Float> preferences;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_tabed);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //INITIALIZATION OF TABS ADAPTER
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        tabs.getTabAt(0).setIcon(R.drawable.fingerprinticon);
        tabs.getTabAt(1).setIcon(R.drawable.radiomapicon);
        tabs.getTabAt(2).setIcon(R.drawable.preferencesicon);

        // OVERALL INITIALIZATION OF VARIABLES
        zoneClassifier = null;
        client = new OkHttpClient();
        dataTypes = new ArrayList<String>();
        preferences = new HashMap<String, Float>();
        fingerprints = new ArrayList<>();
        fingerprintId = "";
        interval = 0;
        numberOfFingerprints = 0;
        isScanning = false;

        //FILE STORAGE INITIALIZATION
        targetFile = writeToFile(FINGERPRINT_FILE);
        try {
            writer = new FileWriter(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // SENSOR RELATED INITIALIZATION
        verifyBluetooth();
        activateSensorScan();
        requestPermissions();
    }

    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("This app needs background location access");
                        builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @TargetApi(23)
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        PERMISSION_REQUEST_BACKGROUND_LOCATION);
                            }

                        });
                        builder.show();
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Functionality limited");
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }

                        });
                        builder.show();
                    }

                }
            } else {
                if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "fine location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
            case PERMISSION_REQUEST_BACKGROUND_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "background location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void populateRecycleView(View view) {

        mFingerprintRecyclerView = view.findViewById(R.id.recyclerViewFingerprint);
        mFingerprintAdapter = new FingerprintAdapter(this, fingerprints);
        mFingerprintRecyclerView.setAdapter(mFingerprintAdapter);
        mFingerprintRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (beaconManager != null)
            beaconManager.unbind(this);
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException ex) {

        }

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
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
                } else {
                    dataTypes.remove("Wi-Fi");
                }
                break;
            case R.id.checkbox_bluetooth:
                if (checked) {
                    if (!dataTypes.contains("Bluetooth"))
                        dataTypes.add("Bluetooth");
                } else {
                    dataTypes.remove("Bluetooth");
                }
                break;
            case R.id.checkbox_device_sensors:
                if (checked) {
                    if (!dataTypes.contains("DeviceData"))
                        dataTypes.add("DeviceData");
                } else {
                    dataTypes.remove("DeviceData");
                }
                break;
        }
    }

    public void addFingerprintListener(View view) throws InterruptedException {

        if (preferences.size() != 0) {
            numberOfFingerprints = Math.round(preferences.get("Number of Fingerprints"));
            interval = Math.round(preferences.get("Time between Fingerprints")) * 1000;
            startBackgroundService(); //STARTS FINGERPRINT COLLECTION AND SEND
        } else {
            Toast.makeText(this, "Check preferences", Toast.LENGTH_SHORT).show();
        }
    }

    public void startBackgroundService() {

        int sentFingerprints = fingerprints.size();
        if (sentFingerprints < numberOfFingerprints) {
            if (sentFingerprints != 0) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Toast.makeText(this, "Scanning Fingerprint", Toast.LENGTH_SHORT).show();

            scanData();
        } else {
            Toast.makeText(this, "Finished Scanning", Toast.LENGTH_SHORT).show();
            //Gson gson = new Gson();
            //gson.toJson(fingerprints, writer);
            //requestCSVFile();
        }
    }

    public void requestCSVFile() {
        Gson gson = new Gson();
        String jsonString = gson.toJson("csv");
        Toast.makeText(this, "Server creating CSV file with data", Toast.LENGTH_SHORT).show();
        new SendHTTPRequest(jsonString).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void computeFingerprint(Fingerprint fingerprint) {
        Fingerprint newFingerprint = new Fingerprint();
        newFingerprint.setZone(fingerprint.getZone());
        newFingerprint.setX_coordinate(fingerprint.getX_coordinate());
        newFingerprint.setY_coordinate(fingerprint.getY_coordinate());
        newFingerprint.setmAccessPoints(fingerprint.getmAccessPoints());
        newFingerprint.setmBeaconsList(fingerprint.getmBeaconsList());
        newFingerprint.setmSensorInformationList(fingerprint.getmSensorInformationList());
        fingerprints.add(newFingerprint);
        ServerFingerprint fingerprintToSend = new ServerFingerprint(newFingerprint.getX_coordinate(), newFingerprint.getY_coordinate(), newFingerprint.getZone());
        mFingerprintAdapter.notifyDataSetChanged();
        sendFingerprintHTTPRequest(fingerprintToSend);
        for (SensorObject deviceSensorScanned : fingerprint.getmSensorInformationList()) {
            ServerDeviceData serverDeviceData = new ServerDeviceData(deviceSensorScanned.getName(), deviceSensorScanned.getX_value(), deviceSensorScanned.getY_value(), deviceSensorScanned.getZ_value());
            sendDeviceHTTPRequest(serverDeviceData);
        }
        for (WifiObject accessPoint : fingerprint.getmAccessPoints()) {
            ServerWifiData serverWifiData = new ServerWifiData(accessPoint.getName(), accessPoint.getSingleValue());
            sendWiFiHTTPRequest(serverWifiData);
        }
        for (BluetoothObject beacon : fingerprint.getmBeaconsList()) {
            ServerBluetoothData serverBluetoothData = new ServerBluetoothData(beacon.getName(), beacon.getSingleValue());
            sendBLERequest(serverBluetoothData);
        }
        Toast.makeText(this, "Fingerprint Created and being sent to Server", Toast.LENGTH_SHORT).show();
        startBackgroundService();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendFingerprintHTTPRequest(ServerFingerprint fingerprint) {
        //SEND DATA TO SERVER - POST FINGERPRINT THEN DEVICE THEN BLUETOOTH THEN WIFI
        new SendHTTPRequest(fingerprint).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendDeviceHTTPRequest(ServerDeviceData deviceData) {
        //SEND DATA TO SERVER - POST FINGERPRINT THEN DEVICE THEN BLUETOOTH THEN WIFI
        new SendHTTPRequest(deviceData).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendWiFiHTTPRequest(ServerWifiData wifiData) {
        //SEND DATA TO SERVER - POST FINGERPRINT THEN DEVICE THEN BLUETOOTH THEN WIFI
        new SendHTTPRequest(wifiData).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendBLERequest(ServerBluetoothData bluetoothData) {
        //SEND DATA TO SERVER - POST FINGERPRINT THEN DEVICE THEN BLUETOOTH THEN WIFI
        new SendHTTPRequest(bluetoothData).execute();
    }

    public String getZoneClassifier() {
        return this.zoneClassifier;
    }

    public void setZoneClassifier(String zoneClassifier) {
        this.zoneClassifier = zoneClassifier;
    }

    public Map<String, Float> getPreferences() {
        return this.preferences;
    }

    public void setPreferences(Map<String, Float> preferences) {
        this.preferences = preferences;
    }

    public List<String> getSelectedTypes() {
        return this.dataTypes;
    }

    public File writeToFile(String sFileName) {

        File root = new File(Environment.getExternalStorageDirectory(), "fingerprintTets");
        // if external memory exists and folder with name Notes
        if (!root.exists()) {
            root.mkdirs(); // this will create folder.
        }
        File filepath = new File(root, sFileName + ".json");  // file path to save
        return filepath;

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void activateSensorScan() {
        //SMARTPHONE RELATED SENSOR
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorInformationList = new ArrayList<>();
        setupSmartphoneSensors();
        Log.d(SMARTPHONE_SENSOR, "Device sensors configuration ready. Started listening for changes on sensors");

        //BLUETOOTH SENSOR
        mBeaconsList = new ArrayList<>();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.setBackgroundMode(false);
        beaconManager.setForegroundScanPeriod(150);
        beaconManager.bind(this);
        Log.d(BLE, "Alt-Beacon configuration ready. Started advertising packets");

        //WI-FI SENSOR
        mAccessPoints = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiScanReceiver, intentFilter);
        verifyLocation();
        handler.post(locationUpdate);
        Log.d(WIFI, "Access Points configuration ready. Started advertising packets");
    }

    public void verifyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            buildAlertMessageNoGps();
        Log.d(WIFI, "Location Verified");
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

    public void resetDataStructures() {
        mAccessPoints = new ArrayList<>();
        mSensorInformationList = new ArrayList<>();
        mBeaconsList = new ArrayList<>();
    }

    public void scanData() {

        isScanning = true;
        CountDownTimer waitTimer;
        waitTimer = new CountDownTimer(10000, 300) {

            public void onTick(long millisUntilFinished) {
            }


            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onFinish() {
                if (!dataTypes.contains("Wi-fi")) {
                    mAccessPoints = new ArrayList<>();
                }
                if (!dataTypes.contains("Bluetooth")) {
                    mBeaconsList = new ArrayList<>();
                }
                if (!dataTypes.contains("DeviceData")) {
                    mSensorInformationList = new ArrayList<>();
                }
                Fingerprint mNewFingeprint = new Fingerprint(preferences.get("X"), preferences.get("Y"), zoneClassifier, mSensorInformationList, mBeaconsList, mAccessPoints);
                isScanning = false;
                resetDataStructures();
                computeFingerprint(mNewFingeprint);
            }
        };

        try {
            waitTimer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setupSmartphoneSensors() {
        float[] defaultValues = new float[3];
        defaultValues[0] = 0f;
        defaultValues[1] = 0f;
        defaultValues[2] = 0f;
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        SensorObject sensorInfo = new SensorObject("ORIENTATION", defaultValues);
        mSensorInformationList.add(sensorInfo);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isScanning) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading,
                        0, accelerometerReading.length);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading,
                        0, magnetometerReading.length);
            }
            updateOrientationAngles();
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer. (ANDROID DEV CODE)
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        float[] updated_orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles);
        // "mOrientationAngles" now has up-to-date information.
        if (mSensorInformationList.size() != 0) {
            mSensorInformationList.get(0).setValue(updated_orientation);
            Log.d(SMARTPHONE_SENSOR, "Updated orientation sensor with values");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        Log.d(WIFI, "Scanned of Wi-Fi Access Points failed. Consider using old scan results but for now just this log");
    }


    private void scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();
        if (isScanning) {
            Log.d(WIFI, "Scanned " + results.size() + " access points");
            for (ScanResult result : results
            ) {
                int rssi = result.level;
                Log.d(WIFI, "Access Point: " + result.BSSID + " with RSSI " + rssi + " dBm");
                boolean found = false;
                for (int i = 0; i < mAccessPoints.size(); i++) {
                    WifiObject resultInList = mAccessPoints.get(i);
                    if (resultInList.getName().equals(result.BSSID)) {
                        resultInList.setSingleValue(rssi);
                        Log.d(WIFI, "Set RSSI value " + rssi + " dBm to the access point's " + result.BSSID + " list.");
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
    }


    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, org.altbeacon.beacon.Region region) {
                for (Beacon mBeaconScanned : beacons) {
                    if (isScanning) {
                        Log.d(BLE, "didRangeBeaconsInRegion called with beacon count:  " + beacons.size());
                        int rss = mBeaconScanned.getRssi();
                        boolean found = false;
                        for (int i = 0; i < mBeaconsList.size(); i++) {
                            BluetoothObject beaconInList = mBeaconsList.get(i);
                            if (beaconInList.getName().equals(mBeaconScanned.getBluetoothAddress())) {
                                beaconInList.setSingleValue(rss);
                                Log.d(BLE, "Set RSSI value " + rss + " dBm to the beacon's " + mBeaconScanned.getBluetoothAddress() + " list.");
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            BluetoothObject beacon = new BluetoothObject(mBeaconScanned.getBluetoothAddress(), rss);
                            mBeaconsList.add(beacon);
                        }
                    }
                }
            }
        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("uniqueIdRegion", null, null, null));
        } catch (
                RemoteException e) {
        }
    }

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
        Log.d(BLE, "Bluetooth verified.");

    }


    private class SendHTTPRequest extends AsyncTask<Void, Void, String> {

        private ServerFingerprint serverFingerprint;
        private ServerDeviceData serverDeviceData;
        private ServerWifiData serverWifiData;
        private ServerBluetoothData serverBluetoothData;

        private String json;

        public SendHTTPRequest(String json) {
            this.json = json;
        }

        private SendHTTPRequest(ServerFingerprint fingerprint) {
            this.serverFingerprint = fingerprint;
            this.serverDeviceData = null;
            this.serverWifiData = null;
            this.serverBluetoothData = null;
        }

        private SendHTTPRequest(ServerDeviceData deviceData) {
            this.serverFingerprint = null;
            this.serverDeviceData = deviceData;
            this.serverWifiData = null;
            this.serverBluetoothData = null;
        }

        private SendHTTPRequest(ServerWifiData wifiData) {
            this.serverFingerprint = null;
            this.serverDeviceData = null;
            this.serverWifiData = wifiData;
            this.serverBluetoothData = null;
        }

        private SendHTTPRequest(ServerBluetoothData bluetoothData) {
            this.serverFingerprint = null;
            this.serverDeviceData = null;
            this.serverWifiData = null;
            this.serverBluetoothData = bluetoothData;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(Void... voids) {
            Gson gson = new Gson();
            String fingerprintInJson = null;

            try {
                if (json != null && !json.isEmpty()) {
                    post(SERVER_ADDRESS_HEROKU + "filter/", json, "");
                }
                fingerprintInJson = gson.toJson(serverFingerprint);

                if (serverFingerprint != null) {
                    fingerprintId = post(SERVER_ADDRESS_HEROKU + "fingerprints/", fingerprintInJson, "id");
                }
                if (!fingerprintId.equals("")) {
                    if (serverDeviceData != null) {
                        serverDeviceData.setFingerprintId("http://127.0.0.1:8000/fingerprints/" + fingerprintId + "/");
                        String deviceDataInJson = gson.toJson(serverDeviceData);
                        post(SERVER_ADDRESS_HEROKU + "device/", deviceDataInJson, "");
                    }
                    if (serverWifiData != null) {
                        serverWifiData.setFingerprint("http://127.0.0.1:8000/fingerprints/" + fingerprintId + "/");
                        String wifiDataInJson = gson.toJson(serverWifiData);
                        post(SERVER_ADDRESS_HEROKU + "wifi/", wifiDataInJson, "");
                    }
                    if (serverBluetoothData != null) {
                        serverBluetoothData.setFingerprint("http://127.0.0.1:8000/fingerprints/" + fingerprintId + "/");
                        String bleDataInJson = gson.toJson(serverBluetoothData);
                        post(SERVER_ADDRESS_HEROKU + "bluetooth/", bleDataInJson, "");
                    }
                } else {
                    System.err.println("Fingerprint Id missing");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        protected String post(String url, String json, String parameter) throws IOException {
            RequestBody body = RequestBody.create(json, JSON);
            Handler mainHandler = new Handler(getMainLooper());
            String responseString = "";
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                if (parameter.length() > 1)
                    responseString = parse(response.body().string(), parameter);
                else
                    responseString = response.body().string();

            } catch (ConnectException e) {

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SocketTimeoutException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return responseString;
        }

        public String parse(String jsonLine, String parameter) {
            JsonElement jelement = new JsonParser().parse(jsonLine);
            JsonObject jobject = jelement.getAsJsonObject();
            String result = jobject.get(parameter).getAsString();
            return result;
        }
    }

}