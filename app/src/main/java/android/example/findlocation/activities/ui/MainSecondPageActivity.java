package android.example.findlocation.activities.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.example.findlocation.R;
import android.example.findlocation.activities.fingerprinting.FingerprintingScreenActivity;
import android.example.findlocation.activities.proximity.ProximityScreenActivity;
import android.example.findlocation.activities.sensors.SensorInformationActivity;
import android.example.findlocation.activities.trilateration.TrilaterationScreenActivity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainSecondPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_second_page);
        startScanningInformation();
        startRegistrationPhase();
        startLoginPhase();
    }

    public void startScanningInformation(){
        final Intent scanStartIntent = new Intent(this, MainPageActivity.class);
        Button scanButton = (Button) findViewById(R.id.scanInformationButtonId);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(scanStartIntent);
            }
        });
    }

    public void startRegistrationPhase(){
        String uri = "https://yanux-auth.herokuapp.com/auth/register";
        final Intent registerStartIntent = new Intent("android.intent.action.VIEW",Uri.parse(uri));
        Button mRegisterButton = findViewById(R.id.registerButtonId);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(registerStartIntent);
            }
        });
    }

    public void startLoginPhase(){
        String uri = "https://yanux-auth.herokuapp.com/auth/login";
        final Intent loginStartIntent = new Intent("android.intent.action.VIEW",Uri.parse(uri));
        Button mLoginButton = findViewById(R.id.loginButtonId);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(loginStartIntent);
            }
        });
    }

}