package com.budscompanion.app;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

/**
 * Starts monitoring when our saved earbuds' A2DP (music) profile actually
 * connects, and stops immediately when it disconnects.
 *
 * This deliberately tracks the A2DP profile's own connection state rather
 * than the raw ACL link. The raw link can stay up even after the user
 * manually disconnects the device in system Bluetooth settings, if
 * something else - including our own RFCOMM socket - is still holding it
 * open. A2DP's connection state reflects what Bluetooth settings actually
 * shows, so reacting to it keeps us in sync with the user's real intent,
 * and - critically - makes sure we close our own socket the moment the
 * user disconnects, instead of leaving the earbuds "occupied" and unable
 * to pair with another device.
 */
public class BluetoothConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            return;
        }

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

        int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);

        if (state == BluetoothProfile.STATE_CONNECTED) {
            boolean monitoringEnabled = prefs.getBoolean(BudsConnectionService.PREF_MONITORING_ENABLED, true);
            if (!monitoringEnabled) {
                return; // user turned monitoring off manually - don't override that
            }
            ContextCompat.startForegroundService(context, new Intent(context, BudsConnectionService.class));
        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            // Stopping the service closes our RFCOMM socket right away -
            // if our own channel was the one thing still keeping the
            // earbuds' Bluetooth link alive, this is what actually
            // releases them to pair with another device.
            context.stopService(new Intent(context, BudsConnectionService.class));
        }
    }
}
