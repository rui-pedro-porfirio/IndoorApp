package android.example.findlocation.activities.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.example.findlocation.App;
import android.example.findlocation.R;
import android.example.findlocation.services.ScanBackgroundService;
import android.example.findlocation.services.ScanBackgroundService.*;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ScanningActivity extends AppCompatActivity {

    private ScanBackgroundService mService;
    private boolean mBound = false;
    private SharedPreferences applicationPreferences;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applicationPreferences = App.preferences;
        //Start Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ScanBackgroundService.class));
        } else {
            startService(new Intent(this, ScanBackgroundService.class));
        }
        //Bind to Service
        Intent bindIntent = new Intent(this, ScanBackgroundService.class);
        bindService(bindIntent, connection, Context.BIND_AUTO_CREATE);
        setContentView(R.layout.activity_scanning);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences.Editor preferencesEditor = applicationPreferences.edit();
        preferencesEditor.putBoolean("TRACKING_STATUS", true);
        preferencesEditor.apply();
        TextView trackingView = (TextView) findViewById(R.id.tracking_statusIdP2);
        ImageView trackingImage = (ImageView) findViewById(R.id.tracking_buttonIdP2);
        checkTrackingStatus(trackingView,trackingImage);
    }

    private void checkTrackingStatus(TextView trackingView, ImageView imageView){
        boolean status = applicationPreferences.getBoolean("TRACKING_STATUS",false);
        if(!status){
            trackingView.setText("Not Tracking");
            trackingView.setTextColor(Color.parseColor("#E80A0A"));
            imageView.setImageResource(android.R.drawable.ic_notification_overlay);
        }
        else{
            trackingView.setText("Tracking");
            trackingView.setTextColor(Color.parseColor("#4CAF50"));
            imageView.setImageResource(android.R.drawable.presence_online);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to ScanBackgroundService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Toast.makeText(getApplicationContext(),"Scanning Service Started",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        //Destroy Service
        Intent destroyServiceIntent = new Intent(this, ScanBackgroundService.class);
        unbindService(connection);
        mBound = false;
        stopService(destroyServiceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor preferencesEditor = applicationPreferences.edit();
        preferencesEditor.putBoolean("TRACKING_STATUS", false);
        preferencesEditor.apply();
    }
}