package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothObject;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.altbeacon.beacon.Beacon;

import java.util.LinkedList;
import java.util.List;

public class BluetoothAdapterRC extends
        RecyclerView.Adapter<BluetoothAdapterRC.BluetoothViewHolder>{

    private final List<BluetoothObject> mBeaconsList;
    private LayoutInflater mInflater;

    public BluetoothAdapterRC(Context context, List<BluetoothObject> mBeaconsList) {
        mInflater = LayoutInflater.from(context);
        this.mBeaconsList = mBeaconsList;
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
        BluetoothObject mBeacon = mBeaconsList.get(position);
        holder.bluetoothItemView.setText("Name: " + mBeacon.getName());
        holder.bluetoothValueView.setText("RSSI: " + mBeacon.getSingleValue());
    }

    @Override
    public int getItemCount() {
        return mBeaconsList.size();
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
