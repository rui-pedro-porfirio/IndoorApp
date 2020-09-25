package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.SensorObject;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GraphicalSensorAdapter extends
        RecyclerView.Adapter<GraphicalSensorAdapter.GraphicalSensorViewHolder>{

    private LinkedList<SensorObject> mSensorInformationList;
    private LayoutInflater mInflater;

    public static final int SCAN_TIME = 10;

    public static final String DEVICE_SENSOR_FILE = "sensorData";
    private ObjectMapper mapper;

    public GraphicalSensorAdapter(Context context, LinkedList<SensorObject> mSensorInformationList) {
        mInflater = LayoutInflater.from(context);
        mapper = new ObjectMapper();
        this.mSensorInformationList = mSensorInformationList;
    }

    @NonNull
    @Override
    public GraphicalSensorAdapter.GraphicalSensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.cardviewsensor_graphic_layout,
                parent, false);
        return new GraphicalSensorViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull GraphicalSensorAdapter.GraphicalSensorViewHolder holder, int position) {
        SensorObject mCurrentSensor = mSensorInformationList.get(position);
        holder.sensorNameView.setText(mCurrentSensor.getName());
        computeDeviceGraphicalRepresentation(holder,mCurrentSensor.getScannedValues());

    }

    @Override
    public int getItemCount() {
        return mSensorInformationList.size();
    }

    public void computeDeviceGraphicalRepresentation(@NonNull GraphicalSensorAdapter.GraphicalSensorViewHolder holder, List<List<Float>> data){
        List<DataPoint> dataPointsX = new ArrayList<DataPoint>(SCAN_TIME);
        List<DataPoint> dataPointsY = new ArrayList<DataPoint>(SCAN_TIME);
        List<DataPoint> dataPointsZ = new ArrayList<DataPoint>(SCAN_TIME);
        int dataPerSecond = data.size() / SCAN_TIME;
        int lookupValue = dataPerSecond;
        double averageValueX = 0.0;
        double averageValueY = 0.0;
        double averageValueZ = 0.0;
        dataPointsX.add(new DataPoint(0,0));
        dataPointsY.add(new DataPoint(0,0));
        dataPointsZ.add(new DataPoint(0,0));
        int seconds = 1;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;

        for(int i = 0; i < data.size();i++){
            if(data.get(i).size() == 3){
                double xValue = data.get(i).get(0);
                double yValue = data.get(i).get(1);
                double zValue = data.get(i).get(2);
                sumX += xValue;
                sumY += yValue;
                sumZ += zValue;
                if(i == (lookupValue -1)) {
                    averageValueX = sumX / dataPerSecond;
                    averageValueY = sumY / dataPerSecond;
                    averageValueZ = sumZ / dataPerSecond;
                    dataPointsX.add(new DataPoint(seconds, averageValueX));
                    dataPointsY.add(new DataPoint(seconds, averageValueY));
                    dataPointsZ.add(new DataPoint(seconds, averageValueZ));
                    sumX = 0.0;
                    sumY = 0.0;
                    sumZ = 0.0;
                    seconds++;
                    lookupValue = i + dataPerSecond;
                }
            }
            else if(data.get(i).size() == 2){
                double xValue = data.get(i).get(0);
                double yValue = data.get(i).get(1);
                    sumX += xValue;
                    sumY += yValue;
                    if(i == (lookupValue -1)) {
                        averageValueX = sumX / dataPerSecond;
                        averageValueY = sumY / dataPerSecond;
                        dataPointsX.add(new DataPoint(seconds, averageValueX));
                        dataPointsY.add(new DataPoint(seconds, averageValueY));
                        sumX = 0.0;
                        sumY = 0.0;
                        seconds++;
                        lookupValue = i + dataPerSecond;
                    }
            }
            else{
                double xValue = data.get(i).get(0);
                        sumX += xValue;
                        if(i == (lookupValue -1)) {
                            averageValueX = sumX / dataPerSecond;
                            dataPointsX.add(new DataPoint(seconds, averageValueX));
                            sumX = 0.0;
                            sumY = 0.0;
                            seconds++;
                            lookupValue = i + dataPerSecond;
                        }
            }

        }

        LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<DataPoint>(dataPointsX.toArray(new DataPoint[dataPointsX.size()]));
        LineGraphSeries<DataPoint> seriesY = new LineGraphSeries<DataPoint>(dataPointsY.toArray(new DataPoint[dataPointsY.size()]));
        LineGraphSeries<DataPoint> seriesZ = new LineGraphSeries<DataPoint>(dataPointsZ.toArray(new DataPoint[dataPointsZ.size()]));

        // set manual X bounds
        holder.graph.getViewport().setXAxisBoundsManual(true);
        holder.graph.getViewport().setMinX(0);
        holder.graph.getViewport().setMaxX(seconds);
        seriesX.setTitle("X Value");
        seriesX.setColor(Color.argb(255,255,0,0));
        seriesY.setTitle("Y Value");
        seriesY.setColor(Color.argb(255,0,255,0));
        seriesZ.setTitle("Z Value");
        seriesZ.setColor(Color.argb(255,0,0,255));
        holder.graph.addSeries(seriesX);
        holder.graph.addSeries(seriesY);
        holder.graph.addSeries(seriesZ);

    }

    class GraphicalSensorViewHolder extends RecyclerView.ViewHolder {


        public final TextView sensorNameView;
        public final GraphView graph;
        final GraphicalSensorAdapter mAdapter;

        public GraphicalSensorViewHolder(View itemView, GraphicalSensorAdapter adapter) {
            super(itemView);
            sensorNameView = itemView.findViewById(R.id.graph_sensor_name);
            graph = itemView.findViewById(R.id.graph);
            this.mAdapter = adapter;
        }

    }
}