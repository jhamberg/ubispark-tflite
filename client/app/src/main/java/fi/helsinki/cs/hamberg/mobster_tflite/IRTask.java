package fi.helsinki.cs.hamberg.mobster_tflite;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Messenger;
import android.os.PowerManager;
import android.text.TextUtils;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Image recognition task using TensorFlow Lite classifier
 *
 * This runnable downloads an image from the master node and attempts to
 * classify it using the provided model. For this, the image first needs
 * to be converted to bytes. When the inference is complete, the result
 * is sent to the master over the provided socket.
 *
 * (C) 2018 Jonatan Hamberg [jonatan.hamberg@cs.helsinki.fi]
 */
public class IRTask extends InferenceTask {
    private static String modelPath = "mobilenet_v1_0.25_224_quant.tflite";
    private static String labelPath = "labels.txt";
    private String resourceURI;

    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int MAX_RESULTS = 3;

    IRTask(String resourceURI, PowerManager powerManager, AssetManager assets, WebSocketClient client, Messenger messenger) {
        super(modelPath, powerManager, assets, client, messenger);
        this.resourceURI = resourceURI;
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


    @Override
    public String runClassifier(Interpreter tf) {
        URL url = null;
        try {
            PriorityQueue<Map.Entry<String, Float>> sortedLabels = new PriorityQueue<>(
                    3,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o2.getValue()).compareTo(o1.getValue());
                        }
                    }
            );
            List<String> labels = loadLabelList(labelPath);

            url = new URL(resourceURI);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            byte[][] labelProbabilities = new byte[1][labels.size()];

            ByteBuffer imgData = convertBitmapToByteBuffer(bitmap);
            tf.run(imgData, labelProbabilities);

            for(int i = 0; i < labels.size(); i++) {
                sortedLabels.add(new AbstractMap.SimpleEntry<>(i + "#" + labels.get(i), (float) labelProbabilities[0][i]));
            }

            final ArrayList<String> results = new ArrayList<>();
            results.add(Constants.SUBMIT_RESULT);
            results.add(resourceURI);
            for(int i = 0; i < Math.min(sortedLabels.size(), MAX_RESULTS); i++) {
                Map.Entry<String, Float> result = sortedLabels.poll();
                results.add(result.getKey() + ":" + result.getValue());
            }

            return TextUtils.join("|", results);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "ERR_CLASSIFIER_INTERRUPTED";
    }
}
