import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jonatan Hamberg on 23.4.2019
 * Contact: jonatan.hamberg@cs.helsinki.fi
 * University of Helsinki
 */

public class ImageRecognitionTask {
    private static final int MAX_RESULTS = 3;
    private static final String MODEL = "models/mobilenet_v1_0.5_224_quant_frozen.pb";
    private static final String LABELS = "models/labels.txt";
    private static final Graph GRAPH = new Graph();
    private static final PriorityQueue<Map.Entry<String, Float>> RESULT = new PriorityQueue<>(
            3,
            (o1, o2) -> (o2.getValue()).compareTo(o1.getValue())
    );

    private static List<String> labels;

    public static void main(String[] args) throws IOException {
        labels = FileUtils.loadLines(LABELS);
        GRAPH.importGraphDef(FileUtils.loadBytes(MODEL));
        System.out.println(labels);

        run(FileUtils.loadImage("models/ILSVRC2012_val_00031951.JPEG"));
    }

    public static void run(BufferedImage image) {
        Tensor<Float> tensor = Tensor.create(new long[]{1, image.getWidth(), image.getHeight(), 3},
                FileUtils.imageToFloatBuffer(image));
        try (Session session = new Session(GRAPH)){
            // Operation names are specified in the mobilenet_v1_0.5_224_quant_info.txt file
            Tensor<Float> result = session.runner()
                    .feed("input", tensor)
                    .fetch("MobilenetV1/Predictions/Reshape_1")
                    .run()
                    .get(0)
                    .expect(Float.class);
            float[] predictions = result.copyTo(new float[1][labels.size()])[0];
            for (int i = 1; i < labels.size(); i++) {
                RESULT.add(new AbstractMap.SimpleEntry<>(i + "#" + labels.get(i-1),
                        predictions[i]));
            }

            for (int i = 0; i < Math.min(predictions.length, MAX_RESULTS); i++) {
                System.out.println(RESULT.poll());
            }
        }
    }
}
