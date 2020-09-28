package android.example.findlocation.ui.activities.main;

import android.content.Intent;
import android.example.findlocation.R;
import android.example.findlocation.ui.activities.fingerprinting.FingerprintingScreenActivity;
import android.example.findlocation.ui.activities.proximity.ProximityScreenActivity;
import android.example.findlocation.ui.activities.sensors.SensorAnalysisActivity;
import android.example.findlocation.ui.activities.trilateration.TrilaterationScreenActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Screen with options "Analysis" and "Find location of device"
 */
public class ExperimentScreenActivity extends AppCompatActivity {

    private static final String TAG = ExperimentScreenActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment_screen);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        startButtonListeners();
    }

    protected void startButtonListeners() {
        handleSensorAnalysisButton();
        handleFingerprintingButton();
        handleProximityButton();
        handleTrilaterationButton();
    }

    public void handleSensorAnalysisButton() {
        final Intent mStartSensorAnalysisIntent = new Intent(this, SensorAnalysisActivity.class);
        Button mAnalysisButton = (Button) findViewById(R.id.button_analysisButton);
        mAnalysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicked on Analysis button.");
                startActivity(mStartSensorAnalysisIntent);
            }
        });
    }

    public void handleFingerprintingButton() {
        final Intent mStartFingerprintingAnalysisIntent = new Intent(this, FingerprintingScreenActivity.class);
        Button mFingerprintingButton = findViewById(R.id.button_fingerprintingButton);
        mFingerprintingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicked on Fingerprinting button.");
                startActivity(mStartFingerprintingAnalysisIntent);
            }
        });
    }

    public void handleProximityButton() {
        final Intent mStartProximityAnalysisIntent = new Intent(this, ProximityScreenActivity.class);
        Button mProximityButton = findViewById(R.id.button_proximityButton);
        mProximityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicked on Proximity button.");
                startActivity(mStartProximityAnalysisIntent);
            }
        });
    }

    public void handleTrilaterationButton() {
        final Intent mStartTrilaterationAnalysisIntent = new Intent(this, TrilaterationScreenActivity.class);
        Button mTrilaterationButton = findViewById(R.id.button_trilaterationButton);
        mTrilaterationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicked on Trilateration button.");
                startActivity(mStartTrilaterationAnalysisIntent);
            }
        });
    }

}
