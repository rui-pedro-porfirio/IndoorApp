package android.example.findlocation.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.example.findlocation.App;
import android.example.findlocation.R;
import android.example.findlocation.activities.ui.DisplayToast;
import android.example.findlocation.activities.ui.MainActivity;
import android.example.findlocation.activities.ui.MainSecondPageActivity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.prefs.Preferences;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthBackgroundService extends JobIntentService {

    private static final String AUTHORIZE_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
    private static final String EXCHANGE_AUTH_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/token";
    private static final String REDIRECT_URI = "indoorapp://auth/redirect";

    private static final String TAG = OAuthBackgroundService.class.getSimpleName();
    public static final String RECEIVER = "receiver";
    private static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    private static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    private static final String CLIENT_ID = "indoor-location-app";
    private static final String CLIENT_SECRET = "indoorsecret";

    public static final int CHECK_RESULT_CODE = 101;
    public static final int FAILED_RESULT_CODE = 500;

    private Handler mHandler;
    private boolean hasAuthenticationCode;
    private ResultReceiver mResultReceiver;
    private SharedPreferences applicationPreferences;
    private String authCode;
    private OkHttpClient client;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, ServiceResultReceiver workerResultReceiver,String action,int id,Intent receivedIntent) {
        Intent intent = new Intent(context, OAuthBackgroundService.class);
        if(receivedIntent != null){
            int flags = Intent.FILL_IN_DATA |
                    Intent.FILL_IN_CATEGORIES |
                    Intent.FILL_IN_PACKAGE |
                    Intent.FILL_IN_COMPONENT;

            intent.fillIn(receivedIntent,flags);
        }
        intent.putExtra(RECEIVER, workerResultReceiver);
        intent.setAction(action);
        enqueueWork(context, OAuthBackgroundService.class, id, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient();
        applicationPreferences = App.preferences;
        mHandler = new Handler();
        hasAuthenticationCode = false;
        authCode = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_REQUEST_AUTH_CODE:
                    boolean isAlive = isMyServiceRunning(OAuthBackgroundService.class);
                    System.out.println("ALIVE: " + isAlive);
                    if (!hasAuthenticationCode) {
                        requestAuthorizationCode();
                        hasAuthenticationCode = true;
                    } else {
                        mHandler.post(new DisplayToast(this, "User already logged in YanuX"));
                    }
                    mResultReceiver = intent.getParcelableExtra(RECEIVER);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("code_alive", hasAuthenticationCode);
                    mResultReceiver.send(CHECK_RESULT_CODE, bundle);
                    break;
                case ACTION_REPLY_AUTH_CODE:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER);
                    isAlive = isMyServiceRunning(OAuthBackgroundService.class);
                    System.out.println("ALIVE: " + isAlive);
                    structureAuthorizationCode(intent,mResultReceiver);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void structureAuthorizationCode(Intent intent, ResultReceiver mResultReceiver) {
        String action = intent.getAction();
        Uri responseUri = intent.getData();
        authCode = responseUri.getQueryParameter("code");
        Log.d(TAG,"CODE: " + authCode);
        applicationPreferences.edit().putString("AuthCode", authCode);
        exchangeAuthorizationCode(mResultReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String sendHTTPRequest(String credentials) {

        Handler mainHandler = new Handler(getMainLooper());
        String responseString = null;
        RequestBody requestBody = new FormBody.Builder()
                .add("code", authCode)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", REDIRECT_URI)
                .build();
        Request request = new Request.Builder()
                .url(EXCHANGE_AUTH_ADDRESS)
                .header("Authorization", credentials)
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Bundle bundle = new Bundle();
                bundle.putString("exchange_error", "Problem exchanging code. Asking user to authenticate again.");
                mResultReceiver.send(FAILED_RESULT_CODE, bundle);
                requestAuthorizationCode();
            } else {
                JSONObject json_params = new JSONObject(response.body().string());
                String accessToken = json_params.getString("access_token");
                String refreshToken = json_params.getString("refresh_token");
                SharedPreferences.Editor editor = applicationPreferences.edit();
                editor.remove("authCode");
                editor.putString("Access Token",accessToken);
                editor.putString("Refresh Token",refreshToken);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "User authenticated", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            response.body().close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Do your stuff here related to UI, e.g. show toast
                    Toast.makeText(getApplicationContext(), "Error in Application. Please review line 165 of OAuthBackgroundService.java", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return responseString;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void exchangeAuthorizationCode(ResultReceiver mResultReceiver){
        if(!authCode.isEmpty()){
            String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
            sendHTTPRequest(credentials);
        }
        else{
            Bundle bundle = new Bundle();
            bundle.putString("code_error", "Problem retrieving authorization code");
            mResultReceiver.send(FAILED_RESULT_CODE, bundle);
        }
    }

    public void requestAuthorizationCode() {
        Intent authenticationCodeRequirementIntent = new Intent("android.intent.action.VIEW", Uri.parse(AUTHORIZE_ADDRESS));
        authenticationCodeRequirementIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        authenticationCodeRequirementIntent.putExtra("Authentication Code Requirement",true);
        startActivity(authenticationCodeRequirementIntent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}


