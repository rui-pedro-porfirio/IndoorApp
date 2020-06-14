package android.example.findlocation.activities;

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
    }

    public void startScanningData(){
        final Intent scanStartIntent = new Intent(this, ProximityDistanceScanActivity.class);
        Button scanButton = (Button) findViewById(R.id.proximityDistanceScanButtonId);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(scanStartIntent);
            }
        });
    }
}