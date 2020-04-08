package android.example.findlocation.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WiFiAdapter extends
            RecyclerView.Adapter<WiFiAdapter.WifiViewHolder>{


        private final List<ScanResult> mSensorInformationList;
        private LayoutInflater mInflater;

        public WiFiAdapter(Context context,  List<ScanResult> mSensorInformationList) {
            mInflater = LayoutInflater.from(context);
            this.mSensorInformationList = mSensorInformationList;
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
            ScanResult mCurrentSensor = mSensorInformationList.get(position);
            holder.wifiSSIDView.setText("SSID: " + mCurrentSensor.SSID);
            holder.wifiRSSIView.setText("RSSI: " + String.valueOf(mCurrentSensor.level));
        }

        @Override
        public int getItemCount() {
            return mSensorInformationList.size();
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