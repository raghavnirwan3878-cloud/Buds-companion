package com.budscompanion.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;

/**
 * Screen 5 — Device details.
 * Shows: device name, MAC address, firmware version.
 * "Unpair" clears the saved MAC and navigates back to FirstLaunchFragment.
 */
public class DeviceInfoFragment extends Fragment {

    private static final String PREFS = "buds_prefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, 0);
        String mac = prefs.getString("device_mac", "–");

        TextView tvName     = view.findViewById(R.id.tv_info_name);
        TextView tvMac      = view.findViewById(R.id.tv_info_mac);
        TextView tvFirmware = view.findViewById(R.id.tv_info_firmware);

        // Resolve device name
        String name = resolveDeviceName(mac);
        tvName.setText(name);
        tvMac.setText(mac);

        // Firmware decoded by BudsConnectionService and stored in prefs
        String firmware = prefs.getString("firmware_version", "–");
        tvFirmware.setText(firmware);

        MaterialButton btnUnpair = view.findViewById(R.id.btn_unpair);
        btnUnpair.setOnClickListener(v -> confirmUnpair(prefs));
    }

    private void confirmUnpair(SharedPreferences prefs) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Unpair device")
                .setMessage("This will forget the device. You can re-pair any time.")
                .setPositiveButton("Unpair", (dialog, which) -> {
                    // Stop service first
                    requireContext().stopService(
                            new android.content.Intent(requireContext(), BudsConnectionService.class));

                    // Clear all prefs
                    prefs.edit().clear().apply();

                    // Navigate back to first launch
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_deviceInfo_to_firstLaunch);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String resolveDeviceName(String mac) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && mac != null && !mac.equals("–")) {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(mac);
                String name = device.getName();
                if (name != null && !name.isEmpty()) return name;
            } catch (Exception ignored) {}
        }
        return mac;
    }
}
