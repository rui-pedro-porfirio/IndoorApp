package android.example.findlocation.ui.activities.scanning;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.example.findlocation.IndoorApp;
import android.example.findlocation.R;
import android.example.findlocation.interfaces.SharedPreferencesInterface;
import android.example.findlocation.services.OAuthBackgroundService;
import android.example.findlocation.services.ScanBackgroundService;
import android.example.findlocation.services.ScanBackgroundService.LocalBinder;
import android.example.findlocation.ui.common.BluetoothVerifier;
import android.example.findlocation.ui.common.DevicePermissions;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.altbeacon.beacon.BeaconManager;

public class ScanningActivity extends AppCompatActivity{

    private static final String TAG = ScanningActivity.class.getSimpleName();

    private ScanBackgroundService mService;
    private DevicePermissions mDevicePermissions;
    private boolean mBound;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"Starting Online Scanning activity.");
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mService = null;
        mBound = false;
        setContentView(R.layout.activity_scanning);
        startBackgroundScanningService();
        bindToBackgroundScanningService();
        verifyBluetooth();
        requestPermissions();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Destroying activity and the Scanning Background Service");
        Intent mDestroyServiceIntent = new Intent(this, ScanBackgroundService.class);
        unbindService(connection);
        mBound = false;
        stopService(mDestroyServiceIntent);
    }

    protected void startBackgroundScanningService(){
        Log.i(TAG,"Starting Scanning Background Service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ScanBackgroundService.class));
        } else {
            startService(new Intent(this, ScanBackgroundService.class));
        }
    }

    protected void bindToBackgroundScanningService(){
        Log.i(TAG,"Binding to Scanning Background Service");
        Intent mStartBindIntent = new Intent(this, ScanBackgroundService.class);
        bindService(mStartBindIntent, connection, Context.BIND_AUTO_CREATE);
    }

    protected void requestPermissions(){
        mDevicePermissions = new DevicePermissions(this);
        Log.i(TAG,"Requesting Permissions to run in background.");
        mDevicePermissions.requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mDevicePermissions.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    protected void verifyBluetooth(){
        Log.i(TAG,"Verifying Bluetooth's state.");
        BluetoothVerifier mBluetoothVerifier = new BluetoothVerifier(this);
        mBluetoothVerifier.verifyBluetooth();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to ScanBackgroundService, cast the IBinder and get LocalService instance
            LocalBinder mBinder = (LocalBinder) service;
            mService = mBinder.getService();
            mBound = true;
            Log.i(TAG,"Bound to Scanning Service.");
            Toast.makeText(getApplicationContext(), "Scanning Service Started", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG,"UnBound to Scanning Service.");
            mBound = false;
        }
    };

}