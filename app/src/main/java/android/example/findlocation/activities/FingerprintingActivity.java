package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.example.findlocation.R;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Needs Offline and Online Phase buttons
(Offline phase)Needs filling scrolling, start button and finish button
(Online phase)Needs current data window and position detected
 */
public class FingerprintingActivity extends AppCompatActivity {

    private List<String> dataTypes;
    private Map<String,Integer> preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprinting);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dataTypes = getIntent().getStringArrayListExtra("dataTypes");
        preferences = (HashMap<String,Integer>) getIntent().getSerializableExtra("Preferences");
    }


}
