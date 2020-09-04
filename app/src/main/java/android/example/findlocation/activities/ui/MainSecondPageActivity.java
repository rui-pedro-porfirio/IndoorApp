package android.example.findlocation.activities.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.example.findlocation.App;
import android.example.findlocation.R;
import android.example.findlocation.activities.fingerprinting.FingerprintingOfflineActivity;
import android.example.findlocation.services.OAuthBackgroundService;
import android.example.findlocation.services.ServiceResultReceiver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.OkHttpClient;

public class MainSecondPageActivity extends AppCompatActivity implements ServiceResultReceiver.Receiver {

    static final int OAUTH_ID = 1010;
    private static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    private static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    private static final String ACTION_CHECK_AUTH_CODE = "action.CHECK_AUTH_CODE";

    public static final int ACCESS_TOKEN_CODE = 102;
    public static final int FAILED_RESULT_CODE = 500;
    public static final int FINISHED_CODE = 100;

    private static final String AUTH_CODE_KEY = "Auth Code";

    private static final String OAUTH_BASIC = "Authorization Code Flow";
    private static final String OAUTH_PKCE = "PKCE Authorization Code Flow";

    public boolean isAuthenticated;
    private SharedPreferences applicationPreferences;
    private ServiceResultReceiver mServiceResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applicationPreferences = App.preferences;
        setContentView(R.layout.activity_main_second_page);
        mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        mServiceResultReceiver.setReceiver(this);
        startTestingPhase();
        startRegistrationPhase();
        startLoginPhase();
        startScanningPhase();
        OAuthBackgroundService.enqueueWork(MainSecondPageActivity.this, mServiceResultReceiver,ACTION_CHECK_AUTH_CODE,OAUTH_ID,OAUTH_PKCE,null);
        String autorizationCode = applicationPreferences.getString(AUTH_CODE_KEY, null);
        isAuthenticated = autorizationCode != null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView trackingView = (TextView) findViewById(R.id.tracking_statusId);
                ImageView trackingImage = (ImageView) findViewById(R.id.tracking_buttonId);
                checkTrackingStatus(trackingView,trackingImage);
            }
        },2000);
    }

    private void checkTrackingStatus(TextView trackingView, ImageView imageView){
        boolean status = applicationPreferences.getBoolean("TRACKING_STATUS",false);
        if(!status){
            trackingView.setText("Not Tracking");
            trackingView.setTextColor(Color.parseColor("#E80A0A"));
            imageView.setImageResource(android.R.drawable.ic_notification_overlay);
        }
        else{
            trackingView.setText("Tracking");
            trackingView.setTextColor(Color.parseColor("#4CAF50"));
            imageView.setImageResource(android.R.drawable.presence_online);
        }
    }


    public void startTestingPhase() {
        final Intent testStartIntent = new Intent(this, MainPageActivity.class);
        Button testButton = (Button) findViewById(R.id.testButtonId);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(testStartIntent);
            }
        });
    }

    public void startScanningPhase() {
        final Intent scanStartIntent = new Intent(this, ScanningActivity.class);
        Button scanButton = (Button) findViewById(R.id.startScanButtonId);
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

                if (!isAuthenticated) {
                    OAuthBackgroundService.enqueueWork(MainSecondPageActivity.this, mServiceResultReceiver,ACTION_REQUEST_AUTH_CODE,OAUTH_ID,null,null);
                }
                else{
                    Toast.makeText(getApplicationContext(),"Already Authenticated.",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        OAuthBackgroundService.enqueueWork(MainSecondPageActivity.this,mServiceResultReceiver,ACTION_REPLY_AUTH_CODE,OAUTH_ID,null,intent);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case FAILED_RESULT_CODE:
                if(resultData != null){
                    Toast.makeText(this, resultData.getString("code_error"), Toast.LENGTH_SHORT).show();
                }
                break;
            case ACCESS_TOKEN_CODE:
                if(resultData != null){
                    isAuthenticated = resultData.getBoolean("hasAccessToken");
                    if (!isAuthenticated) {
                        OAuthBackgroundService.enqueueWork(MainSecondPageActivity.this, mServiceResultReceiver,ACTION_REQUEST_AUTH_CODE,OAUTH_ID,null,null);
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Already Authenticated.",Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case FINISHED_CODE:
                isAuthenticated = resultData.getBoolean("authorized");
                break;
        }
    }

}

