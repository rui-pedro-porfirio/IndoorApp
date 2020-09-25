package android.example.findlocation.ui.tabs;

import android.example.findlocation.R;
import android.example.findlocation.ui.activities.trilateration.TrilaterationScreenActivity;
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

public class TabTrilaterationOnlinePreferences extends Fragment {


    private String selectedAlgorithm;

    private String[] algorithms = {"KNN Regression","MLP Regression","SVM Regressor","Linear Regression"};
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tab_trilateration_online_preferences, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Spinner spin = (Spinner) view.findViewById(R.id.trilaterationAlgorithmsSpinnerId);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, algorithms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(new AlgorithmsSpinnerClass());
    }

    class AlgorithmsSpinnerClass implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
            selectedAlgorithm = algorithms[position];
            ((TrilaterationScreenActivity) getActivity()).setAlgorithm(selectedAlgorithm);
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO - Custom Code
        }
    }
}
