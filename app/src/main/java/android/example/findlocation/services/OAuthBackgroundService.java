package android.example.findlocation.services;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.example.findlocation.IndoorApp;
import android.example.findlocation.exceptions.HTTPRequestException;
import android.example.findlocation.interfaces.SharedPreferencesInterface;
import android.example.findlocation.ui.common.DisplayToast;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthBackgroundService extends JobIntentService implements SharedPreferencesInterface {

    private static final String TAG = OAuthBackgroundService.class.getSimpleName();

    static final String AUTHORIZE_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
    static final String EXCHANGE_AUTH_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/token";
    static final String VERIFY_AUTH_DATA_ADDRESS = "https://yanux-auth.herokuapp.com/api/verify_oauth2";
    static final String REDIRECT_URI = "indoorapp://auth/redirect";

    public static final String RECEIVER = "receiver";

    private static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    private static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    private static final String ACTION_CHECK_AUTH_CODE = "action.CHECK_AUTH_CODE";

    private static final String CLIENT_ID = "indoor-location-app";
    private static final String CLIENT_SECRET = "indoorsecret";

    private static final int NUMBER_OF_TRIES = 3;

    public static final int AUTH_VALIDITY = 102;
    public static final int FAILED_RESULT_CODE = 500;

    private static final String PREF_USERNAME = "PREF_USERNAME";
    private static final String PREF_ACCESS_TOKEN = "PREF_ACCESS_TOKEN";
    private static final String PREF_REFRESH_TOKEN = "PREF_REFRESH_TOKEN";
    private static final String PREF_EXPIRATION_DATE = "PREF_EXPIRATION_DATE";
    private static final String PREF_AUTH_CODE = "PREF_AUTH_CODE";
    private static final String PREF_AUTH_FLOW = "PREF_AUTH_FLOW";
    private static final String PREF_PKCE_CODE_VERIFIER_KEY = "PREF_PKCE_CODE_VERIFIER_KEY";

    private static final String OAUTH_BASIC = "Authorization Code Flow";
    private static final String OAUTH_PKCE = "PKCE Authorization Code Flow";

    private Handler mHandler;
    private SharedPreferences mApplicationPreferences;
    private SharedPreferences.Editor mPreferencesEditor;
    private OkHttpClient mHttpClient;
    private int mAuthRetries;
    private String mAccessToken;
    private String mRefreshToken;
    private String mUsername;
    private String mExpirationDate;
    private String mAutorizationCode;
    private boolean mIsTokenValid;

    //PKCE Related
    private String mCodeVerifier;
    private String mCodeChallenge;
    private String mOAuthFlow;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        initializeSharedPreferences();
        mHttpClient = new OkHttpClient();
        mHandler = new Handler();
        mAuthRetries = 0;
        mCodeChallenge = null;
        initializeSharedPreferences();
        checkExpirationTokenValidity();
        if (!mIsTokenValid && mAccessToken != null && mRefreshToken != null)
            exchangeRefreshToken();
    }

    @Override
    public void initializeSharedPreferences() {
        mApplicationPreferences = IndoorApp.appPreferences;
        mPreferencesEditor = mApplicationPreferences.edit();
    }

    @Override
    public void loadVariablesFromSharedPreferences() {
        mCodeVerifier = mApplicationPreferences.getString(PREF_PKCE_CODE_VERIFIER_KEY, null);
        mUsername = mApplicationPreferences.getString(PREF_USERNAME, null);
        mAccessToken = mApplicationPreferences.getString(PREF_ACCESS_TOKEN, null);
        mRefreshToken = mApplicationPreferences.getString(PREF_REFRESH_TOKEN, null);
        mExpirationDate = mApplicationPreferences.getString(PREF_EXPIRATION_DATE, null);
        mAutorizationCode = mApplicationPreferences.getString(PREF_AUTH_CODE, null);
        mOAuthFlow = mApplicationPreferences.getString(PREF_AUTH_FLOW, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkExpirationTokenValidity() {
        if (mExpirationDate != null) //Here it is assumed that the access token and refresh token are both existent
            mIsTokenValid = isAccessTokenValid();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isAccessTokenValid() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime expiration = LocalDateTime.parse(mExpirationDate, formatter);
        LocalDateTime now = LocalDateTime.now();
        if (expiration.isAfter(now)) {
            return true;
        }
        return false;
    }

    /**
     * Convenience method for enqueuing work into this service.
     */
    public static void enqueueWork(Context context, ServiceResultReceiver workerResultReceiver, String action, int id, String authFlow, Intent receivedIntent) {
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
        if (authFlow != null) intent.putExtra("Flow Type", authFlow);
        enqueueWork(context, OAuthBackgroundService.class, id, intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {
            if (mOAuthFlow == null) {
                mOAuthFlow = intent.getStringExtra("Flow Type");
                mPreferencesEditor.putString(PREF_AUTH_FLOW, mOAuthFlow);
                mPreferencesEditor.apply();
            }
            switch (intent.getAction()) {
                case ACTION_REQUEST_AUTH_CODE:
                    if (mAutorizationCode == null) {
                        if (mOAuthFlow.equals(OAUTH_BASIC))
                            requestAuthorizationCode();
                        else
                            generateCodeVerifierAndChallengePKCE();
                    } else {
                        mHandler.post(new DisplayToast(this, "User already logged in YanuX."));
                        exchangeAuthorizationCode();
                    }
                    break;
                case ACTION_REPLY_AUTH_CODE:
                    ResultReceiver mResultReceiver = intent.getParcelableExtra(RECEIVER);
                    structureAuthorizationCode(intent, mResultReceiver);
                    break;
                case ACTION_CHECK_AUTH_CODE:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER);
                    boolean hasAccessToken = mAccessToken != null;
                    Bundle bundle_token = new Bundle();
                    bundle_token.putBoolean("hasAccessToken", hasAccessToken);
                    mResultReceiver.send(AUTH_VALIDITY, bundle_token);
                    break;
            }
        }
    }

    private void requestAuthorizationCode() {
        computeAuthorizationRequest(AUTHORIZE_ADDRESS);
    }

    private void computeAuthorizationRequest(String authorizeAddress) {
        Intent authenticationCodeRequirementIntent = new Intent("android.intent.action.VIEW", Uri.parse(authorizeAddress));
        authenticationCodeRequirementIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        authenticationCodeRequirementIntent.putExtra("Authentication Code Requirement", true);
        startActivity(authenticationCodeRequirementIntent);
        mAuthRetries++;
    }

    private void generateCodeVerifierAndChallengePKCE() {
        generateCodeVerifier();
        generateCodeChallenge();
        requestPKCEAuthorizationCode();
    }

    private void generateCodeVerifier() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[32];
        sr.nextBytes(code);
        mCodeVerifier = Base64.encodeToString(code, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        mPreferencesEditor.putString(PREF_PKCE_CODE_VERIFIER_KEY, mCodeVerifier);
        mPreferencesEditor.apply();
    }

    private void generateCodeChallenge() {
        byte[] bytes = new byte[0];
        try {
            bytes = mCodeVerifier.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(bytes, 0, bytes.length);
        byte[] digest = md.digest();
        mCodeChallenge = Base64.encodeToString(digest, Base64.URL_SAFE);
    }

    private void requestPKCEAuthorizationCode() {
        String final_uri = AUTHORIZE_ADDRESS +
                "&code_challenge=" + mCodeChallenge +
                "&code_challenge_method=S256";
        computeAuthorizationRequest(final_uri);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void structureAuthorizationCode(Intent intent, ResultReceiver mResultReceiver) {
        Uri responseUri = intent.getData();
        mAutorizationCode = responseUri.getQueryParameter("code");
        String error = responseUri.getQueryParameter("error");
        Log.d(TAG, "Authorization Code: " + mAutorizationCode);
        if (mAutorizationCode == null) {
            sendAuthorizationErrorMessageBackToUI(responseUri, mResultReceiver);
        } else {
            mPreferencesEditor.putString(PREF_AUTH_CODE, mAutorizationCode);
            mPreferencesEditor.apply();
            if (mOAuthFlow.equals(OAUTH_BASIC)) exchangeAuthorizationCode();
            else exchangeAuthorizationCodePKCE();
        }
    }

    private void sendAuthorizationErrorMessageBackToUI(Uri responseUri, ResultReceiver mResultReceiver) {
        String error = responseUri.getQueryParameter("error");
        String response;
        if (error != null)
            response = error;
        else
            response = "Problem occured during the authentication code retrieval";
        Bundle bundle = new Bundle();
        bundle.putString("code_error", "Error in authentication:" + response);
        mResultReceiver.send(FAILED_RESULT_CODE, bundle);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void exchangeAuthorizationCode() {
        requestAccessToken(buildBodyAuthBasic());
    }

    private RequestBody buildBodyAuthBasic() {
        return new FormBody.Builder()
                .add("code", mAutorizationCode)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", REDIRECT_URI)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void exchangeAuthorizationCodePKCE() {
        requestAccessToken(buildBodyPKCE());
    }

    private RequestBody buildBodyPKCE() {
        return new FormBody.Builder()
                .add("code", mAutorizationCode)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", mCodeVerifier)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void requestAccessToken(RequestBody requestBody) {
        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
        Request request = new Request.Builder()
                .url(EXCHANGE_AUTH_ADDRESS)
                .header("Authorization", credentials)
                .header("content-type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();
        sendPostHTTPRequest(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void exchangeRefreshToken() {
        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
        RequestBody requestBody = new FormBody.Builder()
                .add("refresh_token", mRefreshToken)
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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void requestTokenInfo() {
        Request request = new Request.Builder()
                .url(VERIFY_AUTH_DATA)
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + accessToken)
                .build();
        sendGetHTTPRequest(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendPostHTTPRequest(Request request) {
        Handler mainHandler = new Handler(getMainLooper());

        try (Response response = mHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (mAuthRetries >= NUMBER_OF_TRIES) {
                    throw new HTTPRequestException("Exception raised in exceeding the limit tries of authorization flow.");
                } else {
                    throw new HTTPRequestException("Exception raised in unexpected failure on communication.");
                }
            } else {
                JSONObject json_params = new JSONObject(response.body().string());
                mAccessToken = json_params.getString("access_token");
                mPreferencesEditor.putString(PREF_ACCESS_TOKEN, mAccessToken);
                mPreferencesEditor.apply();
                mRefreshToken = json_params.getString("refresh_token");
                mPreferencesEditor.putString(PREF_REFRESH_TOKEN, mRefreshToken);
                mPreferencesEditor.apply();
                requestTokenInfo();
                cleanAuthCode();
            }
            response.body().close();
        } catch (IOException | JSONException | HTTPRequestException e) {
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

    private void cleanAuthCode() {
        mPreferencesEditor.remove(PREF_AUTH_CODE);
        mPreferencesEditor.apply();
        mPreferencesEditor.remove(PREF_PKCE_CODE_VERIFIER_KEY);
        mPreferencesEditor.apply();
    }

}


