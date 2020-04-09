package android.example.findlocation.activities;

import android.example.findlocation.R;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.example.findlocation.ui.main.SectionsPagerAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineTabedActivity extends AppCompatActivity {

    private List<String> dataTypes;
    private Map<String,Integer> preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_tabed);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        tabs.getTabAt(0).setIcon(R.drawable.fingerprinticon);
        tabs.getTabAt(1).setIcon(R.drawable.radiomapicon);
        tabs.getTabAt(2).setIcon(R.drawable.preferencesicon);
        dataTypes = new ArrayList<String>();
        preferences = new HashMap<String,Integer>();
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();


        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_wifi:
                if(checked) {
                    dataTypes.add("Wi-fi");
                    Toast.makeText(this, "Wi-Fi selected", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.checkbox_bluetooth:
                if(checked) {
                    dataTypes.add("Bluetooth");
                    Toast.makeText(this, "Bluetooth selected", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.checkbox_device_sensors:
                if(checked) {
                    dataTypes.add("DeviceData");
                    Toast.makeText(this, "Device Data selected", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Toast.makeText(this,"Error in the selection",Toast.LENGTH_SHORT).show();
        }
    }

    public void setPreferences(Map<String,Integer> preferences){
        this.preferences = preferences;
    }
}