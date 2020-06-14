package android.example.findlocation.tabs;

import android.example.findlocation.R;
import android.example.findlocation.activities.OfflineTabedActivity;
import android.example.findlocation.activities.ProximityDistanceScanActivity;
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

public class TabProximityDistancePreferences extends Fragment {


    private Map<String, Float> preferences;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tab_distance_proximity_preferences, container, false);
        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new HashMap<String, Float>();
        EditText mXCoordinate = view.findViewById(R.id.proximityXCoordinateValueId);
        preferences.put("X", Float.valueOf(mXCoordinate.getText().toString()));
        EditText mYCoordinate = view.findViewById(R.id.proximityYCoordinateValueId);
        preferences.put("Y", Float.valueOf(mYCoordinate.getText().toString()));
        ((ProximityDistanceScanActivity) getActivity()).setPreferences(preferences);

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
                    ((ProximityDistanceScanActivity) getActivity()).setPreferences(preferences);
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
                    ((ProximityDistanceScanActivity) getActivity()).setPreferences(preferences);
                }
            }
        });
    }
}
