package android.example.findlocation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Screen with options "Analysis" and "Find location of device"
 */
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        startScanningData();
    }

    public void startScanningData(){
        final Intent scanStartIntent = new Intent(this,ThirdActivity.class);
        Button scanButton = (Button) findViewById(R.id.scanButtonId);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(scanStartIntent);
            }
        });
    }
}
