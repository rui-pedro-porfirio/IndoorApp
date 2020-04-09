package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.example.findlocation.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Needs Offline and Online Phase buttons
(Offline phase)Needs filling scrolling, start button and finish button
(Online phase)Needs current data window and position detected
 */
public class FingerprintingActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprinting);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        listenForOfflineButton();
        listenForOnlineButton();
    }

    public void listenForOfflineButton(){
        final Intent offlinePhaseIntent = new Intent(this, OfflineTabedActivity.class);
        Button offlineButton = findViewById(R.id.offlineButtonId);
        offlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(offlinePhaseIntent);
            }
        });
    }

    public void listenForOnlineButton(){
        final Intent onlinePhaseIntent = new Intent(this, OfflineTabedActivity.class);
        Button onlineButton = findViewById(R.id.onlineButtonId);
        onlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(onlinePhaseIntent);
            }
        });
    }

}
