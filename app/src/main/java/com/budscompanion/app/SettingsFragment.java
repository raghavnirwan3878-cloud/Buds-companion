package com.budscompanion.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;

/**
 * Screen 6 — Settings.
 *
 * Notification toggles:
 *   - Connection status notifications
 *   - Low battery alerts (with threshold SeekBar)
 *
 * Theme picker:
 *   - Classic (Material 3 solid)
 *   - Liquid Glass (frosted glassmorphic dark)
 *
 * Accent color picker: ColorWheelView (custom view).
 */
public class SettingsFragment extends Fragment {

    private static final String PREFS = "buds_prefs";

    private SharedPreferences prefs;
    private Switch switchConnectionNotif;
    private Switch switchLowBattery;
    private SeekBar seekLowThreshold;
    private TextView tvThresholdValue;
    private MaterialButton btnThemeClassic, btnThemeLiquid;
    private ColorWheelView colorWheel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS, 0);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // --- Notification toggles ---
        switchConnectionNotif = view.findViewById(R.id.switch_connection_notif);
        switchLowBattery      = view.findViewById(R.id.switch_low_battery_notif);
        seekLowThreshold      = view.findViewById(R.id.seek_low_threshold);
        tvThresholdValue      = view.findViewById(R.id.tv_threshold_value);

        switchConnectionNotif.setChecked(prefs.getBoolean("notif_connection", true));
        switchLowBattery.setChecked(prefs.getBoolean("notif_low_battery", true));

        int threshold = prefs.getInt("low_threshold", 20);
        seekLowThreshold.setProgress(threshold);
        tvThresholdValue.setText(threshold + "%");

        switchConnectionNotif.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("notif_connection", checked).apply());

        switchLowBattery.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("notif_low_battery", checked).apply();
            seekLowThreshold.setEnabled(checked);
        });
        seekLowThreshold.setEnabled(prefs.getBoolean("notif_low_battery", true));

        seekLowThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tvThresholdValue.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                prefs.edit().putInt("low_threshold", s.getProgress()).apply();
            }
        });

        // --- Theme picker ---
        btnThemeClassic = view.findViewById(R.id.btn_theme_classic);
        btnThemeLiquid  = view.findViewById(R.id.btn_theme_liquid);

        String currentTheme = prefs.getString("theme", "classic");
        updateThemeButtons(currentTheme);

        btnThemeClassic.setOnClickListener(v -> applyTheme("classic"));
        btnThemeLiquid.setOnClickListener(v  -> applyTheme("liquid"));

        // --- Accent color wheel ---
        colorWheel = view.findViewById(R.id.color_wheel);
        int savedColor = prefs.getInt("accent_color", 0xFF3098AC);
        colorWheel.setColor(savedColor);
        colorWheel.setOnColorChangedListener(color -> {
            prefs.edit().putInt("accent_color", color).apply();
            // Notify activity to re-tint
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).applyAccentColor(color);
            }
        });
    }

    private void applyTheme(String theme) {
        prefs.edit().putString("theme", theme).apply();
        updateThemeButtons(theme);
        // Full theme switch requires recreate
        if (getActivity() != null) getActivity().recreate();
    }

    private void updateThemeButtons(String theme) {
        boolean isClassic = "classic".equals(theme);
        btnThemeClassic.setStrokeWidth(isClassic ? 4 : 0);
        btnThemeLiquid.setStrokeWidth(isClassic  ? 0 : 4);
    }
}
