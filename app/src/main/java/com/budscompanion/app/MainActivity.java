package com.budscompanion.app;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 1;

    private TextView statusText;
    private TextView deviceText;
    private TextView debugText;
    private TextView debugStatusText;
    private TextView dataReceivingText;

    private ProgressBar leftBatteryBar, rightBatteryBar, caseBatteryBar;
    private TextView leftBatteryText, rightBatteryText, caseBatteryText;

    private TextView detailDeviceText, detailConnectionText, detailDataReceivingText, detailInfoText;
    private ProgressBar detailLeftBatteryBar, detailRightBatteryBar, detailCaseBatteryBar;
    private TextView detailLeftBatteryText, detailRightBatteryText, detailCaseBatteryText;

    private boolean detailsScreen = false;
    private boolean receiverRegistered = false;
    private String appliedTheme;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        appliedTheme = themePrefs.getString("theme", "liquid");
        setTheme("classic".equals(appliedTheme)
                ? R.style.Theme_BudsCompanionClassic
                : R.style.Theme_BudsCompanion);

        super.onCreate(savedInstanceState);
        showDevicesScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentTheme = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE)
                .getString("theme", "liquid");
        if (appliedTheme != null && !appliedTheme.equals(currentTheme)) {
            appliedTheme = currentTheme;
            recreate();
            return;
        }
        if (!receiverRegistered) {
            registerReceiver(updateReceiver,
                    new IntentFilter(BudsConnectionService.ACTION_LEVELS_UPDATED),
                    Context.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        refreshStatus();
    }

    @Override
    protected void onPause() {
        if (receiverRegistered) {
            unregisterReceiver(updateReceiver);
            receiverRegistered = false;
        }
        super.onPause();
    }

    private void showDevicesScreen() {
        detailsScreen = false;
        setContentView(R.layout.activity_main);
        bindViews();

        View detailsButton = findViewById(R.id.device_details_button);
        if (detailsButton != null) {
            detailsButton.setOnClickListener(v -> showDeviceDetailsScreen());
        }

        View settingsButton = findViewById(R.id.settings_button);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));
        }

        Button chooseDeviceBtn = findViewById(R.id.choose_device_button);
        Button startServiceBtn = findViewById(R.id.start_service_button);
        if (chooseDeviceBtn != null) chooseDeviceBtn.setOnClickListener(v -> chooseDeviceDialog());
        if (startServiceBtn != null) startServiceBtn.setOnClickListener(v -> requestPermissionsThenStart());
        refreshStatus();
    }

    private void showDeviceDetailsScreen() {
        SharedPreferences prefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        if (prefs.getString(BudsConnectionService.PREF_MAC, null) == null) {
            Toast.makeText(this, "Choose your earbuds first", Toast.LENGTH_SHORT).show();
            return;
        }

        detailsScreen = true;
        setContentView(R.layout.activity_device_details);
        bindViews();

        View backButton = findViewById(R.id.back_button);
        if (backButton != null) backButton.setOnClickListener(v -> showDevicesScreen());
        refreshStatus();
    }

    private void bindViews() {
        statusText = findViewById(R.id.status_text);
        deviceText = findViewById(R.id.device_text);
        debugText = findViewById(R.id.debug_text);
        debugStatusText = findViewById(R.id.debug_status_text);
        dataReceivingText = findViewById(R.id.data_receiving_text);

        leftBatteryBar = findViewById(R.id.left_battery_bar);
        rightBatteryBar = findViewById(R.id.right_battery_bar);
        caseBatteryBar = findViewById(R.id.case_battery_bar);
        leftBatteryText = findViewById(R.id.left_battery_text);
        rightBatteryText = findViewById(R.id.right_battery_text);
        caseBatteryText = findViewById(R.id.case_battery_text);

        detailDeviceText = findViewById(R.id.detail_device_text);
        detailConnectionText = findViewById(R.id.detail_connection_text);
        detailDataReceivingText = findViewById(R.id.detail_data_receiving_text);
        detailInfoText = findViewById(R.id.detail_info_text);
        detailLeftBatteryBar = findViewById(R.id.detail_left_battery_bar);
        detailRightBatteryBar = findViewById(R.id.detail_right_battery_bar);
        detailCaseBatteryBar = findViewById(R.id.detail_case_battery_bar);
        detailLeftBatteryText = findViewById(R.id.detail_left_battery_text);
        detailRightBatteryText = findViewById(R.id.detail_right_battery_text);
        detailCaseBatteryText = findViewById(R.id.detail_case_battery_text);
    }

    private void refreshStatus() {
        SharedPreferences prefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        String mac = prefs.getString(BudsConnectionService.PREF_MAC, null);
        boolean connected = prefs.getBoolean(BudsConnectionService.PREF_CONNECTED, false);
        boolean receiving = connected;

        if (deviceText != null) {
            deviceText.setText(mac != null ? "Selected device: " + mac : "No device selected yet");
        }
        if (detailDeviceText != null) {
            detailDeviceText.setText(mac != null ? mac : "No device selected");
        }

        int l = prefs.getInt(BudsConnectionService.PREF_LEFT, -1);
        int r = prefs.getInt(BudsConnectionService.PREF_RIGHT, -1);
        int c = prefs.getInt(BudsConnectionService.PREF_CASE, -1);
        long caseTs = prefs.getLong(BudsConnectionService.PREF_CASE_TS, 0);
        boolean caseFresh = connected && (System.currentTimeMillis() - caseTs) < 25_000L;

        if (!connected) {
            if (statusText != null) statusText.setText("Disconnected");
            updateBatteryGauge(leftBatteryBar, leftBatteryText, -1, false);
            updateBatteryGauge(rightBatteryBar, rightBatteryText, -1, false);
            updateBatteryGauge(caseBatteryBar, caseBatteryText, -1, false);
        } else {
            StringBuilder sb = new StringBuilder();
            if (l >= 0) sb.append("Left: ").append(l).append("%\n");
            if (r >= 0) sb.append("Right: ").append(r).append("%\n");
            if (c >= 0 && caseFresh) sb.append("Case: ").append(c).append("%\n");
            if (statusText != null) statusText.setText(sb.length() > 0 ? sb.toString().trim() : "Connected - waiting for a reading…");
            updateBatteryGauge(leftBatteryBar, leftBatteryText, l, true);
            updateBatteryGauge(rightBatteryBar, rightBatteryText, r, true);
            updateBatteryGauge(caseBatteryBar, caseBatteryText, c, caseFresh);
        }

        updateBatteryGauge(detailLeftBatteryBar, detailLeftBatteryText, l, connected);
        updateBatteryGauge(detailRightBatteryBar, detailRightBatteryText, r, connected);
        updateBatteryGauge(detailCaseBatteryBar, detailCaseBatteryText, c, caseFresh);

        String connectionLabel = connected ? "Connected" : "Disconnected";
        String receivingLabel = receiving ? "Data receiving: ON" : "Data receiving: OFF";
        if (detailConnectionText != null) detailConnectionText.setText(connectionLabel);
        if (dataReceivingText != null) dataReceivingText.setText(receivingLabel);
        if (detailDataReceivingText != null) detailDataReceivingText.setText(receiving ? "ON" : "OFF");

        if (detailInfoText != null) {
            detailInfoText.setText(
                    "Address: " + (mac != null ? mac : "—") + "\n" +
                    "Connection: " + connectionLabel + "\n" +
                    "Data receiving: " + (receiving ? "ON" : "OFF"));
        }

        boolean subAcked = prefs.getBoolean(BudsConnectionService.PREF_SUBSCRIPTION_ACKED, false);
        boolean gotPush = prefs.getBoolean(BudsConnectionService.PREF_GOT_PUSH_UPDATE, false);
        if (debugStatusText != null) {
            debugStatusText.setText(
                    "Subscription acknowledged: " + (subAcked ? "YES" : "no") + "\n" +
                    "Received a pushed update: " + (gotPush ? "YES" : "no (relying on polling)"));
        }

        if (debugText != null) {
            String rawLog = prefs.getString(BudsConnectionService.PREF_LAST_RAW, "");
            debugText.setText(rawLog.isEmpty() ? "(no data received from buds yet)" : rawLog);
        }
    }

    private void updateBatteryGauge(ProgressBar bar, TextView label, int value, boolean valid) {
        if (bar == null || label == null) return;
        boolean show = valid && value >= 0 && value <= 100;
        bar.setProgress(show ? value : 0);
        label.setText(show ? value + "%" : "—");
    }

    @Override
    public void onBackPressed() {
        if (detailsScreen) {
            showDevicesScreen();
        } else {
            super.onBackPressed();
        }
    }

    /** Let the user pick from already-paired Bluetooth devices. */
    private void chooseDeviceDialog() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            requestPermissionsThenStart();
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "No Bluetooth adapter on this device", Toast.LENGTH_LONG).show();
            return;
        }

        Set<BluetoothDevice> bonded;
        try {
            bonded = adapter.getBondedDevices();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission needed", Toast.LENGTH_LONG).show();
            return;
        }

        if (bonded.isEmpty()) {
            Toast.makeText(this, "No paired devices found - pair your earbuds in system Bluetooth settings first", Toast.LENGTH_LONG).show();
            return;
        }

        List<BluetoothDevice> devices = new ArrayList<>(bonded);
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            try {
                String name = devices.get(i).getName();
                names[i] = (name == null ? "Unknown device" : name) + "  (" + devices.get(i).getAddress() + ")";
            } catch (SecurityException e) {
                names[i] = devices.get(i).getAddress();
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select your earbuds")
                .setItems(names, (dialog, which) -> {
                    String selectedMac = devices.get(which).getAddress();
                    getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(BudsConnectionService.PREF_MAC, selectedMac)
                            .apply();
                    refreshStatus();
                    Toast.makeText(this, "Saved. Tap 'Start monitoring' next.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void requestPermissionsThenStart() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) needed.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) needed.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_PERMISSIONS);
            return;
        }

        startService();
    }

    private void startService() {
        SharedPreferences prefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        if (prefs.getString(BudsConnectionService.PREF_MAC, null) == null) {
            Toast.makeText(this, "Choose your earbuds first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent svc = new Intent(this, BudsConnectionService.class);
        ContextCompat.startForegroundService(this, svc);
        Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            startService();
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
