package android.example.findlocation.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ActiveScanningServiceBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_DISABLE_SERVICE = "android.example.findlocation.NOTIFICATION_CHANNEL.ACTION_DISABLE_SERVICE";
    private static final String LOG_TAG = "LOG_" + ActiveScanningServiceBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, intent.toString());
        if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(LOG_TAG, "Boot Completed");
        } else if (ACTION_DISABLE_SERVICE.equals(intent.getAction())) {
            boolean serviceStopped = context.stopService(new Intent(context, ActiveScanningService.class));
            Log.d(LOG_TAG, "android.example.findlocation.NOTIFICATION_CHANNEL.ACTION_DISABLE_SERVICE: " + serviceStopped);

        }
    }
}
