package com.github.ntsee.bleclient.wifi;

import androidx.lifecycle.MutableLiveData;
import com.github.ntsee.bleclient.DataTimer;
import com.github.ntsee.bleclient.PowerService;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class WifiTimer extends Timer implements DataTimer {

    private Socket socket;
    private final byte[] buffer;
    private final MutableLiveData<Integer> sent;
    private int messages;

    public WifiTimer() {
        this.buffer = new byte[PowerService.POWER_PACKET_SIZE];
        this.sent = new MutableLiveData<>(0);
        this.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    socket = new Socket(PowerService.WIFI_HOST, PowerService.WIFI_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }

    public MutableLiveData<Integer> getSent() {
        return this.sent;
    }

    public void start() {
        this.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.getOutputStream().write(buffer);
                    synchronized (WifiTimer.this) {
                        sent.postValue(++messages);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000L);
    }

    public synchronized void reset() {
        this.messages = 0;
    }

    public void stop() {
        this.cancel();
    }

    public void close() {
        this.cancel();
        this.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.getInputStream().close();
                    socket.getOutputStream().close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }
}
