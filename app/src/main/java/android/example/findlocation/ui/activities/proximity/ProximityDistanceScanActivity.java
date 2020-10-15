package android.example.findlocation.ui.activities.proximity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothDistanceObject;
import android.example.findlocation.ui.adapters.SectionsPagerAdapterProximityDistance;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProximityDistanceScanActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final String SERVER_ADDRESS_LOCAL = "http://192.168.42.55:8000/";
    private static final String SERVER_ADDRESS_HEROKU = "http://indoorlocationapp.herokuapp.com/";

    private static final long SCAN_PERIOD_TIME = 60000 * 1; // 1 minute of continuous scanning
    private static final String TAG = "TIMER";
    private static final String BEACON = "BEACON";
    private static final String LOG = "LOG";
    private static final String REGION_UUID = "b9407f30-f5f8-466e-aff9-25556b57fe6d";

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;

    private long startTimeNs;
    private OkHttpClient client;
    private BeaconManager beaconManager;
    private BluetoothDistanceObject mTargetBeacon;
    private Map<String, Float> preferences;
    private boolean isScanning;
    private Region region;
    private String zoneClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proximity_distance_scan);
        client = new OkHttpClient();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SectionsPagerAdapterProximityDistance sectionsPagerAdapter = new SectionsPagerAdapterProximityDistance(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pagerDistanceProximityScanId);
        viewPager.setAdapter(sectionsPagerAdapter); //CHANGE
        TabLayout tabs = findViewById(R.id.tabsDistanceProximityScanId);
        tabs.setupWithViewPager(viewPager);
        tabs.getTabAt(0).setIcon(R.drawable.proximitydistanceicon);
        tabs.getTabAt(1).setIcon(R.drawable.preferencesicon);
        preferences = new HashMap<String, Float>();
        isScanning = false;
        zoneClassifier = null;
        verifyBluetooth();
        activateSensorScan();
        requestPermissions();
    }

    public void requestPermissions(){
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
                    }
                    else {
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
                }
                else {
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
                                           String permissions[], int[] grantResults) {
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void setPreferences(Map<String, Float> preferences) {
        this.preferences = preferences;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    public void resetDataStructures() {
        mTargetBeacon.resetValues();
    }

    public void setZoneClassifier(String zoneClassifier) {
        this.zoneClassifier = zoneClassifier;
    }

    public void startScan(View view) throws InterruptedException {

        if (preferences.size() != 0) {
            Toast.makeText(this, "Scanning", Toast.LENGTH_SHORT).show();
            scanData();
        } else {
            Toast.makeText(this, "Check preferences", Toast.LENGTH_SHORT).show();
        }
    }

    protected void activateSensorScan() {
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.setBackgroundMode(false);
        beaconManager.setForegroundScanPeriod(150);
        beaconManager.bind(this);
        startTimeNs = System.nanoTime();
        Log.d(BEACON, "Beacon configuration ready. Start advertising");
    }


    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                long elapsedTimeNs = System.nanoTime() - startTimeNs;
                for (Beacon mBeaconScanned : beacons) {
                    if (beacons.size() > 0 && isScanning) {
                        Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  " + beacons.size());
                        Log.d(BEACON, "Advertising time: " + TimeUnit.MILLISECONDS.convert(elapsedTimeNs, TimeUnit.NANOSECONDS));
                        Log.d(BEACON, "Found beacon " + mBeaconScanned.getBluetoothAddress());
                        int rssi = mBeaconScanned.getRssi(); //RSSI value of beacon
                        if (mTargetBeacon == null) {
                            Log.d(BEACON, "Beacon initialization");
                            mTargetBeacon = new BluetoothDistanceObject(mBeaconScanned.getBluetoothAddress(), rssi);
                        }
                        if (mTargetBeacon.getName().equals(mBeaconScanned.getBluetoothAddress())) {
                            mTargetBeacon.setSingleValue(rssi);
                            mTargetBeacon.addRSSIValue(rssi);
                            Log.d(BEACON, "RSSI VALUE: " + rssi);
                        }
                    }
                }
            }
        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
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

    }

    public void sendScanToServer() {
        BluetoothDistanceObject mCopyCatBeacon = new BluetoothDistanceObject(mTargetBeacon.getName(), mTargetBeacon.getValues());
        long scanningTime = beaconManager.getForegroundScanPeriod();
        System.out.println("Time spent in Scanning: " + scanningTime);
        long scanningPeriod = beaconManager.getForegroundBetweenScanPeriod();
        System.out.println("Time spent between Scannings: " + scanningPeriod);
        System.err.println("NUMBER OF SCANNED VALUES: " + mTargetBeacon.getValues().size());
        mCopyCatBeacon.setX_coordinate(preferences.get("X"));
        mCopyCatBeacon.setY_coordinate(preferences.get("Y"));
        mCopyCatBeacon.setZone(zoneClassifier);
        Log.d(LOG, "Created Copy Cat version of beacon, sending data to server...");
        Gson gson = new Gson();
        String jsonString = gson.toJson(mCopyCatBeacon);
        Toast.makeText(this, "Sending data to server", Toast.LENGTH_SHORT).show();
        new SendHTTPRequest(jsonString).execute();
    }

    public void scanData() {

        isScanning = true;
        CountDownTimer waitTimer;
        long tickTimer = preferences.get("Scan Time").longValue();
        waitTimer = new CountDownTimer(SCAN_PERIOD_TIME, tickTimer) {

            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "BLE samples scanned at: " + millisUntilFinished + " msecs");
                if (mTargetBeacon != null) {
                    Log.d(LOG, "Beacon is up");
                    sendScanToServer();
                    mTargetBeacon.resetValues();
                }
            }

            public void onFinish() {
                sendScanToServer();
                Toast.makeText(getApplicationContext(), "Finished Scanning ", Toast.LENGTH_SHORT).show();
                isScanning = false;
                resetDataStructures();
            }
        };

        Log.d(TAG, "Starting BLE scanning for" + SCAN_PERIOD_TIME + " msecs");
        waitTimer.start();
    }

    private class SendHTTPRequest extends AsyncTask<Void, Void, String> {

        private String json;

        public SendHTTPRequest(String json) {
            this.json = json;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(Void... voids) {
            try {
                post(SERVER_ADDRESS_HEROKU + "proximity/distance", json, "");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "200";
        }

        @Override
        protected void onPostExecute(String message) {
            super.onPostExecute(message);
            System.out.println("LOG MESSAGE: " + message);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        protected void post(String url, String json, String parameter) throws IOException {
            RequestBody body = RequestBody.create(json, JSON);
            Handler mainHandler = new Handler(getMainLooper());
            String responseString = "";
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                if (parameter.length() > 1) {
                    responseString = parse(response.body().string(), parameter);
                    System.out.println("RESPONSE: " + responseString);
                } else {
                    responseString = response.body().string();
                    System.out.println("RESPONSE: " + responseString);
                }
                response.body().close();
            } catch (ConnectException e) {
                e.printStackTrace();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        public String parse(String jsonLine, String parameter) {
            JsonElement jelement = new JsonParser().parse(jsonLine);
            JsonObject jobject = jelement.getAsJsonObject();
            String result = jobject.get(parameter).getAsString();
            return result;
        }
    }
}