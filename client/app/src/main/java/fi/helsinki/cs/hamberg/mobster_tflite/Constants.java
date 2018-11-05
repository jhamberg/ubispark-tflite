package fi.helsinki.cs.hamberg.mobster_tflite;

/**
 * Created by Jonatan Hamberg on 5.11.2018
 */
public class Constants {
    private final static int NUM_CORES = Runtime.getRuntime().availableProcessors();

    public final static String ENDPOINT_MASTER = "localhost:8080";
    public final static String UPDATE_BUFFER = "UPDATE_BUFFER";
    public final static String SUBMIT_RESULT = "SUBMIT_RESULT";
    public final static int NUM_TASKS = NUM_CORES * 12;
    public final static int NUM_THREADS = NUM_CORES * 2;
}
