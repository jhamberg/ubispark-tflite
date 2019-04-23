import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Created by Jonatan Hamberg on 23.4.2019
 * Contact: jonatan.hamberg@cs.helsinki.fi
 * University of Helsinki
 */

public class ImageRecognitionTask {
    private static final int MAX_RESULTS = 1;
    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static final String MODEL = "models/mobilenet_v1_0.5_224_quant_frozen.pb";
    private static final String LABELS = "models/labels.txt";
    private static final Graph GRAPH = new Graph();

    private static PriorityQueue<Map.Entry<String, Float>> result;
    private static List<String> labels;

    public static void main(String[] args) throws IOException {
        GRAPH.importGraphDef(FileUtils.loadBytes(MODEL));

        labels = FileUtils.loadLines(LABELS);
        result = new PriorityQueue<>(labels.size(),
                Comparator.comparing(AbstractMap.Entry::getValue, Comparator.reverseOrder()));

        File folder = new File(args.length > 0
                ? args[0]
                : promptFolder());

        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Target needs to be a folder");
        }

        // Only match JPEG files for now
        Pattern pattern = Pattern.compile("^[^.]*.(jpg|jpeg)$", Pattern.CASE_INSENSITIVE);
        File[] files = folder.listFiles((dir, name) -> pattern.matcher(name).find());
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("Target folder contains no JPEG files");
        }

        // Submit tasks concurrently using threads
        ExecutorService service = Executors.newFixedThreadPool(THREADS);
        for (File file : files) {
            service.submit(() -> {
                try {
                    run(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static String promptFolder() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Folder name:");
        return scanner.nextLine();
    }

    private static void run(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        Tensor<Float> tensor = Tensor.create(new long[]{1, image.getWidth(), image.getHeight(), 3},
                FileUtils.imageToFloatBuffer(image));
        try (Session session = new Session(GRAPH)){
            // Operation names are specified in the mobilenet_v1_0.5_224_quant_info.txt file
            Tensor<Float> output = session.runner()
                    .feed("input", tensor)
                    .fetch("MobilenetV1/Predictions/Reshape_1")
                    .run()
                    .get(0)
                    .expect(Float.class);

            // Copy prediction accuracy for each label
            float[] predictions = output.copyTo(new float[1][labels.size()])[0];
            for (int i = 1; i < labels.size(); i++) {
                result.add(new SimpleEntry<>(i + "#" + labels.get(i-1), predictions[i]));
            }

            // Poll and print the best results
            for (int i = 0; i < Math.min(predictions.length, MAX_RESULTS); i++) {
                System.out.println(Thread.currentThread().getName() + ": " + file.getName() +
                        " -> " + result.poll());
            }
        }
    }
}
