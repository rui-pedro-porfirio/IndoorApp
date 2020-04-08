package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.example.findlocation.R;
import android.example.findlocation.adapters.GraphicalBluetoothAdapter;
import android.example.findlocation.adapters.GraphicalSensorAdapter;
import android.example.findlocation.adapters.GraphicalWiFiAdapter;
import android.example.findlocation.objects.BluetoothObject;
import android.example.findlocation.objects.SensorObject;
import android.example.findlocation.objects.WifiObject;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonWriter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class GraphicalSensorInformationActivity extends AppCompatActivity {

    //Map with long scan results
    private Map<String, List<Integer>> wifiResults;
    private Map<String, List<List<Float>>> deviceResults;
    private Map<String, List<Integer>> bluetoothResults;

    private LinkedList<SensorObject> mSensorInformationList;
    private LinkedList<BluetoothObject> mBeaconsList;
    private LinkedList<WifiObject> mAccessPoints;

    private RecyclerView mRecyclerView;
    private RecyclerView bluetoothRecyclerView;
    private RecyclerView wifiRecyclerView;

    //Adapters
    private GraphicalSensorAdapter mAdapter;
    private GraphicalBluetoothAdapter bluetoothAdapter;
    private GraphicalWiFiAdapter wifiAdapter;

    public static final String DEVICE_SENSOR_FILE = "sensorData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar
        String type = getIntent().getStringExtra("Type");
        if (type.equals("Scan")) {

            wifiResults = new HashMap<>();
            deviceResults = new HashMap<>();
            bluetoothResults = new HashMap<>();
            mSensorInformationList = new LinkedList<>();
            mBeaconsList = new LinkedList<>();
            mAccessPoints = new LinkedList<>();
            deviceResults = (HashMap) getIntent().getSerializableExtra("Device Data");
            wifiResults = (HashMap) getIntent().getSerializableExtra("WiFi Data");
            bluetoothResults = (HashMap) getIntent().getSerializableExtra("Bluetooth Data");
            getAvailableDeviceSensors();
            getBluetoothData();
            getWifiData();
            initDeviceSensorRecycleView();
            initBluetoothSensorRecycleView();
            initWifiSensorRecycleView();
            try {
                writeJsonStreamSensorData(new FileOutputStream(writeToFile(DEVICE_SENSOR_FILE), true), mSensorInformationList);
                writeJsonStreamBLE(new FileOutputStream(writeToFile(DEVICE_SENSOR_FILE), true), mBeaconsList);
                writeJsonStreamWiFi(new FileOutputStream(writeToFile(DEVICE_SENSOR_FILE), true), mAccessPoints);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeJsonStreamSensorData(OutputStream out, List<SensorObject> values) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArraySensorData(writer, values);
        writer.close();
    }

    public void writeMessagesArraySensorData(JsonWriter writer, List<SensorObject> values) throws IOException {
        writer.beginObject();
        writer.name("Device Sensors");
        writer.beginArray();
        for (SensorObject sensor : values
        ) {
            writeMessageSensorData(writer, sensor);
        }
        writer.endArray();
        writer.endObject();
    }

    public void writeMessageSensorData(JsonWriter writer, SensorObject sensor) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(sensor.getName());
        writer.name("samples");
        writeListArraySensorData(writer, sensor.getScannedValues());
        writer.endObject();
    }


    public void writeListArraySensorData(JsonWriter writer, List<List<Float>> scannedValues) throws IOException {
        writer.beginArray();
        for (int i = 0; i < scannedValues.size(); i++) {
            writer.beginObject();
            writer.name("Sample").value(i);
            writer.name("values");
            writer.beginArray();
            for (Float f : scannedValues.get(i)
            ) {
                writer.value(f);
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endArray();
    }

    public void writeJsonStreamBLE(OutputStream out, List<BluetoothObject> values) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArrayBLE(writer, values);
        writer.close();
    }

    public void writeMessagesArrayBLE(JsonWriter writer, List<BluetoothObject> values) throws IOException {
        writer.beginObject();
        writer.name("BLE");
        writer.beginArray();
        for (BluetoothObject sensor : values
        ) {
            writeMessageBLE(writer, sensor);
        }
        writer.endArray();
        writer.endObject();
    }

    public void writeMessageBLE(JsonWriter writer, BluetoothObject sensor) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(sensor.getName());
        writer.name("samples");
        writeListArrayBLE(writer, sensor.getValues());
        writer.endObject();
    }


    public void writeListArrayBLE(JsonWriter writer, List<Integer> scannedValues) throws IOException {
        writer.beginArray();
        for (int i = 0; i < scannedValues.size(); i++) {
            writer.beginObject();
            writer.name("RSSI").value(scannedValues.get(i));
            writer.endObject();
        }
        writer.endArray();
    }

    public void writeJsonStreamWiFi(OutputStream out, List<WifiObject> values) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArrayWiFi(writer, values);
        writer.close();
    }

    public void writeMessagesArrayWiFi(JsonWriter writer, List<WifiObject> values) throws IOException {
        writer.beginObject();
        writer.name("Wi-Fi");
        writer.beginArray();
        for (WifiObject sensor : values
        ) {
            writeMessageWiFi(writer, sensor);
        }
        writer.endArray();
        writer.endObject();
    }

    public void writeMessageWiFi(JsonWriter writer, WifiObject sensor) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(sensor.getName());
        writer.name("samples");
        writeListArrayWiFi(writer, sensor.getValues());
        writer.endObject();
    }


    public void writeListArrayWiFi(JsonWriter writer, List<Integer> scannedValues) throws IOException {
        writer.beginArray();
        for (int i = 0; i < scannedValues.size(); i++) {
            writer.beginObject();
            writer.name("RSSI").value(scannedValues.get(i));
            writer.endObject();
        }
        writer.endArray();
    }

    public File writeToFile(String sFileName) {

        File root = new File(Environment.getExternalStorageDirectory(), "Sensor Data");
        // if external memory exists and folder with name Notes
        if (!root.exists()) {
            root.mkdirs(); // this will create folder.
        }
        File filepath = new File(root, sFileName + ".json");  // file path to save
        return filepath;

    }

    public LinkedList<SensorObject> getAvailableDeviceSensors() {
        for (String currentSensor : deviceResults.keySet()) {
            SensorObject sensorInfo = new SensorObject(currentSensor, deviceResults.get(currentSensor));
            mSensorInformationList.add(sensorInfo);
        }
        return mSensorInformationList;
    }

    public LinkedList<BluetoothObject> getBluetoothData() {
        for (String currentValue : bluetoothResults.keySet()) {
            BluetoothObject sensorInfo = new BluetoothObject(currentValue, bluetoothResults.get(currentValue));
            mBeaconsList.add(sensorInfo);
        }
        return mBeaconsList;
    }

    public LinkedList<WifiObject> getWifiData() {
        for (String currentValue : wifiResults.keySet()) {
            WifiObject sensorInfo = new WifiObject(currentValue, wifiResults.get(currentValue));
            mAccessPoints.add(sensorInfo);
        }
        return mAccessPoints;
    }

    public void initDeviceSensorRecycleView() {

        mRecyclerView = findViewById(R.id.sensor_graphic_recyclerView);
        mAdapter = new GraphicalSensorAdapter(this, mSensorInformationList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void initBluetoothSensorRecycleView() {
        bluetoothRecyclerView = findViewById(R.id.bluetooth_graphic_recyclerView);
        bluetoothAdapter = new GraphicalBluetoothAdapter(this, mBeaconsList);
        bluetoothRecyclerView.setAdapter(bluetoothAdapter);
        bluetoothRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void initWifiSensorRecycleView() {
        wifiRecyclerView = findViewById(R.id.wifi_graphic_recyclerView);
        wifiAdapter = new GraphicalWiFiAdapter(this, mAccessPoints);
        wifiRecyclerView.setAdapter(wifiAdapter);
        wifiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

}

