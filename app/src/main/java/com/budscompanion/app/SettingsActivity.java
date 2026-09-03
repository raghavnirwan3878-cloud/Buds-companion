package com.budscompanion.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private String selectedTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE);
        selectedTheme = prefs.getString("theme", "liquid");
        setTheme("classic".equals(selectedTheme)
                ? R.style.Theme_BudsCompanionClassic
                : R.style.Theme_BudsCompanion);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        RadioButton liquid = findViewById(R.id.theme_liquid);
        RadioButton classic = findViewById(R.id.theme_classic);
        liquid.setChecked("liquid".equals(selectedTheme));
        classic.setChecked("classic".equals(selectedTheme));

        View back = findViewById(R.id.settings_back_button);
        back.setOnClickListener(v -> finish());

        liquid.setOnClickListener(v -> applyTheme("liquid"));
        classic.setOnClickListener(v -> applyTheme("classic"));
    }

    private void applyTheme(String theme) {
        selectedTheme = theme;
        getSharedPreferences(BudsConnectionService.PREFS, MODE_PRIVATE)
                .edit()
                .putString("theme", theme)
                .apply();
        finish();
    }
}
