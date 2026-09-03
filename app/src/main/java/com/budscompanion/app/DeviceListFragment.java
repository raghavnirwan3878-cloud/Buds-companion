package com.budscompanion.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

/**
 * Screen 2 — Device list.
 * Shows the saved device with live L/R/Case battery tick-gauge.
 * Tap device card → DeviceDetailFragment.
 * Settings icon → SettingsFragment.
 * Add button → FirstLaunchFragment (to pair another device; currently single-device only).
 */
public class DeviceListFragment extends Fragment {

    private static final String PREFS = "buds_prefs";
    private static final int REFRESH_MS = 2000;

    private SharedPreferences prefs;
    private Handler handler;
    private Runnable refreshRunnable;

    // Views
    private TextView tvDeviceName, tvConnectionStatus;
    private BatteryTickView tickLeft, tickRight, tickCase;
    private TextView tvLeftPct, tvRightPct, tvCasePct;
    private CardView cardDevice;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS, 0);
        handler = new Handler(Looper.getMainLooper());

        tvDeviceName      = view.findViewById(R.id.tv_device_name);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        tickLeft          = view.findViewById(R.id.tick_left);
        tickRight         = view.findViewById(R.id.tick_right);
        tickCase          = view.findViewById(R.id.tick_case);
        tvLeftPct         = view.findViewById(R.id.tv_left_pct);
        tvRightPct        = view.findViewById(R.id.tv_right_pct);
        tvCasePct         = view.findViewById(R.id.tv_case_pct);
        cardDevice        = view.findViewById(R.id.card_device);
        emptyState        = view.findViewById(R.id.empty_state);

        ImageButton btnSettings = view.findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_deviceList_to_settings));

        ImageButton btnAdd = view.findViewById(R.id.btn_add_device);
        btnAdd.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_deviceList_to_firstLaunch));

        cardDevice.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_deviceList_to_deviceDetail));

        refreshRunnable = this::refresh;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        handler.postDelayed(refreshRunnable, REFRESH_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void refresh() {
        String mac = prefs.getString("device_mac", null);
        boolean connected = prefs.getBoolean("is_connected", false);

        if (mac == null) {
            cardDevice.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        cardDevice.setVisibility(View.VISIBLE);

        // Resolve device name from bonded set
        String name = resolveDeviceName(mac);
        tvDeviceName.setText(name);
        tvConnectionStatus.setText(connected ? "Connected" : "Disconnected");
        tvConnectionStatus.setAlpha(connected ? 1f : 0.5f);

        int left  = prefs.getInt("level_left",  -1);
        int right = prefs.getInt("level_right", -1);
        int cas   = prefs.getInt("level_case",  -1);

        setSlot(tickLeft,  tvLeftPct,  left,  "L");
        setSlot(tickRight, tvRightPct, right, "R");
        setSlot(tickCase,  tvCasePct,  cas,   "Case");

        // Schedule next refresh
        handler.removeCallbacks(refreshRunnable);
        handler.postDelayed(refreshRunnable, REFRESH_MS);
    }

    private void setSlot(BatteryTickView tick, TextView label, int level, String prefix) {
        if (level < 0) {
            tick.setLevel(0);
            tick.setAlpha(0.3f);
            label.setText("–");
        } else {
            tick.setLevel(level);
            tick.setAlpha(1f);
            label.setText(level + "%");
        }
    }

    private String resolveDeviceName(String mac) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(mac);
                String name = device.getName();
                if (name != null && !name.isEmpty()) return name;
            } catch (Exception ignored) {}
        }
        return mac;
    }
}
