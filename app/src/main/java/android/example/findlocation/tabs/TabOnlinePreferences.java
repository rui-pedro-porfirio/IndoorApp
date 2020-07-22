package android.example.findlocation.tabs;

import android.example.findlocation.R;
import android.example.findlocation.activities.fingerprinting.FingerprintingOnlineActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TabOnlinePreferences extends Fragment{


    private String selectedAlgorithm;
    private String selectedFilter;

    private String[] algorithms = {"KNN Regression","KNN Classifier", "MLP Regression","MLP Classifier","K-Means Classifier","SVM Classifier"};
    private String[] filters = {"None","Median", "Mean"};
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tabonlinepreferences, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Spinner spin = (Spinner) view.findViewById(R.id.algorithmsSpinnerId);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, algorithms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(new AlgorithmsSpinnerClass());
        Spinner spin2 = (Spinner) view.findViewById(R.id.filterSpinnerId);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, filters);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin2.setAdapter(adapter2);
        spin2.setOnItemSelectedListener(new FiltersSpinnerClass());
    }

    class AlgorithmsSpinnerClass implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
            selectedAlgorithm = algorithms[position];
            ((FingerprintingOnlineActivity) getActivity()).setAlgorithm(selectedAlgorithm);
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO - Custom Code
        }
    }

    class FiltersSpinnerClass implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
            selectedFilter = filters[position];
            ((FingerprintingOnlineActivity) getActivity()).setFilter(selectedFilter);
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO - Custom Code
        }
    }
}
