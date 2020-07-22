package android.example.findlocation.activities.proximity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.example.findlocation.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ProximityScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_proximity_screen);
        startScanningData();
        startOnlinePhase();
    }

    public void startScanningData(){
        final Intent mScanStartIntent = new Intent(this, ProximityDistanceScanActivity.class);
        Button mScanButton = (Button) findViewById(R.id.proximityDistanceScanButtonId);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mScanStartIntent);
            }
        });
    }

    public void startOnlinePhase(){
        final Intent mStartOnlinePhaseIntent = new Intent(this, ProximityOnlineActivity.class);
        Button mOnlineTrilaterationButton = (Button) findViewById(R.id.proximityStartProximityButtonId);
        mOnlineTrilaterationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mStartOnlinePhaseIntent);
            }
        });
    }
}