package android.example.findlocation.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AuthorizationBackgroundService extends IntentService {

    private static final String AUTHORIZATION_CODE_REQUEST = "AuthorizationCodeRequest";

    private Handler mHandler;
    private SharedPreferences sharedPreferences;

    public AuthorizationBackgroundService() {
        super("AuthorizationBackgroundSerivce");
        mHandler = new Handler();
        sharedPreferences = App.preferences;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean hasAuthenticationCode = sharedPreferences.getBoolean(AUTHORIZATION_CODE_REQUEST,false);
        if (!hasAuthenticationCode) {
            requestAuthorizationCode();
            editor.putBoolean(AUTHORIZATION_CODE_REQUEST, true);
            editor.commit();
        } else {
            mHandler.post(new DisplayToast(this, "User already logged in YanuX"));
        }
    }

    public void requestAuthorizationCode() {
        String uri = "https://yanux-auth.herokuapp.com/oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
        Intent authenticationCodeRequirementIntent = new Intent("android.intent.action.VIEW", Uri.parse(uri));
        authenticationCodeRequirementIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(authenticationCodeRequirementIntent);
    }

}


