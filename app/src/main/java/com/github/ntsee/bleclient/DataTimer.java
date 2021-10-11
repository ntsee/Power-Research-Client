package com.github.ntsee.bleclient;

import androidx.lifecycle.MutableLiveData;

public interface DataTimer {

    MutableLiveData<Integer> getSent();
    void start();
    void reset();
    void close();

    enum Type {
        BLE_PERIODIC,
        BLE_FAST,
        WIFI_PERIODIC,
    }
}
