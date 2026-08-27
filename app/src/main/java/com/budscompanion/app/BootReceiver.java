package com.budscompanion.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(BudsConnectionService.PREFS, Context.MODE_PRIVATE);
        if (prefs.getString(BudsConnectionService.PREF_MAC, null) != null) {
            ContextCompat.startForegroundService(context, new Intent(context, BudsConnectionService.class));
        }
    }
}
