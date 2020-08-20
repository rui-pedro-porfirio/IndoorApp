package android.example.findlocation.services;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.example.findlocation.App;
import android.example.findlocation.activities.ui.DisplayToast;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthBackgroundService extends JobIntentService {

    private static final String AUTHORIZE_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
    private static final String EXCHANGE_AUTH_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/token";
    private static final String VERIFY_AUTH_DATA = "https://yanux-auth.herokuapp.com/api/verify_oauth2";
    private static final String REDIRECT_URI = "indoorapp://auth/redirect";

    private static final String TAG = OAuthBackgroundService.class.getSimpleName();

    public static final String RECEIVER = "receiver";

    private static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    private static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    private static final String ACTION_CHECK_AUTH_CODE = "action.CHECK_AUTH_CODE";

    private static final String CLIENT_ID = "indoor-location-app";
    private static final String CLIENT_SECRET = "indoorsecret";

    private static final int NUMBER_OF_TRIES = 3;

    public static final int ACCESS_TOKEN_CODE = 102;
    public static final int FAILED_RESULT_CODE = 500;
    public static final int FINISHED_CODE = 100;

    private static final String USERNAME_KEY = "Username";
    private static final String ACCESS_TOKEN_KEY = "Access Token";
    private static final String REFRESH_TOKEN_KEY = "Refresh Token";
    private static final String EXPIRATION_DATE_KEY = "Expiration Date";
    private static final String AUTH_CODE_KEY = "Auth Code";

    private Handler mHandler;
    private ResultReceiver mResultReceiver;
    private SharedPreferences applicationPreferences;
    private SharedPreferences.Editor preferencesEditor;
    private OkHttpClient client;
    private int retries;
    private String accessToken;
    private String refreshToken;
    private String username;
    private String expirationDate;
    private String autorizationCode;
    private boolean isTokenValid;

    /**
     * method called on the start of the service
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient();
        applicationPreferences = App.preferences;
        mHandler = new Handler();
        retries = 0;
        preferencesEditor = applicationPreferences.edit();
        username = applicationPreferences.getString(USERNAME_KEY, null);
        accessToken = applicationPreferences.getString(ACCESS_TOKEN_KEY, null);
        refreshToken = applicationPreferences.getString(REFRESH_TOKEN_KEY, null);
        expirationDate = applicationPreferences.getString(EXPIRATION_DATE_KEY, null);
        autorizationCode = applicationPreferences.getString(AUTH_CODE_KEY, null);
        if (expirationDate != null) //Here it is assumed that the access token and refresh token are both existent
            isTokenValid = isAccessTokenValid();
        if (!isTokenValid && accessToken != null && refreshToken != null)
            exchangeRefreshToken();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, ServiceResultReceiver workerResultReceiver, String action, int id, Intent receivedIntent) {
        Intent intent = new Intent(context, OAuthBackgroundService.class);
        if (receivedIntent != null) {
            int flags = Intent.FILL_IN_DATA |
                    Intent.FILL_IN_CATEGORIES |
                    Intent.FILL_IN_PACKAGE |
                    Intent.FILL_IN_COMPONENT;

            intent.fillIn(receivedIntent, flags);
        }
        intent.putExtra(RECEIVER, workerResultReceiver);
        intent.setAction(action);
        enqueueWork(context, OAuthBackgroundService.class, id, intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_REQUEST_AUTH_CODE:
                    boolean isAlive = isMyServiceRunning(OAuthBackgroundService.class);
                    Log.d(TAG, "SERVICE IS ALIVE : " + isAlive);
                    if (autorizationCode == null) {
                        requestAuthorizationCode();
                    } else {
                        mHandler.post(new DisplayToast(this, "User already logged in YanuX"));
                    }
                    break;
                case ACTION_REPLY_AUTH_CODE:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER);
                    isAlive = isMyServiceRunning(OAuthBackgroundService.class);
                    Log.d(TAG, "SERVICE IS ALIVE : " + isAlive);
                    structureAuthorizationCode(intent, mResultReceiver);
                    break;
                case ACTION_CHECK_AUTH_CODE:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER);
                    isAlive = isMyServiceRunning(OAuthBackgroundService.class);
                    Log.d(TAG, "SERVICE IS ALIVE : " + isAlive);
                    boolean hasAccessToken = accessToken != null;
                    Bundle bundle_token = new Bundle();
                    bundle_token.putBoolean("hasAccessToken", hasAccessToken);
                    mResultReceiver.send(ACCESS_TOKEN_CODE, bundle_token);
                    break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void structureAuthorizationCode(Intent intent, ResultReceiver mResultReceiver) {
        String action = intent.getAction();
        Uri responseUri = intent.getData();
        autorizationCode = responseUri.getQueryParameter("code");
        Log.d(TAG, "CODE: " + autorizationCode);
        preferencesEditor.putString(AUTH_CODE_KEY, autorizationCode);
        preferencesEditor.apply();
        exchangeAuthorizationCode(mResultReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void exchangeAuthorizationCode(ResultReceiver mResultReceiver) {
        if (autorizationCode != null) {
            String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
            RequestBody requestBody = new FormBody.Builder()
                    .add("code", autorizationCode)
                    .add("grant_type", "authorization_code")
                    .add("redirect_uri", REDIRECT_URI)
                    .build();
            Request request = new Request.Builder()
                    .url(EXCHANGE_AUTH_ADDRESS)
                    .header("Authorization", credentials)
                    .post(requestBody)
                    .build();
            sendPostHTTPRequest(request);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("code_error", "Problem retrieving authorization code");
            mResultReceiver.send(FAILED_RESULT_CODE, bundle);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void exchangeRefreshToken() {
        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
        RequestBody requestBody = new FormBody.Builder()
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .add("redirect_uri", REDIRECT_URI)
                .build();
        Request request = new Request.Builder()
                .url(EXCHANGE_AUTH_ADDRESS)
                .header("Authorization", credentials)
                .post(requestBody)
                .build();
        sendPostHTTPRequest(request);
    }

    public void requestAuthorizationCode() {
        Intent authenticationCodeRequirementIntent = new Intent("android.intent.action.VIEW", Uri.parse(AUTHORIZE_ADDRESS));
        authenticationCodeRequirementIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        authenticationCodeRequirementIntent.putExtra("Authentication Code Requirement", true);
        startActivity(authenticationCodeRequirementIntent);
        retries++;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void requestTokenInfo() {
        Request request = new Request.Builder()
                .url(VERIFY_AUTH_DATA)
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + accessToken)
                .build();
        sendGetHTTPRequest(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isAccessTokenValid() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime expiration = LocalDateTime.parse(expirationDate, formatter);
        LocalDateTime now = LocalDateTime.now();
        if (expiration.isAfter(now)) {
            return true;
        }
        return false;

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendPostHTTPRequest(Request request) {
        Handler mainHandler = new Handler(getMainLooper());
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (retries == NUMBER_OF_TRIES) {
                    Bundle bundle = new Bundle();
                    bundle.putString("code_error", "Reached maximum limit of attempts to authenticate. Please login again.");
                    mResultReceiver.send(FAILED_RESULT_CODE, bundle);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("code_error", "Problem exchanging code. Asking user to authenticate again.");
                    mResultReceiver.send(FAILED_RESULT_CODE, bundle);
                    requestAuthorizationCode();
                }
            } else {
                JSONObject json_params = new JSONObject(response.body().string());
                accessToken = json_params.getString("access_token");
                refreshToken = json_params.getString("refresh_token");
                preferencesEditor.putString(ACCESS_TOKEN_KEY, accessToken);
                preferencesEditor.apply();
                preferencesEditor.putString(REFRESH_TOKEN_KEY, refreshToken);
                preferencesEditor.apply();
                requestTokenInfo();
            }
            response.body().close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Do your stuff here related to UI, e.g. show toast
                    Toast.makeText(getApplicationContext(), "Error in Application. Please review Post Request of OAuthBackgroundService.java", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendGetHTTPRequest(Request request) {
        Handler mainHandler = new Handler(getMainLooper());
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Bundle bundle = new Bundle();
                bundle.putString("code_error", "Problem getting information about token.");
                mResultReceiver.send(FAILED_RESULT_CODE, bundle);
            } else {
                JSONObject json_params = new JSONObject(response.body().string());
                JSONObject responseS = json_params.getJSONObject("response");
                JSONObject user = responseS.getJSONObject("user");
                username = user.getString("email");
                preferencesEditor.putString(USERNAME_KEY, username);
                preferencesEditor.apply();
                JSONObject access_token_json = responseS.getJSONObject("access_token");
                expirationDate = access_token_json.getString("expiration_date");
                preferencesEditor.putString(EXPIRATION_DATE_KEY, expirationDate);
                preferencesEditor.apply();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "User Authenticated.", Toast.LENGTH_SHORT).show();
                    }
                });
                Bundle bundle_auth = new Bundle();
                bundle_auth.putBoolean("authorized", true);
                mResultReceiver.send(FINISHED_CODE, bundle_auth);
            }
            response.body().close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Do your stuff here related to UI, e.g. show toast
                    Toast.makeText(getApplicationContext(), "Error in Application. Please review Get Request of OAuthBackgroundService.java", Toast.LENGTH_SHORT).show();
                }
            });
        }
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


