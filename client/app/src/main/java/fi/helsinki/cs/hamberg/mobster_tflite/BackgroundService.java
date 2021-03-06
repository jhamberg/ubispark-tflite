package fi.helsinki.cs.hamberg.mobster_tflite;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
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
 * Background service to manage incoming tasks
 *
 * Establish a WebSocket (rfc 6455) connection to the master node, after which
 * start waiting for tasks. Upon receiving a task, create an image recognition
 * task and add it to the thread pool queue. Tasks are executed concurrently
 * according to the number of available cores on the device.
 *
 * (C) 2018 Jonatan Hamberg [jonatan.hamberg@cs.helsinki.fi]
 */
public class BackgroundService extends Service implements WebSocketClient.Listener {
    private static final String TAG = BackgroundService.class.getSimpleName();
    private static final String CHANNEL = "mobster";
    private final Binder binder = new Binder();
    private String masterURL;
    private Messenger messenger;
    private WebSocketClient client;
    private ThreadPoolExecutor executor;
    private boolean shutdown;

    public class Binder extends android.os.Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shutdown = false;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            masterURL = extras.getString(Constants.MASTER_ENDPOINT);
        } else {
            Log.e(TAG, "You need to provide an URL for the master server!");
            stopSelf();
        }

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
                URI uri = URI.create("ws://" + masterURL);

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
                Constants.NUM_THREADS,
                Constants.NUM_THREADS,
                5L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        return START_STICKY;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public void onCreate() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            WakeLock serviceLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mobster:service");
            serviceLock.acquire();
            if(client != null && !client.isConnected()) {
                client.connect();
            }
            if (serviceLock.isHeld()) {
                serviceLock.release();
            }
        }
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

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            messenger = (Messenger) extras.get("MESSENGER");
        }
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        messenger = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "Connected to master!");
        setForegroundNotification("ONLINE | Waiting...");
        updateTaskBufferSize(Constants.NUM_TASKS);
    }

    private void updateTaskBufferSize(int taskBufferSize) {
        client.send(Constants.UPDATE_BUFFER + "|" + taskBufferSize);
    }

    public void shutdown() {
        Log.d(TAG, "SHUTDOWN");
        shutdown = true;
        if (executor != null) {
            executor.shutdownNow();
        }
        if (client != null) {
            client.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        shutdown();
        super.onDestroy();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public void onMessage(String imageUrl) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if(powerManager != null) {
            // Create a task and submit to executor
            if(!TextUtils.isEmpty(imageUrl)) {
                //Log.d(TAG, "Received task " + imageUrl);
                String url = "http://" + masterURL + "/" + imageUrl;
                executor.execute(new ImageRecognitionTask(url, powerManager, getAssets(), client, messenger));
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
        if (!shutdown) {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            serviceIntent.putExtra(Constants.MASTER_ENDPOINT, masterURL);
            startService(serviceIntent);
        }
    }
}
