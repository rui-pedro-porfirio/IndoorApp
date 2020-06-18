package android.example.findlocation.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothDistanceObject;
import android.example.findlocation.ui.main.SectionsPagerAdapter;
import android.example.findlocation.ui.main.SectionsPagerAdapterProximityDistance;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProximityDistanceScanActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ADDRESS = "http://192.168.1.123:8000/";
    private static final long SCAN_PERIOD_TIME = 60000; // 1 minute of continuous scanning
    private static final String TAG = "TIMER";
    private static final String LOG = "LOG";


    private OkHttpClient client;
    private BeaconManager beaconManager;
    private BluetoothDistanceObject mTargetBeacon;
    private Map<String, Float> preferences;
    private boolean isScanning;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        activateSensorScan();
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
        beaconManager.bind(this);
        verifyBluetooth();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, org.altbeacon.beacon.Region region) {
                if (isScanning) {
                    if (beacons.size() > 0) {

                        Beacon beaconScanned = beacons.iterator().next();
                        int rssi = beaconScanned.getRssi();
                        boolean found = false;
                        if (mTargetBeacon != null && mTargetBeacon.getName().equals(beaconScanned.getBluetoothAddress())) {
                            mTargetBeacon.setSingleValue(rssi);
                            mTargetBeacon.addRSSIValue(rssi);
                            found = true;
                        }

                        if (found == false) {
                            mTargetBeacon = new BluetoothDistanceObject(beaconScanned.getBluetoothAddress(), rssi);
                            mTargetBeacon.addRSSIValue(rssi);
                            System.out.println("HERE");
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

    }

    public void sendScanToServer() {
        BluetoothDistanceObject mCopyCatBeacon = new BluetoothDistanceObject(mTargetBeacon.getName(),mTargetBeacon.getValues());
        mCopyCatBeacon.setX_coordinate(preferences.get("X"));
        mCopyCatBeacon.setY_coordinate(preferences.get("Y"));
        Log.d(LOG,"Created Copy Cat version of beacon, sending data to server...");
        Gson gson = new Gson();
        String jsonString = gson.toJson(mCopyCatBeacon);
        resetDataStructures();
        Toast.makeText(this, "Sending data to server", Toast.LENGTH_SHORT).show();
        new SendHTTPRequest(jsonString).execute();
    }

    public void scanData() {

        isScanning = true;
        CountDownTimer waitTimer;
        long tickTimer = preferences.get("Scan Time").longValue();
        waitTimer = new CountDownTimer(SCAN_PERIOD_TIME, tickTimer) {

            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "notify countDown: " + millisUntilFinished + " msecs");
                if (mTargetBeacon != null) {
                    Log.d(LOG,"Beacon is up");
                    sendScanToServer();
                }
            }

            public void onFinish() {
                sendScanToServer();
                isScanning = false;
                Toast.makeText(getApplicationContext(),"Finished Scanning ",Toast.LENGTH_SHORT).show();
            }
        };

        Log.d(TAG, "start countDown for " + SCAN_PERIOD_TIME + " msecs");
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
                post(ADDRESS + "proximity/distance", json, "");

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
        }

        public String parse(String jsonLine, String parameter) {
            JsonElement jelement = new JsonParser().parse(jsonLine);
            JsonObject jobject = jelement.getAsJsonObject();
            String result = jobject.get(parameter).getAsString();
            return result;
        }
    }
}