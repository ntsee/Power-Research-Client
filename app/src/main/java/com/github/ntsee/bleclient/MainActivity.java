package com.github.ntsee.bleclient;

import android.content.Intent;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.github.ntsee.bleclient.ble.BLETimer;
import com.github.ntsee.bleclient.wifi.WifiTimer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public TextView counter;
    public TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.counter = findViewById(R.id.counter);
        this.text = findViewById(R.id.text);

        Intent intent = new Intent(this, PowerService.class);
        startService(intent);
    }
}