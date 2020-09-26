package android.example.findlocation.ui.activities.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.example.findlocation.IndoorApp;
import android.example.findlocation.R;
import android.example.findlocation.exceptions.HTTPRequestException;
import android.example.findlocation.interfaces.SharedPreferencesInterface;
import android.example.findlocation.services.OAuthBackgroundService;
import android.example.findlocation.services.ServiceResultReceiver;
import android.example.findlocation.ui.activities.scanning.ScanningActivity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements ServiceResultReceiver.Receiver, SharedPreferencesInterface {

    private static final String TAG = MainActivity.class.getSimpleName();

    static final String PREF_DEVICE_UUID = "PREF_DEVICE_UUID";

    static final int OAUTH_ID = 1010;
    static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    static final String ACTION_CHECK_AUTH_CODE = "action.CHECK_AUTH_CODE";
    static final int FAILED_RESULT_CODE = 500;
    static final int AUTH_VALIDITY = 102;

    static final String OAUTH_BASIC = "Authorization Code Flow";
    static final String OAUTH_PKCE = "PKCE Authorization Code Flow";

    static final String DEVICE_UUID_ADDRESS = "http://localhost:3003/deviceInfo";

    private boolean isAuthenticated;
    private SharedPreferences mAppPreferences;
    private SharedPreferences.Editor mPreferencesEditor;
    private ServiceResultReceiver mServiceResultReceiver;
    private OkHttpClient mHttpClient;
    private String mDeviceUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);
        initializeSharedPreferences();
        this.mHttpClient = new OkHttpClient();
        mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        mServiceResultReceiver.setReceiver(this);
        loadVariablesFromSharedPreferences();
        startOAuthJob();
        if (mDeviceUuid == null) getDeviceUUID();
        startButtonListeners();
    }

    @Override
    public void initializeSharedPreferences() {
        mAppPreferences = IndoorApp.appPreferences;
        mPreferencesEditor = mAppPreferences.edit();
    }

    @Override
    public void loadVariablesFromSharedPreferences() {
        mDeviceUuid = mAppPreferences.getString(PREF_DEVICE_UUID, null);
    }

    protected void startButtonListeners() {
        handleRegisterButton();
        handleLoginButton();
        handleExperimentButton();
        handleScanButton();
    }

    public void handleRegisterButton() {
        Log.i(TAG, "Clicked register button.");
        String yanuxRegisterUri = "https://yanux-auth.herokuapp.com/auth/register";
        final Intent mStartRegisterIntent = new Intent("android.intent.action.VIEW", Uri.parse(yanuxRegisterUri));
        Button mRegisterButton = findViewById(R.id.button_registerButton);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mStartRegisterIntent);
            }
        });
    }

    public void handleLoginButton() {
        Log.i(TAG, "Clicked login button.");
        Button mLoginButton = findViewById(R.id.button_loginButton);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (!isAuthenticated) {
                    Log.i(TAG, "User not authenticated after login click. Initializing OAuth service.");
                    OAuthBackgroundService.enqueueWork(MainActivity.this, mServiceResultReceiver,
                            ACTION_REQUEST_AUTH_CODE, OAUTH_ID, null, null);
                } else {
                    Log.i(TAG, "User already authenticated after login click.");
                    Toast.makeText(getApplicationContext(), "Already Authenticated.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void handleExperimentButton() {
        Log.i(TAG, "Clicked experiment button.");
        final Intent mStartExperimentIntent = new Intent(this, MainPageActivity.class);
        Button mExperimentButton = (Button) findViewById(R.id.button_experimentButton);
        mExperimentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mStartExperimentIntent);
            }
        });
    }

    public void handleScanButton() {
        Log.i(TAG, "Clicked scan button.");
        final Intent mStartScanIntent = new Intent(this, ScanningActivity.class);
        Button mScanButton = (Button) findViewById(R.id.button_scanButton);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mStartScanIntent);
            }
        });
    }

    private void startOAuthJob() {
        Log.i(TAG, "Starting OAuth Background Service.");
        OAuthBackgroundService.enqueueWork(MainActivity.this, mServiceResultReceiver, ACTION_CHECK_AUTH_CODE, OAUTH_ID, OAUTH_PKCE, null);
    }

    /*
    This method is called after the user authorizes the application to get its data
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "User accepted the terms. Authorization Code retrieved.");
        super.onNewIntent(intent);
        // Start OAuth procedure for exchanging authorization code and retrieve the access token
        OAuthBackgroundService.enqueueWork(MainActivity.this, mServiceResultReceiver, ACTION_REPLY_AUTH_CODE, OAUTH_ID, null, intent);
    }

    /*
    This method receives results from the OAuth service
     */
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData == null)
            throw new NullPointerException("Result received from OAuth with null data.");
        if (resultCode == FAILED_RESULT_CODE) {
            String errorMessage = resultData.getString("code_error");
            Log.e(TAG, errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        } else if (resultCode == AUTH_VALIDITY) {
            isAuthenticated = resultData.getBoolean("isValid");
            if (!isAuthenticated) {
                Log.i(TAG, "User not authenticated after validity check. Initializing OAuth service.");
                OAuthBackgroundService.enqueueWork(MainActivity.this, mServiceResultReceiver, ACTION_REQUEST_AUTH_CODE, OAUTH_ID, null, null);
            } else {
                Log.i(TAG, "User already authenticated after validity check.");
                Toast.makeText(getApplicationContext(), "User Authenticated.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getDeviceUUID() {
        Log.i(TAG, "Start retrieving device UUID.");
        new HTTPGetRequest(DEVICE_UUID_ADDRESS).execute();
    }

    private class HTTPGetRequest extends AsyncTask<Void, Void, String> {

        private final String TAG = HTTPGetRequest.class.getSimpleName();
        private String mIpAddress;

        public HTTPGetRequest(String address) {
            this.mIpAddress = address;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(Void... voids) {
            try {
                Log.i(TAG, "Making Get Request to server.");
                return get(mIpAddress);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
        }


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        protected String get(String url) throws IOException {
            Handler mainHandler = new Handler(getMainLooper());
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = mHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String responseString = response.body().string();
                if (responseString.isEmpty()) {
                    throw new HTTPRequestException("Exception raised on empty response in HTTP Get Request.");
                }
                Log.i(TAG, "Received response from the server with populated data.");
                JSONObject jsonObject = new JSONObject(responseString);
                mDeviceUuid = jsonObject.getString("deviceUuid");
                mPreferencesEditor.putString(PREF_DEVICE_UUID, mDeviceUuid);
                mPreferencesEditor.apply();
                return String.valueOf(response.code());
            } catch (ConnectException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connection Error.Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SocketTimeoutException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Socket Timeout.Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (HTTPRequestException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}

