package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.example.findlocation.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FingerprintingCheckboxActivity extends AppCompatActivity {

    private List<String> dataTypes;
    private Map<String,Integer> preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dataTypes = new ArrayList<String>();
        preferences = new HashMap<String,Integer>();
        sendDataToActivity();
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        Button mNextButton = findViewById(R.id.nextId);

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_wifi:
                if(checked) {
                    dataTypes.add("Wi-fi");
                    Toast.makeText(this, "Wi-Fi selected", Toast.LENGTH_SHORT).show();
                    mNextButton.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.checkbox_bluetooth:
                if(checked) {
                    dataTypes.add("Bluetooth");
                    Toast.makeText(this, "Bluetooth selected", Toast.LENGTH_SHORT).show();
                    mNextButton.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.checkbox_device_sensors:
                if(checked) {
                    dataTypes.add("DeviceData");
                    Toast.makeText(this, "Device Data selected", Toast.LENGTH_SHORT).show();
                    mNextButton.setVisibility(View.VISIBLE);
                }
                break;
            default:
                Toast.makeText(this,"Error in the selection",Toast.LENGTH_SHORT).show();
        }
    }

    public void sendDataToActivity(){

        final Intent mFingerprintScreenIntent = new Intent(this, FingerprintingActivity.class);
        Button mNextButton = findViewById(R.id.nextId);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerprintScreenIntent.putStringArrayListExtra("dataTypes", (ArrayList<String>) dataTypes);

                EditText mNumberOfFingerprints = findViewById(R.id.fingerprintPerLocationNumberId);
                preferences.put("Number of Fingerprints",Integer.valueOf(mNumberOfFingerprints.getText().toString()));
                EditText mTimeBetweenFingerprints = findViewById(R.id.intervalBetweenFingerprintsNumberId);
                preferences.put("Time between Fingerprints",Integer.valueOf(mTimeBetweenFingerprints.getText().toString()));
                mFingerprintScreenIntent.putExtra("Preferences",(Serializable) preferences);
                startActivity(mFingerprintScreenIntent);
            }
        });
    }
}
