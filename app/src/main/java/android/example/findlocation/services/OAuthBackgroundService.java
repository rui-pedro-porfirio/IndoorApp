package android.example.findlocation.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.example.findlocation.BuildConfig;
import android.example.findlocation.IndoorApp;
import android.example.findlocation.exceptions.HTTPRequestException;
import android.example.findlocation.interfaces.SharedPreferencesInterface;
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

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthBackgroundService extends JobIntentService implements SharedPreferencesInterface {

    static final String AUTHORIZE_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
    static final String EXCHANGE_AUTH_ADDRESS = "https://yanux-auth.herokuapp.com/oauth2/token";
    static final String VERIFY_AUTH_DATA_ADDRESS = "https://yanux-auth.herokuapp.com/api/verify_oauth2";
    static final String REDIRECT_URI = "indoorapp://auth/redirect";
    static final String RECEIVER = "receiver";
    static final String ACTION_REQUEST_AUTH_CODE = "action.REQUEST_AUTH_CODE";
    static final String ACTION_REPLY_AUTH_CODE = "action.REPLY_AUTH_CODE";
    static final String ACTION_CHECK_AUTH_CODE = "action.CHECK_AUTH_CODE";
    static final String CLIENT_ID = "indoor-location-app";
    static final String CLIENT_SECRET = "indoorsecret";
    static final int NUMBER_OF_TRIES = 3;
    static final int AUTH_VALIDITY = 102;
    static final int FAILED_RESULT_CODE = 500;
    static final String PREF_USERNAME = "PREF_USERNAME";
    static final String PREF_ACCESS_TOKEN = "PREF_ACCESS_TOKEN";
    static final String PREF_REFRESH_TOKEN = "PREF_REFRESH_TOKEN";
    static final String PREF_EXPIRATION_DATE = "PREF_EXPIRATION_DATE";
    static final String PREF_AUTH_CODE = "PREF_AUTH_CODE";
    static final String PREF_AUTH_FLOW = "PREF_AUTH_FLOW";
    static final String PREF_PKCE_CODE_VERIFIER_KEY = "PREF_PKCE_CODE_VERIFIER_KEY";
    static final String OAUTH_BASIC = "Authorization Code Flow";
    static final String OAUTH_PKCE = "PKCE Authorization Code Flow";
    private static final String TAG = OAuthBackgroundService.class.getSimpleName();
    private ResultReceiver mReceiver;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        initializeSharedPreferences();
        mReceiver = null;
        mHttpClient = new OkHttpClient();
        mAuthRetries = 0;
        mCodeChallenge = null;
        loadVariablesFromSharedPreferences();
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

    private void checkExpirationTokenValidity() {
        if (mExpirationDate != null) //Here it is assumed that the access token and refresh token are both existent
            mIsTokenValid = isAccessTokenValid();
        Log.i(TAG, "Token validity: " + mIsTokenValid);
    }

    private boolean isAccessTokenValid() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime expiration = LocalDateTime.parse(mExpirationDate, formatter);
        LocalDateTime now = LocalDateTime.now();
        return expiration.isAfter(now);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (mReceiver == null) mReceiver = intent.getParcelableExtra(RECEIVER);
        if (intent.getAction() != null) {
            if (mOAuthFlow == null) {
                mOAuthFlow = intent.getStringExtra("Flow Type");
                Log.i(TAG, "OAuth Flow: " + mOAuthFlow);
                mPreferencesEditor.putString(PREF_AUTH_FLOW, mOAuthFlow);
                mPreferencesEditor.apply();
            }
            switch (intent.getAction()) {
                case ACTION_REQUEST_AUTH_CODE:
                    if (mAutorizationCode == null) {
                        Log.i(TAG, "Received call for initiating OAuth procedure. Requesting authorization code...");
                        if (mOAuthFlow.equals(OAUTH_BASIC))
                            requestAuthorizationCode();
                        else
                            generateCodeVerifierAndChallengePKCE();
                    } else {
                        Log.i(TAG, "User already has authorization code. Request access token");
                        exchangeAuthorizationCode();
                    }
                    break;
                case ACTION_REPLY_AUTH_CODE:
                    structureAuthorizationCode(intent);
                    break;
                case ACTION_CHECK_AUTH_CODE:
                    checkExpirationTokenValidity();
                    if (!mIsTokenValid && mAccessToken != null && mRefreshToken != null)
                        exchangeRefreshToken();
                    boolean hasAccessToken = mAccessToken != null;
                    Bundle bundle_token = new Bundle();
                    bundle_token.putBoolean("isValid", hasAccessToken);
                    mReceiver.send(AUTH_VALIDITY, bundle_token);
                    break;
            }
        }
    }

    private void requestAuthorizationCode() {
        computeAuthorizationRequest(AUTHORIZE_ADDRESS);
    }

    private void computeAuthorizationRequest(String authorizeAddress) {
        Log.i(TAG, "Retrieving authorization code. Redirecting to authentication page.");
        Intent authenticationCodeRequirementIntent = new Intent("android.intent.action.VIEW", Uri.parse(authorizeAddress));
        authenticationCodeRequirementIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        authenticationCodeRequirementIntent.putExtra("Authentication Code Requirement", true);
        startActivity(authenticationCodeRequirementIntent);
        mAuthRetries++;
        if (mAuthRetries >= NUMBER_OF_TRIES)
            throw new RuntimeException("Exception raised in exceeding the limit tries of authorization flow.");
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
        bytes = mCodeVerifier.getBytes(StandardCharsets.US_ASCII);
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

    private void structureAuthorizationCode(Intent intent) {
        Uri responseUri = intent.getData();
        mAutorizationCode = responseUri.getQueryParameter("code");
        String error = responseUri.getQueryParameter("error");
        if (BuildConfig.DEBUG) Log.d(TAG, "Authorization Code: " + mAutorizationCode);
        if (mAutorizationCode == null) {
            sendAuthorizationErrorMessageBackToUI(responseUri, mReceiver);
        } else {
            Log.i(TAG, "Successfully retrieved authorization code. Requesting access token.");
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

    private void requestAccessToken(RequestBody requestBody) {
        Log.i(TAG, "Requesting access token...");
        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
        Request request = new Request.Builder()
                .url(EXCHANGE_AUTH_ADDRESS)
                .header("Authorization", credentials)
                .header("content-type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();
        sendPostHTTPRequest(request);
    }

    public void exchangeRefreshToken() {
        Log.i(TAG, "Access Token is obsolete. Updating token validity.");
        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
        RequestBody requestBody = new FormBody.Builder()
                .add("refresh_token", mRefreshToken)
                .add("grant_type", "refresh_token")
                .add("redirect_uri", REDIRECT_URI)
                .build();
        Request request = new Request.Builder()
                .url(EXCHANGE_AUTH_ADDRESS)
                .header("Authorization", credentials)
                .header("content-type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();
        sendPostHTTPRequest(request);
    }

    public void requestTokenInfo() {
        Log.i(TAG, "Requesting (GET) information about token including username and expiration date.");
        Request request = new Request.Builder()
                .url(VERIFY_AUTH_DATA_ADDRESS)
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + mAccessToken)
                .build();
        sendGetHTTPRequest(request);
    }

    private void sendPostHTTPRequest(Request request) {
        Handler mainHandler = new Handler(getMainLooper());
        Log.i(TAG, "Post Request started.");
        try (Response response = mHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HTTPRequestException("Response was not successfully retrieved. Code: " + response.code());
            } else {
                JSONObject mJsonObject = new JSONObject(response.body().string());
                Log.i(TAG, "Successfully received POST response.");
                mAccessToken = mJsonObject.getString("access_token");
                mPreferencesEditor.putString(PREF_ACCESS_TOKEN, mAccessToken);
                mPreferencesEditor.apply();
                mRefreshToken = mJsonObject.getString("refresh_token");
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
                    Toast.makeText(getApplicationContext(), "Error in Application. Please review Post Request of OAuthBackgroundService.java", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendGetHTTPRequest(Request request) {
        Handler mainHandler = new Handler(getMainLooper());

        Log.i(TAG, "Get Request started.");
        try (Response response = mHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HTTPRequestException("Response was not successfully retrieved. Code: " + response.code());
            } else {
                Log.i(TAG, "Successfully received GET response.");
                JSONObject mJsonObject = new JSONObject(response.body().string());
                JSONObject mResponseObject = mJsonObject.getJSONObject("response");
                JSONObject mUserObject = mResponseObject.getJSONObject("user");
                mUsername = mUserObject.getString("email");
                mPreferencesEditor.putString(PREF_USERNAME, mUsername);
                mPreferencesEditor.apply();
                JSONObject mAccessTokenObject = mResponseObject.getJSONObject("access_token");
                mExpirationDate = mAccessTokenObject.getString("expiration_date");
                mPreferencesEditor.putString(PREF_EXPIRATION_DATE, mExpirationDate);
                mPreferencesEditor.apply();
                Bundle bundle_token = new Bundle();
                bundle_token.putBoolean("isValid", true);
                mReceiver.send(AUTH_VALIDITY, bundle_token);
            }
            response.body().close();
        } catch (IOException | JSONException | HTTPRequestException e) {
            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Error in Application. Please review Get Request of OAuthBackgroundService.java", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void cleanAuthCode() {
        Log.i(TAG, "Cleaning cache objects.");
        mPreferencesEditor.remove(PREF_AUTH_CODE);
        mPreferencesEditor.apply();
        mPreferencesEditor.remove(PREF_PKCE_CODE_VERIFIER_KEY);
        mPreferencesEditor.apply();
    }

}


