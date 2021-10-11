package com.github.ntsee.bleclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryBroadcastReceiver extends BroadcastReceiver {

    private final Listener listener;
    private boolean initialized = false;
    private float battery;

    public BatteryBroadcastReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        float current = this.getBattery(intent);
        if (!this.initialized) {
            this.initialized = true;
            this.battery = current;
            Log.d("BLEClient", "Initialized first battery event.");
        } else if ((this.battery * 100 - current * 100)  >= 1f) {
            this.listener.onBatteryChanged(this.battery * 100, current * 100);
            this.battery = current;
        }
    }

    private float getBattery(Intent intent) {
        if (intent == null) {
            return 0f;
        }

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        return level / (float)scale;
    }

    public interface Listener {

        void onBatteryChanged(float previous, float current);
    }
}
