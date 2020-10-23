package android.example.findlocation.ui.activities.scanning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.example.findlocation.R;
import android.example.findlocation.services.ActiveScanningService;
import android.example.findlocation.services.ActiveScanningService.LocalBinder;
import android.example.findlocation.ui.common.BluetoothVerifier;
import android.example.findlocation.ui.common.DevicePermissions;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class ScanningActivity extends AppCompatActivity {

    private static final String TAG = ScanningActivity.class.getSimpleName();

    private ActiveScanningService mService;
    private DevicePermissions mDevicePermissions;
    private boolean mBound;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to ScanBackgroundService, cast the IBinder and get LocalService instance
            LocalBinder mBinder = (LocalBinder) service;
            mService = mBinder.getService();
            mBound = true;
            Log.i(TAG, "Bound to Scanning Service.");
            Toast.makeText(getApplicationContext(), "Scanning Service Started", Toast.LENGTH_SHORT).show();
            unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "UnBound to Scanning Service.");
            mBound = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Starting Online Scanning activity.");
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mService = null;
        mBound = false;
        setContentView(R.layout.activity_scanning);
        verifyBluetooth();
        requestPermissions();
        startBackgroundScanningService();
        bindToBackgroundScanningService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.i(TAG, "Destroying activity and the Scanning Background Service");
        //Intent mDestroyServiceIntent = new Intent(this, ActiveScanningService.class);
        //stopService(mDestroyServiceIntent);
    }

    protected void startBackgroundScanningService() {
        Log.i(TAG, "Starting Scanning Background Service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ActiveScanningService.class));
        } else {
            startService(new Intent(this, ActiveScanningService.class));
        }
    }

    protected void bindToBackgroundScanningService() {
        Log.i(TAG, "Binding to Scanning Background Service");
        Intent mStartBindIntent = new Intent(this, ActiveScanningService.class);
        bindService(mStartBindIntent, connection, Context.BIND_AUTO_CREATE);
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

}