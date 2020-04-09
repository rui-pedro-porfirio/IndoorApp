package android.example.findlocation.tabs;

import android.example.findlocation.R;
import android.example.findlocation.activities.OfflineTabedActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabPreferences extends Fragment {


    private Map<String,Integer> preferences;
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tabpreferences, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new HashMap<String,Integer>();
        EditText mNumberOfFingerprints = view.findViewById(R.id.fingerprintPerLocationNumberId);
        preferences.put("Number of Fingerprints",Integer.valueOf(mNumberOfFingerprints.getText().toString()));
        EditText mTimeBetweenFingerprints = view.findViewById(R.id.intervalBetweenFingerprintsNumberId);
        preferences.put("Time between Fingerprints",Integer.valueOf(mTimeBetweenFingerprints.getText().toString()));
        ((OfflineTabedActivity) getActivity()).setPreferences(preferences);
    }

}
