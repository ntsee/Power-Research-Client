 package com.github.ntsee.bleclient.ble;

import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.github.ntsee.bleclient.BatteryBroadcastReceiver;
import com.github.ntsee.bleclient.MainActivity;

import java.util.*;

public class BLEClient {

    private static final String TAG = "BLEClient";
    private static final String TARGET_DEVICE = "C4:5D:83:3C:0B:FF";
    //private static final String TARGET_DEVICE = "48:59:A4:68:50:4E";

    private static final UUID SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    private static final UUID MESSAGE_UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b");

    private final MainActivity context;
    private final BroadcastReceiver receiver;
    private int counter;
    private final Object lock = new Object();
    private final int size;
    private final boolean acknowledgements;

    private boolean connected;
    private boolean running;
    private boolean busy;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic characteristic;


    public BLEClient(MainActivity context, int size, boolean acknowledgements) {
        this.size = size;
        this.context = context;
        this.acknowledgements = acknowledgements;
        this.context.registerReceiver(this.receiver = new BatteryBroadcastReceiver(this::onBatteryChanged), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        Log.d(TAG, "Devices: " + manager.getAdapter().getBondedDevices().toString());
        for (BluetoothDevice device : manager.getAdapter().getBondedDevices()) {
            if (device.getAddress().equals(TARGET_DEVICE)) {
                this.context.text.setText("Connecting to " + TARGET_DEVICE + "\n");
                this.gatt = device.connectGatt(context, false, new BLEClientBluetoothGattCallback());
                break;
            }
        }
    }

    private boolean isRunning() {
        synchronized (this.lock) {
            return this.running;
        }
    }

    private void setRunning(boolean running) {
        synchronized (this.lock) {
            this.running = running;
        }
    }

    private boolean isConnected() {
        synchronized (this.lock) {
            return this.connected;
        }
    }

    private void setConnected(boolean connected) {
        synchronized (this.lock) {
            this.connected = connected;
        }
    }

    private boolean isBusy() {
        synchronized (this.lock) {
            return this.busy;
        }
    }

    private void setBusy(boolean busy) {
        synchronized (this.lock) {
            this.busy = busy;
        }
    }

    private void setMessageCharacteristic(BluetoothGattCharacteristic characteristic) {
        synchronized (this.lock) {
            this.characteristic = characteristic;
        }
    }

    private void resetCounter() {
        synchronized (this.lock) {
            this.counter = 0;
        }
    }

    private void increaseCounter() {
        synchronized (this.lock) {
            this.counter++;
        }
    }

    private int getCounter() {
        synchronized (this.lock) {
            return this.counter;
        }
    }

    private void send() {
        this.setBusy(true);
        this.increaseCounter();
        synchronized (this.lock) {
            this.characteristic.setValue(new byte[this.size]);
            this.characteristic.setWriteType(acknowledgements ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            this.gatt.writeCharacteristic(this.characteristic);
        }
    }

    public void close() {
        this.context.unregisterReceiver(this.receiver);
        this.setRunning(false);
    }

    private void onBatteryChanged(float previous, float current) {
        this.context.text.append("Battery at" + current + " connected=" + isConnected() + " running=" + isRunning() + "\n");
        if (this.isConnected()) {
            if (!this.isRunning()) {
                this.context.text.append("Starting BLE Thread\n");
                this.setRunning(true);
                new Thread(() -> {
                    while (this.isRunning()) {
                        if (!this.isBusy()) {
                            this.setBusy(true);
                            this.send();
                        }
                    }
                    this.gatt.close();
                }).start();
            } else {
                this.context.text.append(String.format( "%d messages sent.\n", this.getCounter()));
                this.resetCounter();
            }
        }
    }

    private class BLEClientBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean success = status == BluetoothGatt.GATT_SUCCESS;
            boolean connected = newState == BluetoothProfile.STATE_CONNECTED;
            Log.d(TAG, "onConnectionStateChange " + success + " " + connected);
            if (success && connected) {
                gatt.discoverServices();
            } else {
                setMessageCharacteristic(null);
                setConnected(false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            boolean success = status == BluetoothGatt.GATT_SUCCESS;
            Log.d(TAG, "onServicesDiscovered " + success);
            if (success) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "Service Found: " + service.getUuid());
                }

                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                setMessageCharacteristic(service.getCharacteristic(MESSAGE_UUID));
                setConnected(true);

            } else {
                setMessageCharacteristic(null);
                setConnected(false);
                setRunning(false);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            setBusy(false);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged " + mtu);
        }
    }
}
