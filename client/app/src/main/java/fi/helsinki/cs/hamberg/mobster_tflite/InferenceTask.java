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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Runnable class for deep learning inference
 *
 * This runnable acts as the base for other inference tasks, such as image
 * recognition and audio captioning. It hides details about networking and
 * message exchange between the service and activity. Please extend this
 * class as it is not meant to be initialized directly.
 *
 * (C) 2018 Jonatan Hamberg [jonatan.hamberg@cs.helsinki.fi]
 */
public abstract class InferenceTask implements Runnable {
    private static final String TAG = InferenceTask.class.getSimpleName();
    private String modelPath;
    private PowerManager.WakeLock wakeLock;
    private AssetManager assets;
    private WebSocketClient client;
    private Messenger messenger;

    InferenceTask(String modelPath, PowerManager powerManager,
                              AssetManager assets, WebSocketClient client, Messenger messenger) {
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mobster:service");
        this.assets = assets;
        this.client = client;
        this.messenger = messenger;
        this.modelPath = modelPath;

    }

    abstract String runClassifier(Interpreter tf);

    @SuppressLint("WakelockTimeout")
    @Override
    public void run() {
        wakeLock.acquire();
        try {
            Interpreter tf = new Interpreter(loadModelFile());
            tf.setUseNNAPI(true); // Use GPU whenever possible
            String result = runClassifier(tf);

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

    List<String> loadLabelList(String path) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(path)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
}
