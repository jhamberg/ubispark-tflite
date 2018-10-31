package fi.helsinki.cs.hamberg.mobster_tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by Jonatan Hamberg on 29.10.2018
 */
public class RequestRunnable implements Runnable {
    private static final String TAG = RequestRunnable.class.getSimpleName();
    private String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
    private String LABELS_PATH = "labels.txt";
    private int taskId;
    private String urlString;
    private WakeLock wakeLock;
    private WebSocketClient client;

    private AssetManager assets;
    private List<String> labels;
    private byte[][] labelProbs = null;

    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int MAX_RESULTS = 3;

    PriorityQueue<Map.Entry<String, Float>> sortedLabels = new PriorityQueue<>(
            3,
            new Comparator<Map.Entry<String, Float>>() {
                @Override
                public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            }
    );

    RequestRunnable(String url, WakeLock wakeLock, AssetManager assets, WebSocketClient client) {
        this.urlString = url;
        this.wakeLock = wakeLock;
        this.assets = assets;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            Interpreter tf = new Interpreter(loadModelFile());
            tf.setUseNNAPI(true);
            labels = loadLabelList();

            URL url = new URL(urlString);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, false);
            Log.d(TAG, "Dimensions " + bitmap.getWidth() + ", " + bitmap.getHeight());

            labelProbs = new byte[1][labels.size()];

            ByteBuffer imgData = convertBitmapToByteBuffer(bitmap);
            tf.run(imgData, labelProbs);

            for(int i = 0; i< labels.size(); i++) {
                sortedLabels.add(new AbstractMap.SimpleEntry<>(labels.get(i), (float) labelProbs[0][i]));
            }

            final ArrayList<String> results = new ArrayList<>();
            results.add(urlString);
            for(int i = 0; i < Math.min(sortedLabels.size(), MAX_RESULTS); i++) {
                Map.Entry<String, Float> result = sortedLabels.poll();
                results.add(result.getKey() + ":" + result.getValue());
            }
            String result = TextUtils.join("|", results);
            Log.d(TAG, "Results: " + result);
            if(client.isConnected()) {
                client.send(result);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wakeLock.release();
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * bitmap.getWidth() * bitmap.getHeight() * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        int[] intValues = new int[bitmap.getWidth() * bitmap.getHeight()];

        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < bitmap.getWidth(); ++i) {
            for (int j = 0; j < bitmap.getHeight(); ++j) {
                final int val = intValues[pixel++];
                imgData.put((byte) ((val >> 16) & 0xFF));
                imgData.put((byte) ((val >> 8) & 0xFF));
                imgData.put((byte) (val & 0xFF));
            }
        }
        return imgData;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(LABELS_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
}
