package fi.helsinki.cs.hamberg.mobster_tflite;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jonatan Hamberg on 29.10.2018
 */
public class BackgroundService extends Service implements WebSocketClient.Listener {
    private static final String TAG = BackgroundService.class.getSimpleName();
    private static final String CHANNEL = "mobster";
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private WebSocketClient client;
    private ThreadPoolExecutor executor;
    private URI uri = URI.create("ws://localhost:8080");

    @SuppressLint("WakelockTimeout")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting service...");
        setForegroundNotification("OFFLINE | Initializing...");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, CHANNEL, NotificationManager.IMPORTANCE_NONE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            WakeLock serviceLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mobster:service");
            serviceLock.acquire();
            if (client == null) {
                WakeLock networkLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mobster:networking");
                client = new WebSocketClient(uri, this, null, networkLock);
                setForegroundNotification("OFFLINE | Connecting...");
                if (!client.isConnected()) {
                    client.connect();
                }
            }
            if (serviceLock.isHeld()) {
                serviceLock.release();
            }
        }
        executor = new ThreadPoolExecutor(
                NUM_CORES * 2,
                NUM_CORES * 2,
                5L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public void setForegroundNotification(String text) {
        Intent notificationIntent = new Intent(this, ControlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("mobster-tflite")
                .setContentText(text)
                .setContentIntent(pendingIntent).build();

        startForeground(18351860, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "Connected to master!");
        setForegroundNotification("ONLINE | Waiting...");
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public void onMessage(String imageUrl) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if(powerManager != null) {
            // Create a task and submit to executor
            if(!TextUtils.isEmpty(imageUrl)) {
                //Log.d(TAG, "Received task " + imageUrl);
                executor.execute(new ImageRecognitionTask(imageUrl, powerManager, getAssets(), client));
                setForegroundNotification("ONLINE | Processing task");
            }
        }
    }

    @Override
    public void onMessage(byte[] data) {
        // Not implemented
    }

    @Override
    public void onDisconnect(int code, String reason) {
        Log.d(TAG, "Disconnected!");
        setForegroundNotification("OFFLINE | Reconnecting");
        reconnect();
    }

    @Override
    public void onError(Exception error) {
        Log.e(TAG, "Error! " + error.toString());
        setForegroundNotification("ERROR | Reconnecting");
        reconnect();
    }


    private void reconnect() {
        startService(new Intent(this, BackgroundService.class));
    }
}
