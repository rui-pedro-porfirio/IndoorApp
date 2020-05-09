package android.example.findlocation.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.example.findlocation.R;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.Fingerprint;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;
import android.example.findlocation.objects.server.ServerBluetoothData;
import android.example.findlocation.objects.server.ServerDeviceData;
import android.example.findlocation.objects.server.ServerFingerprint;
import android.example.findlocation.objects.server.ServerPosition;
import android.example.findlocation.objects.server.ServerWifiData;
import android.example.findlocation.ui.main.SectionsPagerAdapter;
import android.example.findlocation.ui.main.SectionsPagerAdapterOnline;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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

public class OnlineActivity extends AppCompatActivity implements SensorEventListener, BeaconConsumer {

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ADDRESS = "http://192.168.1.3:8000/";

    private List<String> dataTypes;
    private String algorithm;
    private String filter;
    private OkHttpClient client;
    private SensorManager mSensorManager;
    private WifiManager wifiManager;
    private BeaconManager beaconManager;
    private List<WifiObject> mAccessPoints;
    private List<BluetoothObject> mBeaconsList;
    private List<SensorObject> mSensorInformationList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SectionsPagerAdapterOnline sectionsPagerAdapter = new SectionsPagerAdapterOnline(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pagerOnline);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabsOnline);
        tabs.setupWithViewPager(viewPager);
        dataTypes = new ArrayList<String>();
        algorithm = null;
        filter = null;
        tabs.getTabAt(0).setIcon(R.drawable.map_marker_small);
        tabs.getTabAt(1).setIcon(R.drawable.preferencesicon);
        client = new OkHttpClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        activateSensorScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        unregisterReceiver(wifiScanReceiver);
    }

    public void computeNewPosition(List<Float> coordinates){
        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarLocationId);
        mProgressBar.setVisibility(View.INVISIBLE);
        TextView mTextTitle = (TextView) findViewById(R.id.foundPositionTextViewId);
        mTextTitle.setVisibility(View.VISIBLE);
        LinearLayout mLinearLayout = (LinearLayout) findViewById(R.id.linearLayouttabPositionId);
        mLinearLayout.setVisibility(View.VISIBLE);
        TextView xTextView = (TextView) findViewById(R.id.x_coordinate_positionValueId);
        xTextView.setText(String.valueOf(coordinates.get(0)));
        TextView yTextView = (TextView) findViewById(R.id.y_coordinate_positionValueId);
        yTextView.setText(String.valueOf(coordinates.get(1)));
    }

    public void onSensorClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();


        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.checkbox_wifi_online:
                if (checked) {
                    if (!dataTypes.contains("Wi-Fi"))
                        dataTypes.add("Wi-fi");
                } else {
                    if (dataTypes.contains("Wi-Fi"))
                        dataTypes.remove("Wi-Fi");
                }
                break;
            case R.id.checkbox_bluetooth_online:
                if (checked) {
                    if (!dataTypes.contains("Bluetooth"))
                        dataTypes.add("Bluetooth");
                } else {
                    if (dataTypes.contains("Bluetooth"))
                        dataTypes.remove("Bluetooth");
                }
                break;
            case R.id.checkbox_device_sensors_online:
                if (checked) {
                    if (!dataTypes.contains("DeviceData"))
                        dataTypes.add("DeviceData");
                } else {
                    if (dataTypes.contains("DeviceData"))
                        dataTypes.remove("DeviceData");
                }
                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radioknnrId:
                if (checked)
                    algorithm = "KNNR";
                break;
            case R.id.radioknnCId:
                if (checked)
                    algorithm = "KNNC";
                break;
        }
    }

    public void onFilterClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radioNoneFilterId:
                if (checked)
                    filter = "None";
                break;
            case R.id.radioMeanFilterId:
                if (checked)
                    filter = "Mean";
                break;
            case R.id.radioMedianFilterId:
                if(checked)
                    filter = "Median";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void computeFingerprint(Fingerprint fingerprint) {
        Fingerprint newFingerprint = new Fingerprint();
        newFingerprint.setX_coordinate(fingerprint.getX_coordinate());
        newFingerprint.setY_coordinate(fingerprint.getY_coordinate());
        newFingerprint.setmAccessPoints(fingerprint.getmAccessPoints());
        newFingerprint.setmBeaconsList(fingerprint.getmBeaconsList());
        newFingerprint.setmSensorInformationList(fingerprint.getmSensorInformationList());
        Map<String, Integer> access_points = new HashMap<>();
        for (WifiObject ap : newFingerprint.getmAccessPoints()
        ) {
            access_points.put(ap.getName(), ap.getSingleValue());
        }
        Gson gson = new Gson();
        ServerPosition position = new ServerPosition(algorithm,filter,access_points,dataTypes);
        String jsonString = gson.toJson(position);
        new SendHTTPRequest(jsonString).execute();
    }

    public void addFindUserPositionListener(View view) throws InterruptedException {

        if (dataTypes.size() != 0 && algorithm != null && filter != null) {
            Toast.makeText(this, "Finding Your Position", Toast.LENGTH_SHORT).show();
            Button mButton = (Button) view.findViewById(R.id.buttonFindUserPositionId);
            mButton.setVisibility(View.INVISIBLE);
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarLocationId);
            mProgressBar.setVisibility(View.VISIBLE);
            scanData();
        } else {
            Toast.makeText(this, "Check preferences", Toast.LENGTH_SHORT).show();
        }
    }

    protected void activateSensorScan() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorInformationList = new ArrayList<>();
        getAvailableDeviceSensors();

        //BLUETOOTH SENSOR
        mBeaconsList = new ArrayList<>();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.bind(this);
        verifyBluetooth();

        //WI-FI SENSOR
        mAccessPoints = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    public void scanData() {

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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void onPostExecute(Fingerprint fingerprint) {
        //SEND FINGERPRINT
        computeFingerprint(fingerprint);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensorDetected = event.sensor;
        for (SensorObject sensorInList : mSensorInformationList) {
            if (sensorDetected.getName().equals(sensorInList.getName())) {
                sensorInList.setValue(event.values);
            }
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

    private class SendHTTPRequest extends AsyncTask<Void, Void, List<Float>> {

        private String json;

        public SendHTTPRequest(String json) {
            this.json = json;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected List<Float> doInBackground(Void... voids) {
            List<Float> coordinates = new ArrayList<>();
            try {
                coordinates = post(ADDRESS + "radiomap/position", json, "");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return coordinates;
        }

        @Override
        protected void onPostExecute(List<Float> coordinates) {
            super.onPostExecute(coordinates);
            computeNewPosition(coordinates);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        protected List<Float> post(String url, String json, String parameter) throws IOException {
            RequestBody body = RequestBody.create(json, JSON);
            Handler mainHandler = new Handler(getMainLooper());
            String responseString = "";
            List<Float> coordinates = new ArrayList<>();
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
                    coordinates.add((float) jsonObject.getDouble("coordinate_X"));
                    coordinates.add((float) jsonObject.getDouble("coordinate_Y"));
                }

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
            return coordinates;
        }

        public String parse(String jsonLine, String parameter) {
            JsonElement jelement = new JsonParser().parse(jsonLine);
            JsonObject jobject = jelement.getAsJsonObject();
            String result = jobject.get(parameter).getAsString();
            return result;
        }
    }
}
