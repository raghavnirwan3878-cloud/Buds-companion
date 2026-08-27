package com.budscompanion.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Holds a persistent classic-Bluetooth (RFCOMM/SPP) connection to the
 * Realme/Oppo earbuds, subscribes to push battery updates, and falls back to
 * periodic polling. Updates the widget and fires low-battery notifications.
 *
 * This intentionally bypasses Gadgetbridge entirely - it talks the vendor
 * protocol directly, based on Gadgetbridge's (AGPLv3) open-source
 * implementation of it.
 */
public class BudsConnectionService extends Service {

    private static final String TAG = "BudsConnection";

    // Same SPP service UUID Gadgetbridge's OppoHeadphonesSupport registers.
    private static final UUID SPP_UUID = UUID.fromString("0000079a-d102-11e1-9b23-00025b00a5a5");

    private static final String CHANNEL_STATUS = "buds_status";
    private static final String CHANNEL_ALERTS = "buds_alerts";
    private static final int NOTIF_ID_STATUS = 1;
    private static final int NOTIF_ID_LOW_BATTERY_BASE = 100;

    public static final String PREFS = "buds_prefs";
    public static final String PREF_MAC = "device_mac";
    public static final String PREF_LEFT = "level_left";
    public static final String PREF_RIGHT = "level_right";
    public static final String PREF_CASE = "level_case";
    public static final String PREF_LOW_THRESHOLD = "low_threshold";
    public static final String ACTION_LEVELS_UPDATED = "com.budscompanion.app.LEVELS_UPDATED";

    // Fallback poll interval - only matters if the push subscription doesn't
    // stick on this firmware; harmless extra traffic otherwise.
    private static final long POLL_INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final long RECONNECT_BASE_DELAY_MS = 5_000L;
    private static final long RECONNECT_MAX_DELAY_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OppoProtocol protocol = new OppoProtocol();
    private final OppoProtocol.FrameReader frameReader = new OppoProtocol.FrameReader();

