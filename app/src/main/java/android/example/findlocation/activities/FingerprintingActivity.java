package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.example.findlocation.R;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Needs Offline and Online Phase buttons
(Offline phase)Needs filling scrolling, start button and finish button
(Online phase)Needs current data window and position detected
 */
public class FingerprintingActivity extends AppCompatActivity {

    private List<String> dataTypes;
    private Map<String,Integer> preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprinting);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dataTypes = getIntent().getStringArrayListExtra("dataTypes");
        preferences = (HashMap<String,Integer>) getIntent().getSerializableExtra("Preferences");
        fillDataTypeTextView();
        fillPreferencesTextView();
    }

    private void fillDataTypeTextView(){
        TextView mDataTypeTextView = findViewById(R.id.dataTypesTextViewId);
        String typeToAdd = "*";
        for(int i = 0; i < dataTypes.size(); i++){
            typeToAdd += dataTypes.get(i);
            if(i != dataTypes.size() -1){
                typeToAdd += "|";
            }
        }
        mDataTypeTextView.setText(typeToAdd);
    }

    private void fillPreferencesTextView(){
        TextView mPreferencesTextView = findViewById(R.id.preferencesId);
        String preferencesToAdd = "*";
        for(String key: preferences.keySet()){
            preferencesToAdd += key + ":" + preferences.get(key) + "|";
        }
        mPreferencesTextView.setText(preferencesToAdd);
    }
}
