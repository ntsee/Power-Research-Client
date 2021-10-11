package com.github.ntsee.bleclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import java.util.Locale;
import java.util.function.Consumer;

public class TimedDataClient {

    private static final String TAG = "TimedWifiClient";

    private final Context context;
    private final DataTimer timer;
    private final BroadcastReceiver receiver;
    private boolean started;
    private int sent;

    private PowerManager.WakeLock lock;
    private WifiManager.WifiLock wifilock;
    private Consumer<String> callback;


    public TimedDataClient(PowerService context, DataTimer timer, Consumer<String> callback) {
        this.callback = callback;
        WifiManager wMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifilock = wMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyWifiLock");
        wifilock.acquire();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
        this.lock.acquire();

        this.context = context;
        this.timer = timer;
        this.timer.getSent().observe(context, sent -> {
            this.sent = sent;
        });
        this.receiver = new BatteryBroadcastReceiver(this::onBatteryChanged);
        this.context.registerReceiver(this.receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        String message = "Connection initialized.\n";
        //this.context.text.append(message);
        this.started = true;
        this.timer.start();
    }

    private void onBatteryChanged(float previous, float battery) {
        if (!this.started) {
            this.timer.start();
            this.started = true;

            String message = "Data transfer started.\n";
            //this.context.text.append(message);
            return;
        }

        float delta = previous - battery;
        String message = String.format(Locale.ENGLISH, "Sent: %d\n", this.sent);
        //this.context.text.append(message);
        this.timer.reset();
        this.callback.accept(message);
    }

    public void close() {
        Log.d(TAG, "Closing resources.");
        this.lock.release();
        wifilock.release();
        this.context.unregisterReceiver(this.receiver);
        if (this.timer != null) {
            this.timer.close();
        }
    }
}
