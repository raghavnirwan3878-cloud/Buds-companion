package com.budscompanion.app;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private String selectedTheme;
    private String selectedAppearance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        selectedTheme = prefs.getString(ThemeManager.PREF_THEME, "liquid");
        selectedAppearance = prefs.getString(ThemeManager.PREF_APPEARANCE, ThemeManager.APPEARANCE_SYSTEM);
        ThemeManager.applyNightMode(prefs);
        setTheme(ThemeManager.themeRes(this, selectedTheme, prefs));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindThemeControls();
        bindAppearanceControls();
        bindAccentControls();
        ThemeManager.applyToRoot(this, findViewById(R.id.settings_root));

        View back = findViewById(R.id.settings_back_button);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    private void bindThemeControls() {
        RadioButton liquid = findViewById(R.id.theme_liquid);
        RadioButton classic = findViewById(R.id.theme_classic);
        liquid.setChecked("liquid".equals(selectedTheme));
        classic.setChecked("classic".equals(selectedTheme));
        liquid.setOnClickListener(v -> saveTheme("liquid"));
        classic.setOnClickListener(v -> saveTheme("classic"));
    }

    private void bindAppearanceControls() {
        RadioButton system = findViewById(R.id.appearance_system);
        RadioButton light = findViewById(R.id.appearance_light);
        RadioButton dark = findViewById(R.id.appearance_dark);
        system.setChecked(ThemeManager.APPEARANCE_SYSTEM.equals(selectedAppearance));
        light.setChecked(ThemeManager.APPEARANCE_LIGHT.equals(selectedAppearance));
        dark.setChecked(ThemeManager.APPEARANCE_DARK.equals(selectedAppearance));
        system.setOnClickListener(v -> saveAppearance(ThemeManager.APPEARANCE_SYSTEM));
        light.setOnClickListener(v -> saveAppearance(ThemeManager.APPEARANCE_LIGHT));
        dark.setOnClickListener(v -> saveAppearance(ThemeManager.APPEARANCE_DARK));
    }

    private void bindAccentControls() {
        LinearLayout palette = findViewById(R.id.accent_palette);
        int current = ThemeManager.accent(prefs);
        for (int color : ThemeManager.PRESET_COLORS) addSwatch(palette, color, current == color);

        TextView custom = findViewById(R.id.custom_color_button);
        custom.setOnClickListener(v -> showCustomColorDialog());
    }

    private void addSwatch(LinearLayout palette, int color, boolean selected) {
        TextView swatch = new TextView(this);
        int size = dp(42);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        swatch.setLayoutParams(lp);
        swatch.setGravity(Gravity.CENTER);
        swatch.setBackground(ThemeManager.swatch(color, selected));
        swatch.setContentDescription("Accent color");
        swatch.setOnClickListener(v -> saveAccent(color));
        palette.addView(swatch);
    }

    private void showCustomColorDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("#6E78FF");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(String.format("#%06X", ThemeManager.accent(prefs) & 0xFFFFFF));

        int pad = dp(22);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, dp(4), pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));

        new AlertDialog.Builder(this)
                .setTitle("Custom accent color")
                .setMessage("Enter a hex color such as #6E78FF")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    try {
                        String value = input.getText().toString().trim();
                        if (!value.startsWith("#")) value = "#" + value;
                        int color = Color.parseColor(value);
                        saveAccent(color);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, "Invalid color. Use a value like #6E78FF.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void saveTheme(String theme) {
        prefs.edit().putString(ThemeManager.PREF_THEME, theme).apply();
        finish();
    }

    private void saveAppearance(String mode) {
        prefs.edit().putString(ThemeManager.PREF_APPEARANCE, mode).apply();
        ThemeManager.applyNightMode(prefs);
        finish();
    }

    private void saveAccent(int color) {
        prefs.edit().putInt(ThemeManager.PREF_ACCENT, color).apply();
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
