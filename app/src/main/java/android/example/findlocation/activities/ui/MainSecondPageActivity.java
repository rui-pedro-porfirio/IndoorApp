package android.example.findlocation.activities.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.example.findlocation.R;
import android.example.findlocation.services.AuthorizationBackgroundService;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import okhttp3.OkHttpClient;

public class MainSecondPageActivity extends AppCompatActivity {


    private static final String AUTHORIZE_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth";

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new OkHttpClient();
        setContentView(R.layout.activity_main_second_page);
        startScanningInformation();
        startRegistrationPhase();
        startLoginPhase();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    public void startScanningInformation() {
        final Intent scanStartIntent = new Intent(this, MainPageActivity.class);
        Button scanButton = (Button) findViewById(R.id.scanInformationButtonId);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(scanStartIntent);
            }
        });
    }

    public void startRegistrationPhase() {
        String uri = "https://yanux-auth.herokuapp.com/auth/register";
        final Intent registerStartIntent = new Intent("android.intent.action.VIEW", Uri.parse(uri));
        Button mRegisterButton = findViewById(R.id.registerButtonId);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(registerStartIntent);
            }
        });
    }

    public void startLoginPhase() {
        Button mLoginButton = findViewById(R.id.loginButtonId);
        final Intent intent = new Intent(this, AuthorizationBackgroundService.class);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(intent);
            }
        });
    }
}

