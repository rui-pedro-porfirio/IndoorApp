package android.example.findlocation.ui.tabs;

import android.example.findlocation.R;
import android.example.findlocation.ui.activities.fingerprinting.FingerprintingOfflineActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * RECYCLE VIEW WITH FINGERPRINTS
 */
public class TabRadioMap extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tabradiomap, container, false);
        ((FingerprintingOfflineActivity)getActivity()).populateRecycleView(root);
        return root;
    }


}
