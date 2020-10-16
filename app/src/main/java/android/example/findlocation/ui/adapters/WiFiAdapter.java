package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.objects.client.WifiObject;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WiFiAdapter extends
        RecyclerView.Adapter<WiFiAdapter.WifiViewHolder> {


    private final List<WifiObject> mAccessPointsList;
    private final LayoutInflater mInflater;

    public WiFiAdapter(Context context, List<WifiObject> mAccessPointsList) {
        mInflater = LayoutInflater.from(context);
        this.mAccessPointsList = mAccessPointsList;
    }

    @NonNull
    @Override
    public WiFiAdapter.WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.cardviewwifi,
                parent, false);
        return new WiFiAdapter.WifiViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull WiFiAdapter.WifiViewHolder holder, int position) {
        WifiObject mCurrentAccessPoint = mAccessPointsList.get(position);
        holder.wifiSSIDView.setText("SSID: " + mCurrentAccessPoint.getName());
        holder.wifiRSSIView.setText("RSSI: " + mCurrentAccessPoint.getSingleValue());
    }

    @Override
    public int getItemCount() {
        return mAccessPointsList.size();
    }


    class WifiViewHolder extends RecyclerView.ViewHolder {

        public final TextView wifiSSIDView;
        public final TextView wifiRSSIView;
        final WiFiAdapter mAdapter;

        public WifiViewHolder(View itemView, WiFiAdapter adapter) {
            super(itemView);
            wifiSSIDView = itemView.findViewById(R.id.rssi_identifier_wifi);
            wifiRSSIView = itemView.findViewById(R.id.rssi_value_wifi);
            this.mAdapter = adapter;
        }

    }
}