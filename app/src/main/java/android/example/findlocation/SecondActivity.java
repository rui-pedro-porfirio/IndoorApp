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
        checkModelGraphics();
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

    public void checkModelGraphics(){
        final Intent checkModelIntent = new Intent(this,FourthActivity.class);
        checkModelIntent.putExtra("Type","Model");
        Button modelButton = (Button) findViewById(R.id.checkDataButtonId);
        modelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(checkModelIntent);
            }
        });
    }
}
