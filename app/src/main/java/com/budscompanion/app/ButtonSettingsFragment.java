package com.budscompanion.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;

/**
 * Screen 4 — Button / Touch config.
 *
 * Per-side (Left / Right), per-gesture (Double tap, Triple tap, Hold):
 * shows a Spinner for each gesture with the available actions.
 *
 * On "Save", encodes and sends TOUCH_CONFIG_SET via BudsConnectionService.
 */
public class ButtonSettingsFragment extends Fragment {

    private static final String PREFS = "buds_prefs";

    // Spinner refs: [side][gesture] — side 0=Left 1=Right, gesture 0=Double 1=Triple 2=Hold
    private Spinner[][] spinners = new Spinner[2][3];

    private static final String[] ACTIONS = {
            "None",
            "Play / Pause",
            "Next track",
            "Previous track",
            "Volume up",
            "Volume down",
            "Noise control",   // T200 only — harmless to show, service ignores if unsupported
            "Game mode"        // T200 only
    };

    // Pref keys: e.g. "touch_left_double", "touch_right_hold"
    private static final String[][] PREF_KEYS = {
            {"touch_left_double",  "touch_left_triple",  "touch_left_hold"},
            {"touch_right_double", "touch_right_triple", "touch_right_hold"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_button_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, 0);

        // Left side spinners
        spinners[0][0] = view.findViewById(R.id.spinner_left_double);
        spinners[0][1] = view.findViewById(R.id.spinner_left_triple);
        spinners[0][2] = view.findViewById(R.id.spinner_left_hold);

        // Right side spinners
        spinners[1][0] = view.findViewById(R.id.spinner_right_double);
        spinners[1][1] = view.findViewById(R.id.spinner_right_triple);
        spinners[1][2] = view.findViewById(R.id.spinner_right_hold);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ACTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for (int side = 0; side < 2; side++) {
            for (int gesture = 0; gesture < 3; gesture++) {
                spinners[side][gesture].setAdapter(adapter);
                // Restore saved selection
                String saved = prefs.getString(PREF_KEYS[side][gesture], "None");
                int pos = findIndex(saved);
                spinners[side][gesture].setSelection(pos);
            }
        }

        MaterialButton btnSave = view.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> save(prefs));
    }

    private int findIndex(String value) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].equals(value)) return i;
        }
        return 0;
    }

    private void save(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        for (int side = 0; side < 2; side++) {
            for (int gesture = 0; gesture < 3; gesture++) {
                String selected = (String) spinners[side][gesture].getSelectedItem();
                editor.putString(PREF_KEYS[side][gesture], selected);
            }
        }
        editor.apply();

        // Fire off to service to encode and send on current connection
        Intent intent = new Intent(requireContext(), BudsConnectionService.class);
        intent.setAction(BudsConnectionService.ACTION_SET_TOUCH_CONFIG);
        requireContext().startService(intent);

        Navigation.findNavController(requireView()).navigateUp();
    }
}
