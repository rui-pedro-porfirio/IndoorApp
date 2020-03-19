package android.example.findlocation;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.DataPoint;


public class FourthActivity extends AppCompatActivity {

    //Map with long scan results
    private Map<String, List<Integer>> wifiResults;
    private Map<String, List<float[]>> deviceResults;
    private Map<String, List<Integer>> bluetoothResults;

    public static final int SCAN_TIME = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);
        String type = getIntent().getStringExtra("Type");
        wifiResults = new HashMap<>();
        deviceResults = new HashMap<>();
        bluetoothResults = new HashMap<>();
        if(type.equals("Scan")) {
            deviceResults = (HashMap) getIntent().getSerializableExtra("Device Data");
            wifiResults = (HashMap) getIntent().getSerializableExtra("WiFi Data");
            bluetoothResults = (HashMap) getIntent().getSerializableExtra("Bluetooth Data");
        }
        computeGraphicalRepresentation();
    }

    //Try GraphView
    public void computeGraphicalRepresentation(){
        GraphView graph = (GraphView) findViewById(R.id.graph);
        List<float[]> accelerometerData = deviceResults.get("MIR3DA Accelerometer");
        List<DataPoint> dataPoints = new ArrayList<DataPoint>(SCAN_TIME);
        int dataPerSecond = accelerometerData.size() / SCAN_TIME; //10
        int lookupValue = dataPerSecond;
        double averageValue = 0.0;
        int seconds = 1;
        double sum = 0.0;
        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(SCAN_TIME);

        for(int i = 0; i < accelerometerData.size();i++){
            double yValue = accelerometerData.get(i)[0];
            sum += yValue;
            if(i == (lookupValue -1)){
                averageValue = sum / dataPerSecond;
                dataPoints.add(new DataPoint(seconds,averageValue));
                sum = 0.0;
                seconds++;
                lookupValue = i + dataPerSecond;
            }
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints.toArray(new DataPoint[dataPoints.size()]));
        graph.addSeries(series);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

    }

    private float maxValue(List<float[]> data,int position){
        float max = data.get(0)[position];
        for(int i = 1; i < data.size();i++){
            if(data.get(i)[position] >= max)
                max = data.get(i)[position];
        }
        return max;
    }
}
