package fi.helsinki.cs.hamberg.mobster_tflite;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import fi.helsinki.cs.hamberg.mobster_tflite.databinding.ActivityControlBinding;

/**
 * Self-terminating activity for initializing the background service
 *
 * This activity prompts the user to ignore battery optimizations, after
 * which it starts up the background service and closes. In the future,
 * this activity might also handle setting the master URL and showing
 * details of the current task and job.
 *
 * (C) 2018 Jonatan Hamberg [jonatan.hamberg@cs.helsinki.fi]
 */
public class ControlActivity extends AppCompatActivity {
    private BackgroundService serviceBinder;
    private ActivityControlBinding binding;
    private Intent serviceIntent;
    private boolean serviceBound;

    Handler serviceMessageHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message != null) {
                Bundle data = message.getData();
                Bitmap image = data.getParcelable(Constants.SHOW_IMAGE);
                String result = data.getString(Constants.SHOW_RESULT);

                if (binding != null && result != null) {
                    String[] results = result.split("\\|");
                    String text = results[1] + "\n" + results[2];
                    binding.image.setImageBitmap(image);
                    binding.result.setText(text);
                }
            }
            return true;
        }
    });

    Messenger messenger = new Messenger(serviceMessageHandler);

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            serviceBound = true;
            serviceBinder = ((BackgroundService.Binder) binder).getService();
            if (serviceBinder != null) {
                serviceBinder.setMessenger(messenger);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            serviceBound = false;
            if (serviceBinder != null) {
                serviceBinder.setMessenger(null);
                serviceBinder = null;
            }
        }
    };

    @SuppressLint("BatteryLife")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceBound = false;
        serviceIntent = new Intent(ControlActivity.this, BackgroundService.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_control);
        binding.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBackgroundService();
            }
        });
        binding.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopBackgroundService();
            }
        });
    }

    private void startBackgroundService() {
        String masterURL = binding.urlInput.getText().toString();
        if(masterURL.trim().length() == 0) {
            Toast.makeText(this, "Please provide the master address!", Toast.LENGTH_SHORT).show();
            return;
        }
        serviceIntent.putExtra(Constants.MASTER_ENDPOINT, masterURL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private void stopBackgroundService() {
        if(serviceBinder != null) {
            serviceBinder.shutdown();
        }
        if (serviceBound) {
            serviceBound = false;
            unbindService(serviceConnection);
        }
        stopService(serviceIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceBound) {
            unbindService(serviceConnection);
        }
    }

    @SuppressLint("BatteryLife")
    @Override
    protected void onResume() {
        super.onResume();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && powerManager != null
                && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
        bindService(serviceIntent, serviceConnection, 0);
    }
}
