package android.example.findlocation.ui.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;

public class BluetoothVerifier {

    private static final String TAG = BluetoothVerifier.class.getSimpleName();

    private Activity mContext;

    public BluetoothVerifier(Activity mContext) {
        this.mContext = mContext;
    }

    public void verifyBluetooth() {
        Log.i(TAG,"Starting the verification on Bluetooth's state.");
        try {
            if (!BeaconManager.getInstanceForApplication(mContext).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //finish();
                        //System.exit(0);
                    }
                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    //finish();
                    //System.exit(0);
                }

            });
            builder.show();

        }
    }
}
