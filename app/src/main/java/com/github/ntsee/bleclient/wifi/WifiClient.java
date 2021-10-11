package com.github.ntsee.bleclient.wifi;

import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.github.ntsee.bleclient.BatteryBroadcastReceiver;
import com.github.ntsee.bleclient.MainActivity;

import java.io.IOException;
import java.net.Socket;

public class WifiClient implements Runnable {

    private static final String TAG = "WifiClient";

    private final MainActivity context;
    private BatteryBroadcastReceiver receiver;
    private final Thread thread;
    private boolean running;
    private boolean sending;
    private int counter;

    public WifiClient(MainActivity context) {
        this.context = context;
        this.receiver = new BatteryBroadcastReceiver(this::onBatteryChanged);
        this.thread = new Thread(this);
        this.context.registerReceiver(this.receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void onBatteryChanged(float previous, float battery) {
        if (!this.isSending()) {
            this.context.text.setText("Sending initiated.\n");
            synchronized (this) {
                this.sending = true;
            }
        } else {
            this.context.text.append("Sent: " + this.getCount() + "\n");
            this.reset();
        }
    }

    public synchronized void start() {
        this.running = true;
        this.thread.start();
    }

    private synchronized boolean isRunning() {
        return this.running;
    }

    private synchronized boolean isSending() {
        return this.sending;
    }

    private synchronized int getCount() {
        return this.counter;
    }

    private synchronized void increment() {
        this.counter++;
    }

    private synchronized void reset() {
        this.counter = 0;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[500];
        while (this.isRunning()) {
            while (this.isRunning() && this.isSending()) {
                try (Socket socket = new Socket("192.168.254.31", 8078)) {
                    socket.getOutputStream().write(buffer);
                    this.increment();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    }

    public synchronized void close() {
        this.context.unregisterReceiver(this.receiver);
        this.running = false;
    }
}
