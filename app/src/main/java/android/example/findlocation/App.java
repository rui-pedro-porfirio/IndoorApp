package android.example.findlocation;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;
import java.util.Set;

public class App extends Application {

    public static SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences( getPackageName() + "_preferences", MODE_PRIVATE);
        //UNCOMMENT TO TEST FOR THE FIRST AUTH
        Map<String, ?> allPrefs = preferences.getAll(); //your sharedPreference
        Set<String> set = allPrefs.keySet();
        for(String s : set){
            Log.d("TAG", s + "<" + allPrefs.get(s).getClass().getSimpleName() +"> =  "
                    + allPrefs.get(s).toString());
        }
        //boolean result = preferences.edit().clear().commit();*/
        System.out.println("Preferences");
    }
}
