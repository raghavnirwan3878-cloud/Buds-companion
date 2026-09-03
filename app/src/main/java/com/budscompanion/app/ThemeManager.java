package com.budscompanion.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;

/** Centralized appearance handling for Liquid Glass and Classic themes. */
public final class ThemeManager {
    public static final String PREF_THEME = "theme";
    public static final String PREF_APPEARANCE = "appearance_mode";
    public static final String PREF_ACCENT = "accent_color";
    public static final String APPEARANCE_SYSTEM = "system";
    public static final String APPEARANCE_LIGHT = "light";
    public static final String APPEARANCE_DARK = "dark";

    public static final int DEFAULT_ACCENT = Color.rgb(110, 120, 255);
    public static final int[] PRESET_COLORS = {
            Color.rgb(91, 112, 255),   // blue
            Color.rgb(126, 87, 255),   // purple
            Color.rgb(226, 83, 166),   // pink
            Color.rgb(239, 82, 82),    // red
            Color.rgb(245, 137, 52),   // orange
            Color.rgb(240, 183, 62),   // amber
            Color.rgb(69, 181, 105),   // green
            Color.rgb(48, 190, 205)    // cyan
    };

    private ThemeManager() {}

    public static int accent(SharedPreferences prefs) {
        return prefs.getInt(PREF_ACCENT, DEFAULT_ACCENT);
    }

    public static String configKey(SharedPreferences prefs) {
        return prefs.getString(PREF_THEME, "liquid") + "|"
                + prefs.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM) + "|"
                + accent(prefs);
    }

    public static void applyNightMode(SharedPreferences prefs) {
        String mode = prefs.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        int value;
        if (APPEARANCE_LIGHT.equals(mode)) value = AppCompatDelegate.MODE_NIGHT_NO;
        else if (APPEARANCE_DARK.equals(mode)) value = AppCompatDelegate.MODE_NIGHT_YES;
        else value = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        AppCompatDelegate.setDefaultNightMode(value);
    }

    public static int themeRes(Context context, String theme, SharedPreferences prefs) {
        boolean dark = isDark(context, prefs);
        if ("classic".equals(theme)) {
            return dark ? R.style.Theme_BudsCompanionClassicDark : R.style.Theme_BudsCompanionClassic;
        }
        return dark ? R.style.Theme_BudsCompanion : R.style.Theme_BudsCompanionLiquidLight;
    }

    public static boolean isDark(Context context, SharedPreferences prefs) {
        String mode = prefs.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        if (APPEARANCE_DARK.equals(mode)) return true;
        if (APPEARANCE_LIGHT.equals(mode)) return false;
        return (context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyToRoot(Context context, View root) {
        if (root == null) return;
        SharedPreferences prefs = context.getSharedPreferences(BudsConnectionService.PREFS, Context.MODE_PRIVATE);
        int accent = accent(prefs);
        boolean dark = isDark(context, prefs);
        root.setBackground(createBackground(accent, dark));
        tintViews(root, accent, dark);
    }

    private static GradientDrawable createBackground(int accent, boolean dark) {
        int base = dark ? Color.rgb(8, 12, 22) : Color.rgb(242, 245, 249);
        int glow1 = ColorUtils.setAlphaComponent(accent, dark ? 65 : 48);
        int glow2 = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(accent, Color.WHITE, .35f), dark ? 48 : 38);
        int edge = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(accent, Color.BLACK, .45f), dark ? 28 : 20);
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{base, glow1, glow2, base, edge});
        g.setCornerRadius(0);
        return g;
    }

    private static void tintViews(View view, int accent, boolean dark) {
        if (view instanceof Button) {
            Button b = (Button) view;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(accent);
            bg.setCornerRadius(dp(b.getContext(), 28));
            b.setBackground(bg);
            b.setTextColor(Color.WHITE);
        } else if (view instanceof ProgressBar) {
            ProgressBar bar = (ProgressBar) view;
            bar.setProgressTintList(ColorStateList.valueOf(accent));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(dark ? Color.WHITE : Color.BLACK, 42)));
        } else if (view instanceof RadioButton) {
            ((RadioButton) view).setButtonTintList(ColorStateList.valueOf(accent));
        } else if (view instanceof TextView) {
            TextView text = (TextView) view;
            String idName = "";
            try { idName = text.getResources().getResourceEntryName(text.getId()); } catch (Exception ignored) {}
            if (idName.contains("data_receiving") && text.getText() != null
                    && text.getText().toString().contains("ON")) {
                text.setTextColor(accent);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) tintViews(group.getChildAt(i), accent, dark);
        }
    }

    public static GradientDrawable swatch(int color, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        g.setStroke(selected ? 4 : 2, selected ? Color.WHITE : ColorUtils.setAlphaComponent(Color.WHITE, 120));
        return g;
    }

    public static int contrastText(int color) {
        return ColorUtils.calculateLuminance(color) > .55 ? Color.rgb(18, 20, 28) : Color.WHITE;
    }

    private static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

}
