package android.example.findlocation.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.example.findlocation.BuildConfig;
import android.example.findlocation.IndoorApp;
import android.example.findlocation.R;
import android.example.findlocation.exceptions.HTTPRequestException;
import android.example.findlocation.exceptions.SharedPreferencesException;
import android.example.findlocation.interfaces.SharedPreferencesInterface;
import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;
import android.example.findlocation.objects.server.ScanningObject;
import android.example.findlocation.ui.activities.main.MainActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ActiveScanningService extends Service implements SensorEventListener, BeaconConsumer, SharedPreferencesInterface {

    private static final String TAG = ActiveScanningService.class.getSimpleName();

    public static final int NOTIFICATION_ID = 8165;
    static final String CHANNEL_ID = "indoorApp.BackgroundScanningService";

    static final String PREF_USERNAME = "PREF_USERNAME";
    static final String PREF_DEVICE_UUID = "PREF_DEVICE_UUID";

    static final String SERVER_ENDPOINT_ADDRESS = "http://192.168.1.4:8080/scanning/";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    static final long SERVICE_DELAY = 10000;

    static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    private ServiceHandler mServiceHandler;
    private OkHttpClient mHttpClient;

    //TECHNOLOGIES RELATED STRUCTURES
    private SensorManager mSensorManager;
    private WifiManager wifiManager;
    private BeaconManager beaconManager;
    private List<WifiObject> mAccessPointsList;
    private List<BluetoothObject> mBeaconsList;
    private List<SensorObject> mSensorInformationList;
    private Notification.Builder mBuilder;
    private int mLatestKnownAps;
    private int mLatestKnownBeacons;

    private SharedPreferences mAppPreferences;
    private String mUsername;
    private String mDeviceUuid;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSharedPreferences();
        loadVariablesFromSharedPreferences();
        mHttpClient = new OkHttpClient();
        mLatestKnownAps = 0;
        mLatestKnownBeacons = 0;
        startThreadAndHandler();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting Scanning Service...");
        Notification mNotification = structureNotificationForForegroundUsage();
        Log.i(TAG, "Notification created for Foreground usage");
        startForeground(NOTIFICATION_ID, mNotification);
        Log.i(TAG, "Started Foreground Service");
        activateSensorScan();
        handleScanningService();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanUpService();
    }

    @Override
    public void initializeSharedPreferences() {
        mAppPreferences = IndoorApp.appPreferences;
    }

    @Override
    public void loadVariablesFromSharedPreferences() {
        mUsername = mAppPreferences.getString(PREF_USERNAME, null);
        mDeviceUuid = mAppPreferences.getString(PREF_DEVICE_UUID, null);
        if (mUsername == null || mDeviceUuid == null) {
            try {
                throw new SharedPreferencesException("Username or Device UUID not found in shared preferences. Something went wrong in the authentication process.");
            } catch (SharedPreferencesException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanUpService() {
        stopForeground(true);
        beaconManager.unbind(this);
        unregisterReceiver(wifiScanReceiver);
        mSensorManager.unregisterListener(this);
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    private void startThreadAndHandler() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments");
        thread.start();
        Log.i(TAG, "Starting new thread for Active Scanning Service");
        // Get the HandlerThread's Looper and use it for our Handler
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification structureNotificationForForegroundUsage() {
        createNotificationChannel();
        PendingIntent mPendingIntent = createPendingIntentForNotification();
        return createNotification(mPendingIntent);
    }

    /**
     * Create Notification Channel for Foreground Usage
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence mNotificationName = "Scanning Service";
            int mImportance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, mNotificationName, mImportance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private PendingIntent createPendingIntentForNotification() {
        Intent mNotificationIntent = new Intent(this, MainActivity.class);
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, mNotificationIntent, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification(PendingIntent mPendingIntent) {
        mBuilder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText("Access Points Detected: 0 | Beacons Detected: 0")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(mPendingIntent);
        return mBuilder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateNotification(String message) {
        mBuilder.setContentText(message);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    protected void handleScanningService() {
        mServiceHandler.postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void run() {
                if (mLatestKnownAps != mAccessPointsList.size() || mLatestKnownBeacons != mBeaconsList.size()) {
                    updateNotification("Access Points Detected: " + mAccessPointsList.size() + " | Beacons Detected: " + mBeaconsList.size());
                    mLatestKnownAps = mAccessPointsList.size();
                    mLatestKnownBeacons = mBeaconsList.size();
                }
                if (mAccessPointsList.size() != 0 || mBeaconsList.size() != 0)
                    sendDataToServer();
                //serviceHandler.postDelayed(this, delay); Uncomment this to become cyclic
            }
        }, SERVICE_DELAY);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendDataToServer() {
        ScanningObject mScanningObject = new ScanningObject(mUsername, mDeviceUuid, mAccessPointsList, mBeaconsList, mSensorInformationList);
        String mScanningObjectInJson = convertToJsonString(mScanningObject);
        sendPostHTTPRequest(SERVER_ENDPOINT_ADDRESS, mScanningObjectInJson);
    }

    private String convertToJsonString(ScanningObject mScanningObject) {
        Gson gson = new Gson();
        return gson.toJson(mScanningObject);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendPostHTTPRequest(String mUrl, String mJsonString) {
        Handler mainHandler = new Handler(getMainLooper());
        Request mPostRequest = structureRequest(mUrl, mJsonString);
        try (Response response = mHttpClient.newCall(mPostRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new HTTPRequestException("Unexpected code " + response);
            } else {
                Log.i(TAG, "Successfully sent data to the server.");
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (HTTPRequestException e) {
            e.printStackTrace();
        }
    }

    private Request structureRequest(String url, String jsonString) {
        RequestBody mRequestBody = RequestBody.create(jsonString, JSON);
        return new Request.Builder()
                .url(url)
                .post(mRequestBody)
                .build();
    }

    protected void activateSensorScan() {
        initializeDeviceSensor();
        initializeBluetoothSensor();
        initializeWifiSensor();
    }

    private void initializeDeviceSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorInformationList = new ArrayList<>();
        listenForOrientationSensor();
        Log.i(TAG, "Successfully initialized device sensor information");
    }

    private void listenForOrientationSensor() {
        float[] mDefaultValues = new float[3];
        mDefaultValues[0] = 0f;
        mDefaultValues[1] = 0f;
        mDefaultValues[2] = 0f;
        Sensor mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, mOrientation,
                SensorManager.SENSOR_DELAY_NORMAL);
        SensorObject mOrientationSensorObject = new SensorObject(mOrientation.getName(), mDefaultValues);
        mSensorInformationList.add(mOrientationSensorObject);
        Log.i(TAG, "Added Orientation Sensor information to list.");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mSensorDetected = event.sensor;
        for (SensorObject mKnownSensor : mSensorInformationList) {
            if (mSensorDetected.getName().equals(mKnownSensor.getName())) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "New values for orientation: " + Arrays.toString(event.values));
                mKnownSensor.setValue(event.values);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void initializeBluetoothSensor() {
        mBeaconsList = new ArrayList<>();
        beaconManager = BeaconManager.getInstanceForApplication(this);
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
                if (beacons.size() > 0) {

                    Beacon mBeaconScanned = beacons.iterator().next();
                    int mRssi = mBeaconScanned.getRssi();
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "New values for beacon: " + mBeaconScanned.getBluetoothAddress() + " | RSSI: " + mRssi);
                    boolean mBeaconExists = false;
                    for (int i = 0; i < mBeaconsList.size(); i++) {
                        BluetoothObject mKnownBeacon = mBeaconsList.get(i);
                        if (mKnownBeacon.getName().equals(mBeaconScanned.getBluetoothAddress())) {
                            mKnownBeacon.setSingleValue(mRssi);
                            mKnownBeacon.addValue(mRssi);
                            mBeaconExists = true;
                            break;
                        }
                    }

                    if (!mBeaconExists) {
                        BluetoothObject mNewBeaconFound = new BluetoothObject(mBeaconScanned.getBluetoothAddress(), mRssi);
                        mNewBeaconFound.addValue(mRssi);
                        mBeaconsList.add(mNewBeaconFound);
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

    private void initializeWifiSensor() {
        mAccessPointsList = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Log.i(TAG, "Successfully initialized wifi settings for scanning.");
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleApScans();
            }
        }
    };

    private void handleApScans() {
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
                    mAccessPointExists = true;
                    break;
                }
            }

            if (!mAccessPointExists) {
                WifiObject mNewAccessPointFound = new WifiObject(mScanResult.BSSID, mRssi);
                Log.i(TAG, "Added access point " + mNewAccessPointFound.getName() + " to the known list.");
                mAccessPointsList.add(mNewAccessPointFound);
            }
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ActiveScanningService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ActiveScanningService.this;
        }
    }
}
