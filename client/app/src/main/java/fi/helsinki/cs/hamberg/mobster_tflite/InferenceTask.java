package fi.helsinki.cs.hamberg.mobster_tflite;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonatan Hamberg on 11/15/18.
 */
public abstract class InferenceTask implements Runnable {
    private static final String TAG = InferenceTask.class.getSimpleName();
    private String modelPath;
    private String labelPath;
    private PowerManager.WakeLock wakeLock;
    private AssetManager assets;
    private WebSocketClient client;
    private Messenger messenger;

    InferenceTask(String url, String modelPath, String labelPath, PowerManager powerManager,
                              AssetManager assets, WebSocketClient client, Messenger messenger) {
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mobster:service");
        this.assets = assets;
        this.client = client;
        this.messenger = messenger;
        this.modelPath = modelPath;
        this.labelPath = labelPath;
    }


    public abstract String runClassifier(Interpreter tf, List<String> labels);

    @SuppressLint("WakelockTimeout")
    @Override
    public void run() {
        wakeLock.acquire();
        try {
            Interpreter tf = new Interpreter(loadModelFile());
            tf.setUseNNAPI(true); // Use GPU whenever possible
            List<String> labels = loadLabelList();
            String result = runClassifier(tf, labels);
            if(client.isConnected()) {
                client.send(result);
            }
            // sendToActivity();
        } catch (Exception e) {
            Log.d(TAG, "Error running inference " + e);
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void sendToActivity(Bitmap image, String result) {
        if(messenger != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.SHOW_IMAGE, image);
            bundle.putString(Constants.SHOW_RESULT, result);
            Message message = Message.obtain();
            message.setData(bundle);
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
}
