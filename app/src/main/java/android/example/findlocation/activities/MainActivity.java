package android.example.findlocation.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.example.findlocation.R;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Starter Screen
 */
public class MainActivity extends AppCompatActivity {

    Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeToSecondScreen();
    }

    public void changeToSecondScreen(){
        timer = new Timer();
        timer.schedule(new TimerTask(){
            public void run(){
                Intent changeScreenIntent = new Intent(MainActivity.this, MainPageActivity.class);
                startActivity(changeScreenIntent);
                finish();
            }
                       },2000);

    }
}
