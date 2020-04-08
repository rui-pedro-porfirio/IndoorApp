package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.example.findlocation.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class FingerprintingCheckboxActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        Button mNextButton = findViewById(R.id.nextId);

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_wifi:
                if(checked) {
                    Toast.makeText(this, "Wi-Fi selected", Toast.LENGTH_SHORT).show();
                    mNextButton.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.checkbox_bluetooth:
                if(checked) {
                    Toast.makeText(this, "Bluetooth selected", Toast.LENGTH_SHORT).show();
                    mNextButton.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.checkbox_device_sensors:
                if(checked) {
                    Toast.makeText(this, "Device Data selected", Toast.LENGTH_SHORT).show();
                    mNextButton.setVisibility(View.VISIBLE);
                }
                break;
            default:
                Toast.makeText(this,"Error in the selection",Toast.LENGTH_SHORT).show();
        }
    }
}
