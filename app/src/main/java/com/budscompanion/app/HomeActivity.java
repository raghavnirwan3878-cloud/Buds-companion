package com.budscompanion.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class HomeActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme BEFORE setContentView
        SharedPreferences prefs = getSharedPreferences("buds_prefs", MODE_PRIVATE);
        String theme = prefs.getString("theme", "classic");
        if ("liquid".equals(theme)) {
            setTheme(R.style.Theme_BudsCompanion_LiquidGlass);
        } else {
            setTheme(R.style.Theme_BudsCompanion);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp())
                || super.onSupportNavigateUp();
    }

    /**
     * Called by SettingsFragment when the user picks a new accent color.
     * Stores it in prefs; full re-tint happens on next launch/recreate.
     */
    public void applyAccentColor(int color) {
        getSharedPreferences("buds_prefs", MODE_PRIVATE)
                .edit()
                .putInt("accent_color", color)
                .apply();
    }
}
