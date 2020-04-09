package android.example.findlocation.tabs;

import android.example.findlocation.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        return root;
    }
}
