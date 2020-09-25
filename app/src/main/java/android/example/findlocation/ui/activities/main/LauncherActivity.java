package android.example.findlocation.ui.activities.main;

import android.content.Intent;
import android.example.findlocation.R;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = LauncherActivity.class.getSimpleName();
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        changeToMainScreen();
    }

    private void changeToMainScreen(){
        mTimer = new Timer();
        long delayTime = 2000;
        Log.i(TAG,"Launching application.");
        mTimer.schedule(new TimerTask(){
            public void run(){
                Intent changeScreenIntent = new Intent(LauncherActivity.this, MainActivity.class);
                startActivity(changeScreenIntent);
                finish();
            }
        },delayTime);

    }
}
