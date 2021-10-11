package com.github.ntsee.bleclient.ble;

import android.bluetooth.*;
import android.content.Context;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.github.ntsee.bleclient.DataTimer;
import com.github.ntsee.bleclient.PowerService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BLETimer extends Timer implements DataTimer {

    private static final UUID SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    private static final UUID MESSAGE_UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b");

    private final Context context;
    private final MutableLiveData<Integer> sent;
    private int messages;
    private boolean initialized;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic characteristic;


    public BLETimer(Context context) {
        this.context = context;
        this.sent = new MutableLiveData<>(0);
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        for (BluetoothDevice device : manager.getAdapter().getBondedDevices()) {
            if (device.getAddress().equals(PowerService.BLE_TARGET_DEVICE)) {
                Log.d("BLEClient", "Found target device, connecting...");
                this.gatt = device.connectGatt(context, false, new BLETimerBluetoothGattCallback());
                break;
            }
        }
    }

    public MutableLiveData<Integer> getSent() {
        return this.sent;
    }

    public void start() {
        this.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (BLETimer.this) {
                    characteristic.setValue(new byte[PowerService.POWER_PACKET_SIZE]);
                    characteristic.setWriteType(PowerService.BLE_USE_ACKNOWLEDGEMENTS ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    gatt.writeCharacteristic(characteristic);
                    sent.postValue(++messages);
                }
            }
        }, 0, 1000);
    }

    public synchronized void reset() {
        this.messages = 0;
    }

    public void close() {
        this.cancel();
        synchronized (this) {
            if (this.initialized) {
                this.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (gatt != null) {
                            gatt.close();
                        }
                    }
                }, 0);
            }
        }

    }

    public class BLETimerBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean success = status == BluetoothGatt.GATT_SUCCESS;
            boolean connected = newState == BluetoothProfile.STATE_CONNECTED;
            if (success && connected) {
                gatt.discoverServices();
            } else {
                synchronized (BLETimer.this) {
                    initialized= false;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            boolean success = status == BluetoothGatt.GATT_SUCCESS;
            if (success) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().equals(SERVICE_UUID)) {
                        Log.d("BLEClient", "Service UUID: " + service.getUuid().toString());
                        synchronized (BLETimer.this) {
                            Log.d("BLEClient", "Found and initializing.");
                            characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(MESSAGE_UUID);
                            initialized = true;
                        }
                        start();
                    }
                }

            } else {
                synchronized (BLETimer.this) {
                    initialized = false;
                }
            }
        }
    }
}
