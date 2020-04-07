package android.example.findlocation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

public class FifthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_wifi:
                Toast.makeText(this,"Wi-Fi selected",Toast.LENGTH_SHORT).show();
                break;
            case R.id.checkbox_bluetooth:
                Toast.makeText(this,"Bluetooth selected",Toast.LENGTH_SHORT).show();
                break;
            case R.id.checkbox_device_sensors:
                Toast.makeText(this,"Device Data selected",Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this,"Error in the selection",Toast.LENGTH_SHORT).show();
        }
    }
}
