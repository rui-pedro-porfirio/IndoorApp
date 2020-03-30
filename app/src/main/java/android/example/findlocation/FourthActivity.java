package android.example.findlocation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonWriter;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class FourthActivity extends AppCompatActivity {

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

    private ObjectMapper mapper;
    private JsonWriter writer;
    public static final String DEVICE_SENSOR_FILE = "sensorData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //return button on the action bar
        String type = getIntent().getStringExtra("Type");
        mapper = new ObjectMapper();
        if(type.equals("Scan")) {

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
                writeJsonStream(new FileOutputStream(writeToFile(DEVICE_SENSOR_FILE)),mSensorInformationList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void writeJsonStream(OutputStream out, List<SensorObject> sensorData) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeMessagesArray(writer, sensorData);
        writer.close();
    }

    public void writeMessagesArray(JsonWriter writer, List<SensorObject> sensorData) throws IOException {
        writer.beginArray();
        for (SensorObject message : sensorData) {
            writeMessage(writer, message);
        }
        writer.endArray();
    }

    public void writeMessage(JsonWriter writer, SensorObject message) throws IOException {
        writer.beginObject();
        writer.name("sensorName").value(message.getName());
        writer.name("samples");
        writeListArray(writer,message.getScannedValues());
        writer.endObject();
    }


    public void writeListArray(JsonWriter writer, List<List<Float>> scannedValues) throws IOException {
        writer.beginArray();
        for (int i = 0; i < scannedValues.size();i++) {
            writer.beginObject();
            writer.name("Value").value(i);
            writer.name("values");
            writer.beginArray();
            for (Float f: scannedValues.get(i)
                 ) {
                writer.value(f);
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endArray();
    }

    public File writeToFile(String sFileName){

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

