package android.example.findlocation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Random;
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
                Intent changeScreenIntent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(changeScreenIntent);
                finish();
            }
                       },2000);

    }
}
