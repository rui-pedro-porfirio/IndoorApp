package android.example.findlocation.utils;

import android.content.Context;
import android.example.findlocation.R;

import androidx.preference.PreferenceManager;

public class PreferenceUtils {
    /*
    public static final String INDOOR_APP_SERVER_ENDPOINT = "http://192.168.12.1:3101/";
    //public static final String INDOOR_APP_SERVER_ENDPOINT = "https://indoorlocationapp.herokuapp.com/";

    public static final String INDOOR_APP_SERVER_SCANNING_ENDPOINT = INDOOR_APP_SERVER_ENDPOINT + "scanning/";
    public static final String INDOOR_APP_SERVER_TRILATERATION_POSITION_ENDPOINT = INDOOR_APP_SERVER_ENDPOINT + "trilateration/position/";

    public static final String AUTH_SERVER = "http://192.168.12.1:3001/";
    //public static final String AUTH_SERVER = "https://yanux-auth.herokuapp.com/";

    public static final String AUTHORIZE_ADDRESS = AUTH_SERVER + "oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
    public static final String EXCHANGE_AUTH_ADDRESS = AUTH_SERVER + "oauth2/token";
    public static final String VERIFY_AUTH_DATA_ADDRESS = AUTH_SERVER + "api/verify_oauth2";
    */

    public static String getIndoorAppServerEndpoint(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("indoor_app_server_endpoint", context.getString(R.string.indoor_app_server_endpoint_default));
    }

    public static String getAuthServerEndpoint(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("auth_server_endpoint", context.getString(R.string.auth_server_endpoint_default));
    }

    public static String getIndoorAppServerScanningEndpoint(Context context) {
        return getIndoorAppServerEndpoint(context) + "scanning/";
    }

    public static String getIndoorAppServerTrilaterationPositionEndpoint(Context context) {
        return getIndoorAppServerEndpoint(context) + "trilateration/position/";
    }

    public static String getAuthorizeAddress(Context context) {
        return getAuthServerEndpoint(context) + "oauth2/authorize?response_type=code&client_id=indoor-location-app&redirect_uri=indoorapp://auth/redirect";
    }

    public static String getExchangeAuthAddress(Context context) {
        return getAuthServerEndpoint(context) + "oauth2/token";
    }

    public static String getVerifyAuthDataAddress(Context context) {
        return getAuthServerEndpoint(context) + "api/verify_oauth2";
    }
}
