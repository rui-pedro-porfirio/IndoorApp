package android.example.findlocation.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.WifiObject;
import android.graphics.Color;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GraphicalWiFiAdapter extends
        RecyclerView.Adapter<GraphicalWiFiAdapter.GraphicalSensorViewHolder>{

    private final LinkedList<WifiObject> mAccessPointList;
    private LayoutInflater mInflater;

    public static final int SCAN_TIME = 10;
    public static final String DEVICE_SENSOR_FILE = "sensorData";

    public GraphicalWiFiAdapter(Context context, LinkedList<WifiObject> mAccessPointList) {
        mInflater = LayoutInflater.from(context);
        this.mAccessPointList = mAccessPointList;
        writeToFile(DEVICE_SENSOR_FILE,"");
    }

    @NonNull
    @Override
    public GraphicalWiFiAdapter.GraphicalSensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.cardviewsensor_graphic_layout,
                parent, false);
        return new GraphicalSensorViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull GraphicalWiFiAdapter.GraphicalSensorViewHolder holder, int position) {
        WifiObject mCurrentSensor = mAccessPointList.get(position);
        holder.sensorNameView.setText(mCurrentSensor.getName());
        writeToFile(DEVICE_SENSOR_FILE,mCurrentSensor.getName());
        computeDeviceGraphicalRepresentation(holder,mCurrentSensor.getValues());
    }

    public void writeToFile(String sFileName,String sBody){
        try{
            File root = new File(Environment.getExternalStorageDirectory(), "Sensor Data");
            // if external memory exists and folder with name Notes
            if (!root.exists()) {
                root.mkdirs(); // this will create folder.
            }
            File filepath = new File(root, sFileName + ".txt");  // file path to save
            Writer output = new BufferedWriter(new FileWriter(filepath, true));
            output.append(sBody+"\n");
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getItemCount() {
        return mAccessPointList.size();
    }

    public void computeDeviceGraphicalRepresentation(@NonNull GraphicalWiFiAdapter.GraphicalSensorViewHolder holder, List<Integer> data){
        List<DataPoint> dataPointsX = new ArrayList<DataPoint>(SCAN_TIME);
        int dataPerSecond = data.size() / SCAN_TIME;
        int lookupValue = dataPerSecond;
        double averageValueX = 0.0;
        dataPointsX.add(new DataPoint(0,0));
        int seconds = 1;
        double sumX = 0.0;

        for(int i = 0; i < data.size();i++){
            double value = data.get(i);
            sumX += value;
            if(dataPerSecond < 1 && i == data.size()-1){
                averageValueX = sumX / data.size();
                String toAdd = "RSS Wi-Fi: " + averageValueX;
                writeToFile(DEVICE_SENSOR_FILE,toAdd);
                dataPointsX.add(new DataPoint(seconds, averageValueX));
                sumX = 0.0;
                seconds++;
                lookupValue = i + dataPerSecond;
                if(seconds < SCAN_TIME){
                    for(int j = seconds; j <= SCAN_TIME;j++){
                        toAdd = "RSS Wi-Fi: " + averageValueX;
                        writeToFile(DEVICE_SENSOR_FILE,toAdd);
                        dataPointsX.add(new DataPoint(j, averageValueX));
                    }
                    seconds = SCAN_TIME;
                }
            }
            else {
                if (i == (lookupValue - 1)) {
                    averageValueX = sumX / dataPerSecond;
                    String toAdd = "RSS Wi-Fi: " + averageValueX;
                    writeToFile(DEVICE_SENSOR_FILE,toAdd);
                    dataPointsX.add(new DataPoint(seconds, averageValueX));
                    sumX = 0.0;
                    seconds++;
                    lookupValue = i + dataPerSecond;
                }
            }
        }

        LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<DataPoint>(dataPointsX.toArray(new DataPoint[dataPointsX.size()]));

        // set manual X bounds
        holder.graph.getViewport().setXAxisBoundsManual(true);
        holder.graph.getViewport().setMinX(0);
        holder.graph.getViewport().setMaxX(seconds);
        seriesX.setTitle("X Value");
        seriesX.setColor(Color.argb(255,255,0,0));
        holder.graph.addSeries(seriesX);

    }

    class GraphicalSensorViewHolder extends RecyclerView.ViewHolder {


        public final TextView sensorNameView;
        public final GraphView graph;
        final GraphicalWiFiAdapter mAdapter;

        public GraphicalSensorViewHolder(View itemView, GraphicalWiFiAdapter adapter) {
            super(itemView);
            sensorNameView = itemView.findViewById(R.id.graph_sensor_name);
            graph = itemView.findViewById(R.id.graph);
            this.mAdapter = adapter;
        }

    }
}