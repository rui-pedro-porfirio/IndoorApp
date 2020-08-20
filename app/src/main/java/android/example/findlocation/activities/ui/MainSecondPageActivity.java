package android.example.findlocation.activities.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.example.findlocation.App;
import android.example.findlocation.R;
import android.example.findlocation.services.OAuthBackgroundService;
import android.example.findlocation.services.ServiceResultReceiver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import okhttp3.OkHttpClient;

public class MainSecondPageActivity extends AppCompatActivity implements ServiceResultReceiver.Receiver {

    static final int OAUTH_ID = 1010;
    private static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    private static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    public static final int CHECK_RESULT_CODE = 101;
    public static final int FAILED_RESULT_CODE = 500;

    private boolean isServiceRunning;
    private ServiceResultReceiver mServiceResultReceiver;
    private SharedPreferences applicationPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applicationPreferences = App.preferences;
        isServiceRunning = false;
        setContentView(R.layout.activity_main_second_page);
        mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        mServiceResultReceiver.setReceiver(this);
        startScanningInformation();
        startRegistrationPhase();
        startLoginPhase();
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
        final Intent intent = new Intent(this, OAuthBackgroundService.class);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (!isServiceRunning) {
                    OAuthBackgroundService.enqueueWork(MainSecondPageActivity.this, mServiceResultReceiver,ACTION_REQUEST_AUTH_CODE,OAUTH_ID,null);
                }
                else{
                    Toast.makeText(getApplicationContext(),"Already logged in.",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        System.out.println("URI: " + uri);
        OAuthBackgroundService.enqueueWork(MainSecondPageActivity.this,mServiceResultReceiver,ACTION_REPLY_AUTH_CODE,OAUTH_ID,intent);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case CHECK_RESULT_CODE:
                if (resultData != null) {
                    isServiceRunning = resultData.getBoolean("code_alive");
                }
                break;
            case FAILED_RESULT_CODE:
                if(resultData != null){
                    Toast.makeText(this, resultData.getString("code_error"), Toast.LENGTH_SHORT).show();
                }
        }
    }

}

