package android.example.findlocation.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.SensorObject;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.LinkedList;

public class SensorAdapter extends
        RecyclerView.Adapter<SensorAdapter.SensorViewHolder>{

    private final LinkedList<SensorObject> mSensorInformationList;
    private LayoutInflater mInflater;

    public SensorAdapter(Context context,LinkedList<SensorObject> mSensorInformationList) {
        mInflater = LayoutInflater.from(context);
        this.mSensorInformationList = mSensorInformationList;
    }

    @NonNull
    @Override
    public SensorAdapter.SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.cardview_layout,
                parent, false);
        return new SensorViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorAdapter.SensorViewHolder holder, int position) {
        SensorObject mCurrentSensor = mSensorInformationList.get(position);
        holder.sensorItemView.setText(mCurrentSensor.getName());
        holder.sensorXValueView.setText("x:" + new DecimalFormat("##.##").format(mCurrentSensor.getX_value()));
        holder.sensorYValueView.setText("y:" + new DecimalFormat("##.##").format(mCurrentSensor.getY_value()));
        holder.sensorZValueView.setText("z:"+new DecimalFormat("##.##").format(mCurrentSensor.getZ_value()));
    }

    @Override
    public int getItemCount() {
        return mSensorInformationList.size();
    }


    class SensorViewHolder extends RecyclerView.ViewHolder {

        public final TextView sensorItemView;
        public final TextView sensorXValueView;
        public final TextView sensorYValueView;
        public final TextView sensorZValueView;
        final SensorAdapter mAdapter;

        public SensorViewHolder(View itemView, SensorAdapter adapter) {
            super(itemView);
            sensorItemView = itemView.findViewById(R.id.sensor_name);
            sensorXValueView = itemView.findViewById(R.id.sensor_x_value);
            sensorYValueView = itemView.findViewById(R.id.sensor_y_value);
            sensorZValueView = itemView.findViewById(R.id.sensor_z_value);
            this.mAdapter = adapter;
        }

    }
}

