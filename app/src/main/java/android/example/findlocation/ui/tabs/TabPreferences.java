package android.example.findlocation.ui.tabs;

import android.example.findlocation.R;
import android.example.findlocation.ui.activities.fingerprinting.FingerprintingOfflineActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

public class TabPreferences extends Fragment implements AdapterView.OnItemSelectedListener {


    private final String[] zones = {"Personal", "Social", "Public"};
    private String zoneClassifier;
    private Map<String, Float> preferences;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tabpreferences, container, false);
        return root;
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        zoneClassifier = zones[position];
        ((FingerprintingOfflineActivity) getActivity()).setZoneClassifier(zoneClassifier);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO - Custom Code
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new HashMap<String, Float>();
        Spinner spin = view.findViewById(R.id.zoneDropdownId);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, zones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(this);
        EditText mNumberOfFingerprints = view.findViewById(R.id.fingerprintPerLocationNumberId);
        preferences.put("Number of Fingerprints", Float.valueOf(mNumberOfFingerprints.getText().toString()));
        EditText mTimeBetweenFingerprints = view.findViewById(R.id.intervalBetweenFingerprintsNumberId);
        preferences.put("Time between Fingerprints", Float.valueOf(mTimeBetweenFingerprints.getText().toString()));
        EditText mXCoordinate = view.findViewById(R.id.xCoordinateValueId);
        preferences.put("X", Float.valueOf(mXCoordinate.getText().toString()));
        EditText mYCoordinate = view.findViewById(R.id.yCoordinateValueId);
        preferences.put("Y", Float.valueOf(mYCoordinate.getText().toString()));
        ((FingerprintingOfflineActivity) getActivity()).setPreferences(preferences);
        mNumberOfFingerprints.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("")) {
                    preferences.put("Number of Fingerprints", Float.valueOf(s.toString()));
                    ((FingerprintingOfflineActivity) getActivity()).setPreferences(preferences);
                }
            }
        });
        mTimeBetweenFingerprints.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                preferences.put("Time between Fingerprints", Float.valueOf(s.toString()));
                ((FingerprintingOfflineActivity) getActivity()).setPreferences(preferences);
            }
        });
        mXCoordinate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("")) {
                    preferences.put("X", Float.valueOf(s.toString()));
                    ((FingerprintingOfflineActivity) getActivity()).setPreferences(preferences);
                }
            }
        });
        mYCoordinate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("")) {
                    preferences.put("Y", Float.valueOf(s.toString()));
                    ((FingerprintingOfflineActivity) getActivity()).setPreferences(preferences);
                }
            }
        });
    }

}
