package com.budscompanion.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class BudsTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityAndCollapse(intent);
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        SharedPreferences prefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        boolean connected = prefs.getBoolean(BudsConnectionService.PREF_CONNECTED, false);
        int left = prefs.getInt(BudsConnectionService.PREF_LEFT, -1);
        int right = prefs.getInt(BudsConnectionService.PREF_RIGHT, -1);
        int caseLevel = prefs.getInt(BudsConnectionService.PREF_CASE, -1);
        long caseTs = prefs.getLong(BudsConnectionService.PREF_CASE_TS, 0);
        boolean caseFresh = (System.currentTimeMillis() - caseTs) < 25_000L;

        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_headphones));
        tile.setLabel("Devices");

        if (!connected) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle("Not connected");
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            StringBuilder sb = new StringBuilder();
            if (left > 0) sb.append("L").append(left).append("% ");
            if (right > 0) sb.append("R").append(right).append("% ");
            if (caseLevel > 0 && caseFresh) sb.append("Case").append(caseLevel).append("%");
            tile.setSubtitle(sb.length() > 0 ? sb.toString().trim() : "Connected");
        }
        tile.updateTile();
    }
}
