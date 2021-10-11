package com.github.ntsee.bleclient;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.github.ntsee.bleclient.ble.BLEFastTimer;
import com.github.ntsee.bleclient.ble.BLETimer;
import com.github.ntsee.bleclient.wifi.WifiTimer;

public class PowerService extends LifecycleService  {

    public static final DataTimer.Type POWER_CLIENT_TYPE = DataTimer.Type.BLE_PERIODIC;
    public static final int POWER_PACKET_SIZE = 500;
    public static final boolean BLE_USE_ACKNOWLEDGEMENTS = true;
    public static final String BLE_TARGET_DEVICE = "C4:5D:83:3C:0B:FF";
    public static final String WIFI_HOST = "192.168.0.12";
    public static final int WIFI_PORT = 8078;


    private static final String CHANNEL_ID = PowerService.class.getCanonicalName();
    private TimedDataClient client;
    int id = 0;

    public PowerService() {
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (this.client != null) {
            Log.d(PowerService.class.getSimpleName(), "Closing old connection");
            this.client.close();
            this.client = null;
        }

        this.createNotificationChannel();
        Log.d(PowerService.class.getSimpleName(), "Creating new connection");
        DataTimer timer;
        switch (POWER_CLIENT_TYPE) {
            case BLE_FAST: timer = new BLEFastTimer(this); break;
            case BLE_PERIODIC: timer = new BLETimer(this); break;
            case WIFI_PERIODIC: timer = new WifiTimer(); break;
            default: throw new IllegalArgumentException("Invalid POWER_CLIENT_TYPE specified");
        }

        this.client = new TimedDataClient(this, timer, (message) -> {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Power")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(this.id++, builder.build());
        });

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}