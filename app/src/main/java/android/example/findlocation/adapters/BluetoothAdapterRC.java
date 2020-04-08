package android.example.findlocation.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.altbeacon.beacon.Beacon;

import java.util.LinkedList;

public class BluetoothAdapterRC extends
        RecyclerView.Adapter<BluetoothAdapterRC.BluetoothViewHolder>{

    private final LinkedList<Beacon> mSensorInformationList;
    private LayoutInflater mInflater;

    public BluetoothAdapterRC(Context context, LinkedList<Beacon> mSensorInformationList) {
        mInflater = LayoutInflater.from(context);
        this.mSensorInformationList = mSensorInformationList;
    }

    @NonNull
    @Override
    public BluetoothAdapterRC.BluetoothViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.cardviewbluetooth,
                parent, false);
        return new BluetoothAdapterRC.BluetoothViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothAdapterRC.BluetoothViewHolder holder, int position) {
        Beacon mCurrentSensor = mSensorInformationList.get(position);
        holder.bluetoothItemView.setText("Major: " + mCurrentSensor.getId2().toString() + " - Minor: " + mCurrentSensor.getId3());
        holder.bluetoothValueView.setText("RSSI: " + String.valueOf(mCurrentSensor.getRssi()));
    }

    @Override
    public int getItemCount() {
        return mSensorInformationList.size();
    }


    class BluetoothViewHolder extends RecyclerView.ViewHolder {

        public final TextView bluetoothItemView;
        public final TextView bluetoothValueView;
        final BluetoothAdapterRC mAdapter;

        public BluetoothViewHolder(View itemView, BluetoothAdapterRC adapter) {
            super(itemView);
            bluetoothItemView = itemView.findViewById(R.id.rssi_identifier);
            bluetoothValueView = itemView.findViewById(R.id.rssi_value);
            this.mAdapter = adapter;
        }

    }
}