    private BluetoothSocket socket;
    private OutputStream outStream;
    private Thread readThread;
    private volatile boolean running = false;
    private long reconnectDelay = RECONNECT_BASE_DELAY_MS;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            sendBatteryRequest();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID_STATUS, buildStatusNotification("Connecting\u2026"));
        running = true;
        connectLoop();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(pollRunnable);
        closeSocketQuietly();
        super.onDestroy();
    }

    // ---- Connection handling -------------------------------------------------

    private void connectLoop() {
        new Thread(() -> {
            while (running) {
                try {
                    attemptConnect();
                    reconnectDelay = RECONNECT_BASE_DELAY_MS; // reset backoff on success
                    // block here until the read loop exits (disconnect/error)
                    readLoop();
                } catch (SecurityException se) {
                    Log.e(TAG, "Missing Bluetooth permission", se);
                    updateStatusNotification("Missing Bluetooth permission - open the app");
                    return; // nothing more we can do without the permission
                } catch (Exception e) {
                    Log.w(TAG, "Connection attempt failed: " + e.getMessage());
                }

                closeSocketQuietly();
                handler.post(() -> updateStatusNotification("Reconnecting\u2026"));

                try {
                    Thread.sleep(reconnectDelay);
                } catch (InterruptedException ignored) {
                }
                reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY_MS);
            }
        }, "buds-connect-loop").start();
    }

    private void attemptConnect() throws IOException {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String mac = prefs.getString(PREF_MAC, null);
        if (mac == null) {
            throw new IOException("No paired device configured yet");
        }

        if (!hasBtPermission()) {
            throw new SecurityException("BLUETOOTH_CONNECT not granted");
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IOException("No Bluetooth adapter");
        }
        BluetoothDevice device = adapter.getRemoteDevice(mac);

        adapter.cancelDiscovery();
        socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        socket.connect();
        outStream = socket.getOutputStream();

        Log.i(TAG, "Connected to " + mac);
        handler.post(() -> updateStatusNotification("Connected"));

        // Subscribe to push battery updates, then request an immediate read.
        write(protocol.encodeSubscriptionSet(OppoProtocol.SUB_BATTERY));
        write(protocol.encodeBatteryReq());

        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void readLoop() throws IOException {
        InputStream in = socket.getInputStream();
        byte[] buf = new byte[512];
        while (running) {
            int n = in.read(buf);
            if (n < 0) {
                throw new IOException("Stream closed by remote");
            }
            List<byte[]> frames = frameReader.feed(buf, n);
            for (byte[] frame : frames) {
                handleFrame(frame);
            }
        }
    }

    private void handleFrame(byte[] frame) {
        OppoProtocol.Decoded decoded = OppoProtocol.decodeFrame(frame);
        if (decoded == null || decoded.batteries.isEmpty()) {
            return;
        }
        applyBatteryUpdate(decoded.batteries);
    }

    private void sendBatteryRequest() {
        try {
            write(protocol.encodeBatteryReq());
        } catch (IOException e) {
            Log.w(TAG, "Failed to send poll request: " + e.getMessage());
        }
    }

    private synchronized void write(byte[] data) throws IOException {
        if (outStream == null) {
            throw new IOException("Not connected");
        }
        outStream.write(data);
    }

    private void closeSocketQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        socket = null;
        outStream = null;
    }

    private boolean hasBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // ---- Battery state / widget / notifications -------------------------------------------------

    private void applyBatteryUpdate(List<OppoProtocol.BatteryInfo> batteries) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int threshold = prefs.getInt(PREF_LOW_THRESHOLD, 20);

        for (OppoProtocol.BatteryInfo b : batteries) {
            String key = b.index == 0 ? PREF_LEFT : b.index == 1 ? PREF_RIGHT : PREF_CASE;
            int previous = prefs.getInt(key, -1);
            editor.putInt(key, b.level);

            if (!b.charging && previous > threshold && b.level <= threshold) {
                fireLowBatteryNotification(b);
            }
        }
        editor.apply();

        handler.post(() -> {
            updateStatusNotification(summaryText(prefs));
            BudsWidgetProvider.updateAllWidgets(this);
            sendBroadcast(new Intent(ACTION_LEVELS_UPDATED));
        });
    }

    private String summaryText(SharedPreferences prefs) {
        int l = prefs.getInt(PREF_LEFT, -1);
        int r = prefs.getInt(PREF_RIGHT, -1);
        int c = prefs.getInt(PREF_CASE, -1);
        StringBuilder sb = new StringBuilder();
        if (l >= 0) sb.append("L ").append(l).append("%  ");
        if (r >= 0) sb.append("R ").append(r).append("%  ");
        if (c >= 0) sb.append("Case ").append(c).append("%");
        return sb.length() > 0 ? sb.toString().trim() : "Connected";
    }

    // ---- Notifications -------------------------------------------------

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel status = new NotificationChannel(
                CHANNEL_STATUS, "Connection status", NotificationManager.IMPORTANCE_MIN);
        status.setShowBadge(false);
        nm.createNotificationChannel(status);

        NotificationChannel alerts = new NotificationChannel(
                CHANNEL_ALERTS, "Low battery alerts", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(alerts);
    }

    private Notification buildStatusNotification(String text) {
        PendingIntent openApp = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_STATUS)
                .setContentTitle("Buds Companion")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setOngoing(true)
                .setContentIntent(openApp)
                .build();
    }

    private void updateStatusNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIF_ID_STATUS, buildStatusNotification(text));
        }
    }

    private void fireLowBatteryNotification(OppoProtocol.BatteryInfo b) {
        String slot = b.index == 0 ? "Left earbud" : b.index == 1 ? "Right earbud" : "Case";
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ALERTS)
                .setContentTitle(slot + " battery low")
                .setContentText(slot + " is at " + b.level + "%")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIF_ID_LOW_BATTERY_BASE + b.index, n);
        }
    }
}
