package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.BluetoothObject;
import android.example.findlocation.objects.client.Fingerprint;
import android.example.findlocation.objects.client.WifiObject;
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
        if (mFingerprint.getmBeaconsList().size() > 0) {
            int rssi = getAverageRSSIBLE(mFingerprint.getmBeaconsList());
            holder.bluetoothFirstBeaconView.setText(String.valueOf(rssi));
        }
        if (mFingerprint.getmAccessPoints().size() > 0) {
            int rssi = getAverageRSSIWiFi(mFingerprint.getmAccessPoints());
            holder.wifiFirstAccessPointView.setText(String.valueOf(rssi));
        }
    }

    private int getAverageRSSIBLE(List<BluetoothObject> beaconsList){
        int sum = 0;
        for(BluetoothObject beacon : beaconsList){
            sum += beacon.getSingleValue();
        }
        return sum / beaconsList.size();
    }

    private int getAverageRSSIWiFi(List<WifiObject> apList){
        int sum = 0;
        for(WifiObject ap : apList){
            sum += ap.getSingleValue();
        }
        return sum / apList.size();
    }

    @Override
    public int getItemCount() {
        return mFingerprintList.size();
    }


    class FingerprintViewHolder extends RecyclerView.ViewHolder {

        public final TextView bluetoothFirstBeaconView;
        public final TextView wifiFirstAccessPointView;
        final FingerprintAdapter mAdapter;

        public FingerprintViewHolder(View itemView, FingerprintAdapter adapter) {
            super(itemView);
            bluetoothFirstBeaconView = itemView.findViewById(R.id.ble_value);
            wifiFirstAccessPointView = itemView.findViewById(R.id.wifi_value);
            this.mAdapter = adapter;
        }

    }
}

