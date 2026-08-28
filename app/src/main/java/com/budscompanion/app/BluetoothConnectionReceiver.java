package com.budscompanion.app;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

/**
 * Starts monitoring the moment our saved earbuds actually connect over
 * Bluetooth, and stops (notification included) the moment they disconnect.
 * This is what lets the app react correctly even if its process was killed
 * in the background - the system delivers these broadcasts (and can wake a
 * killed app's manifest-registered receiver for them) independently of
 * whether our own service/process is still alive.
 */
public class BluetoothConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) return;

        SharedPreferences prefs = context.getSharedPreferences(BudsConnectionService.PREFS, Context.MODE_PRIVATE);
        String savedMac = prefs.getString(BudsConnectionService.PREF_MAC, null);
        if (savedMac == null) return;

        String deviceMac;
        try {
            deviceMac = device.getAddress();
        } catch (SecurityException e) {
            return; // missing permission to read address; nothing we can do here
        }
        if (deviceMac == null || !savedMac.equalsIgnoreCase(deviceMac)) {
            return; // some other Bluetooth device, not our earbuds
        }

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            ContextCompat.startForegroundService(context, new Intent(context, BudsConnectionService.class));
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            // Stopping the service removes its foreground notification
            // automatically - nothing lingers once the earbuds are gone.
            context.stopService(new Intent(context, BudsConnectionService.class));
        }
    }
}
