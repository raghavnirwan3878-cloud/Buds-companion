package com.budscompanion.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DeviceDetailFragment extends Fragment {

    private static final String PREFS      = "buds_prefs";
    private static final int    REFRESH_MS = 2000;

    private SharedPreferences prefs;
    private Handler handler;
    private Runnable refreshRunnable;

    private BatteryTickView tickLeft, tickRight, tickCase;
    private TextView tvLeftPct, tvRightPct, tvCasePct;
    private MaterialCardView cardAnc, cardGameMode;
    private Switch switchGameMode;
    private TextView tvAncMode;
    private MaterialButton btnFindBuds;

    private boolean isT200 = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs   = requireContext().getSharedPreferences(PREFS, 0);
        handler = new Handler(Looper.getMainLooper());

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        tickLeft   = view.findViewById(R.id.tick_left);
        tickRight  = view.findViewById(R.id.tick_right);
        tickCase   = view.findViewById(R.id.tick_case);
        tvLeftPct  = view.findViewById(R.id.tv_left_pct);
        tvRightPct = view.findViewById(R.id.tv_right_pct);
        tvCasePct  = view.findViewById(R.id.tv_case_pct);

        btnFindBuds   = view.findViewById(R.id.btn_find_buds);
        cardAnc       = view.findViewById(R.id.card_anc);
        tvAncMode     = view.findViewById(R.id.tv_anc_mode);
        cardGameMode  = view.findViewById(R.id.card_game_mode);
        switchGameMode = view.findViewById(R.id.switch_game_mode);

        view.findViewById(R.id.row_button_settings).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_deviceDetail_to_buttonSettings));
        view.findViewById(R.id.row_device_info).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_deviceDetail_to_deviceInfo));

        btnFindBuds.setOnClickListener(v -> sendFindDevice());
        cardAnc.setOnClickListener(v -> cycleAncMode());
        switchGameMode.setOnCheckedChangeListener((btn, checked) -> sendGameMode(checked));

        detectModel();

        // Show/hide T200-only sections
        int t200Visibility = isT200 ? View.VISIBLE : View.GONE;
        cardAnc.setVisibility(t200Visibility);
        cardGameMode.setVisibility(t200Visibility);

        refreshRunnable = this::refresh;
    }

    private void detectModel() {
        android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        String mac = prefs.getString("device_mac", null);
        if (adapter != null && mac != null) {
            try {
                android.bluetooth.BluetoothDevice device = adapter.getRemoteDevice(mac);
                String name = device.getName();
                if (name != null) isT200 = name.toUpperCase().contains("T200");
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
    }

    private void refresh() {
        int left  = prefs.getInt("level_left",  -1);
        int right = prefs.getInt("level_right", -1);
        int cas   = prefs.getInt("level_case",  -1);

        setSlot(tickLeft,  tvLeftPct,  left);
        setSlot(tickRight, tvRightPct, right);
        setSlot(tickCase,  tvCasePct,  cas);

        // Only update ANC/game mode views if visible (T200)
        if (isT200) {
            String ancMode = prefs.getString("anc_mode", "Off");
            if (tvAncMode != null) tvAncMode.setText(ancMode);

            boolean gameModeOn = prefs.getBoolean("game_mode_enabled", false);
            if (switchGameMode != null) {
                switchGameMode.setOnCheckedChangeListener(null);
                switchGameMode.setChecked(gameModeOn);
                switchGameMode.setOnCheckedChangeListener((btn, checked) -> sendGameMode(checked));
            }
        }

        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
            handler.postDelayed(refreshRunnable, REFRESH_MS);
        }
    }

    private void setSlot(BatteryTickView tick, TextView label, int level) {
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

    private void sendFindDevice() {
        Intent intent = new Intent(requireContext(), BudsConnectionService.class);
        intent.setAction(BudsConnectionService.ACTION_FIND_DEVICE);
        requireContext().startService(intent);
        Toast.makeText(requireContext(), "Finding buds…", Toast.LENGTH_SHORT).show();
    }

    private static final String[] ANC_MODES = {"Off", "ANC", "Transparency"};

    private void cycleAncMode() {
        String current = prefs.getString("anc_mode", "Off");
        int idx = 0;
        for (int i = 0; i < ANC_MODES.length; i++) {
            if (ANC_MODES[i].equals(current)) { idx = i; break; }
        }
        String next = ANC_MODES[(idx + 1) % ANC_MODES.length];
        prefs.edit().putString("anc_mode", next).apply();
        if (tvAncMode != null) tvAncMode.setText(next);

        Intent intent = new Intent(requireContext(), BudsConnectionService.class);
        intent.setAction(BudsConnectionService.ACTION_SET_ANC);
        intent.putExtra(BudsConnectionService.EXTRA_ANC_MODE, next);
        requireContext().startService(intent);
    }

    private void sendGameMode(boolean enabled) {
        prefs.edit().putBoolean("game_mode_enabled", enabled).apply();
        Intent intent = new Intent(requireContext(), BudsConnectionService.class);
        intent.setAction(BudsConnectionService.ACTION_SET_GAME_MODE);
        intent.putExtra(BudsConnectionService.EXTRA_GAME_MODE, enabled);
        requireContext().startService(intent);
    }
}
