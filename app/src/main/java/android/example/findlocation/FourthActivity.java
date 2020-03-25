package android.example.findlocation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.DataPoint;


public class FourthActivity extends AppCompatActivity {

    //Map with long scan results
    private Map<String, List<Integer>> wifiResults;
    private Map<String, List<List<Float>>> deviceResults;
    private Map<String, List<Integer>> bluetoothResults;

    private LinkedList<SensorObject> mSensorInformationList;

    private RecyclerView mRecyclerView;
    private GraphicalSensorAdapter mAdapter;

    public static final int SCAN_TIME = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar
        String type = getIntent().getStringExtra("Type");
        if(type.equals("Scan")) {

            wifiResults = new HashMap<>();
            deviceResults = new HashMap<>();
            bluetoothResults = new HashMap<>();
            mSensorInformationList = new LinkedList<>();
            deviceResults = (HashMap) getIntent().getSerializableExtra("Device Data");
            wifiResults = (HashMap) getIntent().getSerializableExtra("WiFi Data");
            bluetoothResults = (HashMap) getIntent().getSerializableExtra("Bluetooth Data");
            getAvailableDeviceSensors();
            initDeviceSensorRecycleView();
        }

    }

    public LinkedList<SensorObject> getAvailableDeviceSensors() {
        for (String currentSensor : deviceResults.keySet()) {
            SensorObject sensorInfo = new SensorObject(currentSensor, deviceResults.get(currentSensor));
            mSensorInformationList.add(sensorInfo);
        }
        return mSensorInformationList;
    }

    public void initDeviceSensorRecycleView() {

        mRecyclerView = findViewById(R.id.sensor_graphic_recyclerView);
        mAdapter = new GraphicalSensorAdapter(this, mSensorInformationList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    }

