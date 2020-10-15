package android.example.findlocation;

import androidx.multidex.MultiDexApplication;
import android.content.Context;
import android.content.SharedPreferences;
import android.example.findlocation.exceptions.SharedPreferencesException;
import android.util.Log;

import java.util.Map;
import java.util.Set;

public class IndoorApp extends MultiDexApplication {

    private static final String TAG = IndoorApp.class.getSimpleName();
    public static SharedPreferences appPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        appPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (BuildConfig.DEBUG) displayPreferences();
        /*try {
            resetPreferences();
        } catch (SharedPreferencesException e) {
            e.printStackTrace();
        }*/
        Log.i(TAG, "Starting new instance of Indoor Application");
    }

    private void displayPreferences() {
        Map<String, ?> allPreferences = appPreferences.getAll();
        Set<String> set = allPreferences.keySet();
        for (String s : set) {
            Log.d(TAG, s + "<" + allPreferences.get(s).getClass().getSimpleName() + "> =  "
                    + allPreferences.get(s).toString());
        }
    }

    private void resetPreferences() throws SharedPreferencesException {
        boolean commitResult = appPreferences.edit().clear().commit();
        if (!commitResult) {
            throw new SharedPreferencesException("Exception raised on committing preferences changes.");
        } else Log.i(TAG, "Successfully cleared existing shared preferences.");
    }
}
