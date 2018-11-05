package fi.helsinki.cs.hamberg.mobster_tflite;

/**
 * Global constants
 *
 * Editing these allows you to fine-tune and potentially optimize the performance
 * of the client. Especially ENDPOINT_MASTER should be of interest, it determines
 * the URL and port at which the client expects the master to be live.
 *
 * (C) 2018 Jonatan Hamberg [jonatan.hamberg@cs.helsinki.fi]
 */
public class Constants {
    private final static int NUM_CORES = Runtime.getRuntime().availableProcessors();

    public final static String ENDPOINT_MASTER = "localhost:8080";
    public final static String UPDATE_BUFFER = "UPDATE_BUFFER";
    public final static String SUBMIT_RESULT = "SUBMIT_RESULT";
    public final static int NUM_TASKS = NUM_CORES * 12;
    public final static int NUM_THREADS = NUM_CORES * 2;
}
