import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonatan Hamberg on 23.4.2019
 * Contact: jonatan.hamberg@cs.helsinki.fi
 * University of Helsinki
 */

@SuppressWarnings("SameParameterValue")
class FileUtils {
    private static final int CHANNELS = 3;
    private static final int BUFFER_SIZE = 16384;

    // Keep a reference to avoid reallocation cost
    @SuppressWarnings("FieldCanBeLocal")
    private static FloatBuffer imgData;

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

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf8"));
        String line;
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

    static BufferedImage loadImage(String path) throws IOException {
        return ImageIO.read(FileUtils.class.getResourceAsStream(path));
    }
    
    static FloatBuffer imageToFloatBuffer(BufferedImage image) {
        imgData = FloatBuffer.allocate(image.getWidth() * image.getHeight() * CHANNELS);

        // Reference is reused, need to overwrite
        imgData.rewind();

        int index = 0;
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                final int val = image.getRGB(column, row);
                imgData.put(index++, (val >> 16) & 0xFF);
                imgData.put(index++, (val >> 8) & 0xFF);
                imgData.put(index++, val & 0xFF);
            }
        }

        return imgData;
    }
}
