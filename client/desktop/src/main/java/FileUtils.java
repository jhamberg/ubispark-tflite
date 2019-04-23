import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonatan Hamberg on 23.4.2019
 * Contact: jonatan.hamberg@cs.helsinki.fi
 * University of Helsinki
 */

@SuppressWarnings({"SameParameterValue", "FieldCanBeLocal"})
class FileUtils {
    private static final int CHANNELS = 3;
    private static final int BUFFER_SIZE = 16384;

    static byte[] loadBytes(String path) throws IOException {
        InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(path);
        assert (is != null);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = is.read(buffer, 0, BUFFER_SIZE)) > 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }

    static List<String> loadLines(String path) throws IOException {
        List<String> result = new ArrayList<>();
        InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(path);
        assert (is != null);

        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        br.close();
        return result;
    }

    static FloatBuffer imageToFloatBuffer(BufferedImage image) {
        FloatBuffer imgData = FloatBuffer.allocate(image.getWidth() * image.getHeight() * CHANNELS);
        imgData.rewind();

        int index = 0;
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                final int val = image.getRGB(column, row);
                // Note, this model expects colors in 0-1 range unlike the tflite model
                imgData.put(index++, ((val >> 16) & 0xFF) / 255.0f);
                imgData.put(index++, ((val >> 8) & 0xFF) / 255.0f);
                imgData.put(index++, (val & 0xFF) / 255.0f);
            }
        }

        return imgData;
    }
}
