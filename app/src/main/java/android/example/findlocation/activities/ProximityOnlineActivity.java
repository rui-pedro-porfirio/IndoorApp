package android.example.findlocation.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothDistanceObject;
import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.Fingerprint;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;
import android.example.findlocation.objects.server.ServerPosition;
import android.example.findlocation.ui.main.SectionsPagerAdapterOnline;
import android.example.findlocation.ui.main.SectionsPagerAdapterOnlineProximity;
import android.example.findlocation.ui.main.SectionsPagerAdapterProximityDistance;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

public class ProximityOnlineActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ADDRESS = "http://192.168.1.4:8000/";

    private String algorithm;
    private OkHttpClient client;
    private BeaconManager beaconManager;
    private static final String TAG = "TIMER";
    private static final String LOG = "LOG";
    private BluetoothDistanceObject mTargetBeacon;
    private boolean isScanning;
    private String zoneClassified;
    private List<Float> coordinates;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proximity_online);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SectionsPagerAdapterOnlineProximity sectionsPagerAdapter = new SectionsPagerAdapterOnlineProximity(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pagerProximityId);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.proximitytabsOnline);
        tabs.setupWithViewPager(viewPager);
        algorithm = null;
        mTargetBeacon = null;
        tabs.getTabAt(0).setIcon(R.drawable.map_marker_small);
        tabs.getTabAt(1).setIcon(R.drawable.preferencesicon);
        client = new OkHttpClient();
        zoneClassified = "";
        coordinates = new ArrayList<>();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    public void resetDataStructures() {
        this.coordinates = new ArrayList<>();
        this.zoneClassified = "";
        this.algorithm = "";
        mTargetBeacon.resetValues();
    }

    public void computeNewPosition() {
        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.proximity_progressBarLocationId);
        mProgressBar.setVisibility(View.INVISIBLE);
        TextView mTextTitle = (TextView) findViewById(R.id.proximityFoundPositionTextViewId);
        mTextTitle.setVisibility(View.VISIBLE);
        if (zoneClassified.length() < 1 && coordinates.size() > 1) {
            LinearLayout mLinearLayout = (LinearLayout) findViewById(R.id.proximityLinearLayoutTabPositionRegressionId);
            mLinearLayout.setVisibility(View.VISIBLE);
            TextView xTextView = (TextView) findViewById(R.id.proximity_x_coordinate_positionValueId);
            xTextView.setText(String.valueOf(coordinates.get(0)));
            TextView yTextView = (TextView) findViewById(R.id.proximity_y_coordinate_positionValueId);
            yTextView.setText(String.valueOf(coordinates.get(1)));
        } else if (zoneClassified.length() > 1) {
            LinearLayout mLinearLayout = (LinearLayout) findViewById(R.id.proximityLinearLayoutTabPositionClassifierId);
            mLinearLayout.setVisibility(View.VISIBLE);
            TextView zoneTextView = (TextView) findViewById(R.id.proximity_zone_predictionId);
            zoneTextView.setText(zoneClassified);
        }
        resetDataStructures();
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }


    public void sendScanToServer() {
        if(mTargetBeacon != null) {
            BluetoothDistanceObject mCopyCatBeacon = new BluetoothDistanceObject(mTargetBeacon.getName(), mTargetBeacon.getValues());
            Log.d(LOG, "Created Copy Cat version of beacon, sending data to server...");
            Gson gson = new Gson();
            String jsonString = gson.toJson(mCopyCatBeacon);
            resetDataStructures();
            Toast.makeText(this, "Sending data to server", Toast.LENGTH_SHORT).show();
            new SendHTTPRequest(jsonString).execute();
        }
        else{
            Toast.makeText(this,"ERROR: Scanning Beacon not working properly",Toast.LENGTH_SHORT).show();
        }
    }

    public void addFindUserPositionListener(View view) throws InterruptedException {

        if (algorithm != null) {
            Toast.makeText(this, "Finding Your Position", Toast.LENGTH_SHORT).show();
            Button mButton = (Button) view.findViewById(R.id.proximityButtonFindUserPositionId);
            mButton.setVisibility(View.INVISIBLE);
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.proximity_progressBarLocationId);
            mProgressBar.setVisibility(View.VISIBLE);
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

    public void scanData() {

        isScanning = true;
        CountDownTimer waitTimer;
        waitTimer = new CountDownTimer(10000, 300) {

            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "notify countDown: " + millisUntilFinished + " msecs");
            }

            public void onFinish() {
                sendScanToServer();
                isScanning = false;
                Toast.makeText(getApplicationContext(), "Finished Scanning ", Toast.LENGTH_SHORT).show();
            }
        };
        waitTimer.start();
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

    private class SendHTTPRequest extends AsyncTask<Void, Void, String> {

        private String json;

        public SendHTTPRequest(String json) {
            this.json = json;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(Void... voids) {
            try {
                post(ADDRESS + "radiomap/position", json, "");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "200";
        }

        @Override
        protected void onPostExecute(String message) {
            super.onPostExecute(message);
            computeNewPosition();
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
                    JSONObject jsonObject = new JSONObject(responseString);
                    if (algorithm.contains("Classifi")) {
                        zoneClassified = jsonObject.getString("zone");
                    } else {
                        coordinates.add((float) jsonObject.getDouble("coordinate_X"));
                        coordinates.add((float) jsonObject.getDouble("coordinate_Y"));
                    }
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
            } catch (JSONException e) {
                e.printStackTrace();
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