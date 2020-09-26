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
import android.example.findlocation.IndoorApp;
import android.example.findlocation.R;
import android.example.findlocation.ui.activities.main.MainActivity;
import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.SensorObject;
import android.example.findlocation.objects.client.WifiObject;
import android.example.findlocation.objects.server.ScanningObject;
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
import java.util.Collection;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScanBackgroundService extends Service implements SensorEventListener, BeaconConsumer {

    public static final int NOTIFICATION_ID = 5555;
    private static final String ADDRESS = "http://192.168.1.4:8080/";
    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private final static String CHANNEL_ID = "indoorApp.ScanningService";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String USERNAME_KEY = "Username";
    private static final String DEVICE_UUID_KEY = "Device UUID";
    private Notification.Builder builder;
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private int delay;
    private OkHttpClient client;

    //TECHNOLOGIES RELATED STRUCTURES
    private SensorManager mSensorManager;
    private WifiManager wifiManager;
    private BeaconManager beaconManager;
    private List<WifiObject> mAccessPoints;
    private List<BluetoothObject> mBeaconsList;
    private List<SensorObject> mSensorInformationList;
    private int latestSizeAP;
    private int latestSizeBLE;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private SharedPreferences applicationPreferences;


    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient();
        applicationPreferences = IndoorApp.appPreferences;
        HandlerThread thread = new HandlerThread("ServiceStartArguments");
        thread.start();
        latestSizeAP = 0;
        latestSizeBLE = 0;
        delay = 10000;

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText("Access Points Detected: 0 | Beacons Detected: 0")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(pendingIntent);

        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);
        activateSensorScan();
        serviceHandler.postDelayed(new Runnable() {
            public void run() {
                //UPDATE NOTIFICATION
                if (latestSizeAP != mAccessPoints.size() || latestSizeBLE != mBeaconsList.size()) {
                    builder.setContentText("Access Points Detected: " + mAccessPoints.size() + " | Beacons Detected: " + mBeaconsList.size());
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                    latestSizeAP = mAccessPoints.size();
                    latestSizeBLE = mBeaconsList.size();
                }
                //SEND TO SERVER COLLECTED DATA
                if (mAccessPoints.size() != 0 || mBeaconsList.size() != 0)
                    sendToServer();
                //serviceHandler.postDelayed(this, delay);
            }
        }, delay);
        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendToServer() {
        String username = applicationPreferences.getString(USERNAME_KEY, null);
        String deviceUuid = applicationPreferences.getString(DEVICE_UUID_KEY, null);
        ScanningObject scanningObject = new ScanningObject(username, deviceUuid, mAccessPoints, mBeaconsList, mSensorInformationList);
        Gson gson = new Gson();
        String json = gson.toJson(scanningObject);
        sendPostHTTPRequest(ADDRESS + "scanning/", json, "");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendPostHTTPRequest(String url, String json, String parameter) {
        RequestBody body = RequestBody.create(json, JSON);
        Handler mainHandler = new Handler(getMainLooper());
        String responseString = "";
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            } else {
                System.out.println("RESPONSE: " + response.body().string());
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
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ScanBackgroundService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ScanBackgroundService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        //Clean up resources as threads
        beaconManager.unbind(this);
        unregisterReceiver(wifiScanReceiver);
        mSensorManager.unregisterListener(this);
        serviceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Create Notification Channel for Foreground Usage
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Scanning Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    protected void activateSensorScan() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorInformationList = new ArrayList<>();
        getAvailableDeviceSensors();

        //BLUETOOTH SENSOR
        mBeaconsList = new ArrayList<>();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundMode(false);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.bind(this);
        beaconManager.setForegroundScanPeriod(150);

        //WI-FI SENSOR
        mAccessPoints = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
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
                            beaconfound.addValue(rss);
                            found = true;
                            break;
                        }
                    }

                    if (found == false) {
                        BluetoothObject beacon = new BluetoothObject(beaconScanned.getBluetoothAddress(), rss);
                        beacon.addValue(rss);
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
}
