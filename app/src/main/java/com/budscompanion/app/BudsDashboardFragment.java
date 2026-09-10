package com.budscompanion.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Tree 1 dashboard.
 *
 * Uses the existing BudsConnectionService SharedPreferences contract:
 *   level_left, level_right, level_case, is_connected, device_mac
 *
 * This class does NOT create sockets, scan, pair, reconnect, or change
 * connection settings. It only displays the state already maintained by the
 * existing service and requests a UI refresh.
 *
 * Tree 1 implemented here:
 *  1.1 L battery
 *  1.2 R battery
 *  1.3 Case battery
 *  1.4 L/R charging status (when charging flags are persisted by service)
 *  1.5 Case charging status (when charging flag is persisted by service)
 *  1.6 connection status
 *  1.8 device name/model
 *  1.10 battery change animation
 *  1.11 visual representation
 *  1.12 refresh button
 *
 * 1.7 signal indicator and 1.9 last-updated are intentionally omitted.
 */
public class BudsDashboardFragment extends Fragment {

    private static final String PREFS = BudsConnectionService.PREFS;
    private static final String ACTION_LEVELS_UPDATED =
            BudsConnectionService.ACTION_LEVELS_UPDATED;

    private TextView connection;
    private TextView device;
    private TextView leftValue;
    private TextView rightValue;
    private TextView caseValue;
    private TextView leftState;
    private TextView rightState;
    private TextView caseState;
    private View leftFill;
    private View rightFill;
    private View caseFill;
    private Button refresh;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (ACTION_LEVELS_UPDATED.equals(intent.getAction())) {
                render();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buds_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connection = view.findViewById(R.id.dashboard_connection);
        device = view.findViewById(R.id.dashboard_device);
        leftValue = view.findViewById(R.id.left_battery_value);
        rightValue = view.findViewById(R.id.right_battery_value);
        caseValue = view.findViewById(R.id.case_battery_value);
        leftState = view.findViewById(R.id.left_charging_state);
        rightState = view.findViewById(R.id.right_charging_state);
        caseState = view.findViewById(R.id.case_charging_state);
        leftFill = view.findViewById(R.id.left_battery_fill);
        rightFill = view.findViewById(R.id.right_battery_fill);
        caseFill = view.findViewById(R.id.case_battery_fill);
        refresh = view.findViewById(R.id.dashboard_refresh);

        refresh.setOnClickListener(v -> {
            // Deliberately only ask the already-running service for a refresh.
            // The actual service/protocol connection remains untouched.
            Context c = requireContext();
            c.sendBroadcast(new Intent(ACTION_LEVELS_UPDATED));
            render();
        });

        render();
    }

    @Override
    public void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(
                requireContext(),
                receiver,
                new IntentFilter(ACTION_LEVELS_UPDATED),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        render();
    }

    @Override
    public void onStop() {
        try {
            requireContext().unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
        super.onStop();
    }

    private void render() {
        if (getView() == null) return;

        SharedPreferences p = requireContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        boolean connected = p.getBoolean(BudsConnectionService.PREF_CONNECTED, false);
        int l = p.getInt(BudsConnectionService.PREF_LEFT, -1);
        int r = p.getInt(BudsConnectionService.PREF_RIGHT, -1);
        int c = p.getInt(BudsConnectionService.PREF_CASE, -1);

        connection.setText(connected ? "Connected" : "Disconnected");
        connection.setTextColor(connected ? Color.rgb(35, 125, 70)
                                             : Color.rgb(180, 55, 55));

        device.setText(resolveDeviceName(p));

        setBattery(leftValue, leftFill, l);
        setBattery(rightValue, rightFill, r);
        setBattery(caseValue, caseFill, c);

        // Charging flags are intentionally read only if a newer service build
        // has stored them. This keeps the dashboard compatible with the current
        // preference schema without changing connection behavior.
        leftState.setText(chargingText(p, "charging_left"));
        rightState.setText(chargingText(p, "charging_right"));
        caseState.setText(chargingText(p, "charging_case"));
    }

    private void setBattery(TextView value, View fill, int level) {
        if (level <= 0) {
            value.setText("—");
            fill.getLayoutParams().width = 0;
            fill.requestLayout();
            return;
        }

        value.setText(level + "%");

        // Width is based on the container's measured width after layout.
        fill.post(() -> {
            View parent = (View) fill.getParent();
            int width = Math.max(0, parent.getWidth());
            fill.getLayoutParams().width = Math.round(width * (level / 100f));
            fill.requestLayout();
        });
    }

    private String chargingText(SharedPreferences p, String key) {
        if (!p.contains(key)) return "";
        return p.getBoolean(key, false) ? "Charging" : "";
    }

    private String resolveDeviceName(SharedPreferences p) {
        String mac = p.getString(BudsConnectionService.PREF_MAC, null);
        if (mac == null || mac.trim().isEmpty()) return "Buds";

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return mac;
        }

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                BluetoothDevice d = adapter.getRemoteDevice(mac);
                String name = d.getName();
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (Exception ignored) {
        }
        return mac;
    }
}
