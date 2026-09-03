package com.budscompanion.app;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bottom sheet listing paired Bluetooth devices.
 * User taps one → onDeviceSelected callback fires.
 */
public class DevicePickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnDeviceSelected {
        void onSelected(BluetoothDevice device);
    }

    private List<BluetoothDevice> devices;
    private OnDeviceSelected listener;

    public static DevicePickerBottomSheet newInstance(Set<BluetoothDevice> bonded) {
        DevicePickerBottomSheet sheet = new DevicePickerBottomSheet();
        sheet.devices = new ArrayList<>(bonded);
        return sheet;
    }

    public void setOnDeviceSelected(OnDeviceSelected listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_device_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rv = view.findViewById(R.id.rv_devices);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new Adapter());
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device_picker, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            BluetoothDevice device = devices.get(position);
            String name = device.getName();
            holder.tvName.setText(name != null ? name : "Unknown device");
            holder.tvMac.setText(device.getAddress());
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSelected(device);
                dismissAllowingStateLoss();
            });
        }

        @Override
        public int getItemCount() { return devices.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvMac;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_device_name);
                tvMac  = itemView.findViewById(R.id.tv_device_mac);
            }
        }
    }
}
