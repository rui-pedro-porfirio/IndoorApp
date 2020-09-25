package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.Fingerprint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

public class FingerprintAdapter extends
        RecyclerView.Adapter<FingerprintAdapter.FingerprintViewHolder> {

    private final List<Fingerprint> mFingerprintList;
    private LayoutInflater mInflater;

    public FingerprintAdapter(Context context, List<Fingerprint> mFingerprintList) {
        mInflater = LayoutInflater.from(context);
        this.mFingerprintList = mFingerprintList;
    }

    @NonNull
    @Override
    public FingerprintAdapter.FingerprintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.cardview_fingerprint,
                parent, false);
        return new FingerprintViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull FingerprintAdapter.FingerprintViewHolder holder, int position) {
        Fingerprint mFingerprint = mFingerprintList.get(position);
        if (mFingerprint.getmSensorInformationList().size() > 0) {
            holder.sensorItemView.setText(mFingerprint.getmSensorInformationList().get(0).getName());
            holder.sensorXValueView.setText(new DecimalFormat("##.##").format(mFingerprint.getmSensorInformationList().get(0).getX_value()));
            holder.sensorYValueView.setText(new DecimalFormat("##.##").format(mFingerprint.getmSensorInformationList().get(0).getY_value()));
            holder.sensorZValueView.setText(new DecimalFormat("##.##").format(mFingerprint.getmSensorInformationList().get(0).getZ_value()));
        }
        if (mFingerprint.getmBeaconsList().size() > 0) {
            int rssi = mFingerprint.getmBeaconsList().get(0).getSingleValue();
            holder.bluetoothFirstBeaconView.setText(String.valueOf(rssi));
        }
        if (mFingerprint.getmAccessPoints().size() > 0) {
            int rssi = mFingerprint.getmAccessPoints().get(0).getSingleValue();
            holder.wifiFirstAccessPointView.setText(String.valueOf(rssi));
        }
    }

    @Override
    public int getItemCount() {
        return mFingerprintList.size();
    }


    class FingerprintViewHolder extends RecyclerView.ViewHolder {

        public final TextView sensorItemView;
        public final TextView sensorXValueView;
        public final TextView sensorYValueView;
        public final TextView sensorZValueView;
        public final TextView bluetoothFirstBeaconView;
        public final TextView wifiFirstAccessPointView;
        final FingerprintAdapter mAdapter;

        public FingerprintViewHolder(View itemView, FingerprintAdapter adapter) {
            super(itemView);
            sensorItemView = itemView.findViewById(R.id.sensor_name_radiomap);
            sensorXValueView = itemView.findViewById(R.id.sensor_x_value_radiomap);
            sensorYValueView = itemView.findViewById(R.id.sensor_y_value_radiomap);
            sensorZValueView = itemView.findViewById(R.id.sensor_z_value_radiomap);
            bluetoothFirstBeaconView = itemView.findViewById(R.id.ble_value);
            wifiFirstAccessPointView = itemView.findViewById(R.id.wifi_value);
            this.mAdapter = adapter;
        }

    }
}

