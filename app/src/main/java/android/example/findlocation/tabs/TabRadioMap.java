package android.example.findlocation.tabs;

import android.example.findlocation.R;
import android.example.findlocation.activities.OfflineTabedActivity;
import android.example.findlocation.adapters.FingerprintAdapter;
import android.example.findlocation.adapters.SensorAdapter;
import android.example.findlocation.objects.Fingerprint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RECYCLE VIEW WITH FINGERPRINTS
 */
public class TabRadioMap extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tabradiomap, container, false);
        ((OfflineTabedActivity)getActivity()).populateRecycleView(root);
        return root;
    }


}
