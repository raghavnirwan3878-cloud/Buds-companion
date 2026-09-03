package com.budscompanion.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.Set;

/**
 * Screen 1 — shown when no device is paired yet.
 * "Pair device" button opens the system Bluetooth picker (already-paired devices).
 * On success → navigates to DeviceListFragment.
 */
public class FirstLaunchFragment extends Fragment {

    private static final String PREFS = "buds_prefs";
    private static final String PREF_MAC = "device_mac";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_launch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // If a device is already saved, skip straight to device list
        String savedMac = requireContext()
                .getSharedPreferences(PREFS, 0)
                .getString(PREF_MAC, null);
        if (savedMac != null) {
            Navigation.findNavController(view)
                    .navigate(R.id.action_firstLaunch_to_deviceList);
            return;
        }

        Button pairBtn = view.findViewById(R.id.btn_pair_device);
        pairBtn.setOnClickListener(v -> openBluetoothPicker(view));
    }

    private void openBluetoothPicker(@NonNull View rootView) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(requireContext(),
                    "Turn on Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a simple bottom sheet listing already-bonded earbuds
        Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        if (bonded == null || bonded.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No paired devices found. Pair your earbuds in Settings first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        DevicePickerBottomSheet sheet = DevicePickerBottomSheet.newInstance(bonded);
        sheet.setOnDeviceSelected(device -> {
            requireContext().getSharedPreferences(PREFS, 0)
                    .edit()
                    .putString(PREF_MAC, device.getAddress())
                    .apply();
            Navigation.findNavController(rootView)
                    .navigate(R.id.action_firstLaunch_to_deviceList);
        });
        sheet.show(getChildFragmentManager(), "picker");
    }
}
