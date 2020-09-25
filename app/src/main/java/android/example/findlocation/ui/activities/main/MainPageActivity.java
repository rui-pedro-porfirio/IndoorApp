package android.example.findlocation.ui.activities.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.example.findlocation.R;
import android.example.findlocation.ui.activities.fingerprinting.FingerprintingScreenActivity;
import android.example.findlocation.ui.activities.proximity.ProximityScreenActivity;
import android.example.findlocation.ui.activities.sensors.SensorInformationActivity;
import android.example.findlocation.ui.activities.trilateration.TrilaterationScreenActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Screen with options "Analysis" and "Find location of device"
 */
public class MainPageActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        startScanningData();
        enableFingerprinting();
        startProximity();
        startTrilateration();
    }


    public void startScanningData(){
        final Intent scanStartIntent = new Intent(this, SensorInformationActivity.class);
        Button scanButton = (Button) findViewById(R.id.scanButtonId);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(scanStartIntent);
            }
        });
    }

    public void enableFingerprinting(){
        final Intent fingerprintingIntent = new Intent(this, FingerprintingScreenActivity.class);
        Button mFingerprintingButton = findViewById(R.id.fingerprintButtonId);
        mFingerprintingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(fingerprintingIntent);
            }
        });
    }

    public void startProximity(){
        final Intent startProximityIntent = new Intent(this, ProximityScreenActivity.class);
        Button mProximityButton = findViewById(R.id.proximityButtonId);
        mProximityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(startProximityIntent);
            }
        });
    }

    public void startTrilateration(){
        final Intent mStartTrilaterationIntent = new Intent(this, TrilaterationScreenActivity.class);
        Button mTrilaterationButton = findViewById(R.id.trilaterationButtonId);
        mTrilaterationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mStartTrilaterationIntent);
            }
        });
    }

}
