package fi.helsinki.cs.hamberg.mobster_tflite;

/**
 * Global constants
 *
 * Editing these allows you to fine-tune and potentially optimize the performance
 * of the client. Especially the NUM_TASKS and NUM_THREADS should be of interest,
 * as they determine the size of the executor thread pool.
 *
 * (C) 2018 Jonatan Hamberg [jonatan.hamberg@cs.helsinki.fi]
 */
public class Constants {
    private final static int NUM_CORES = Runtime.getRuntime().availableProcessors();

    public final static int NUM_TASKS = NUM_CORES * 12;
    public final static int NUM_THREADS = NUM_CORES * 2;

    // Internal values
    public final static String MASTER_ENDPOINT = "MASTER_ENDPOINT";
    public final static String UPDATE_BUFFER = "UPDATE_BUFFER";
    public final static String SUBMIT_RESULT = "SUBMIT_RESULT";
    public final static String SHOW_IMAGE = "SHOW_IMAGE";
    public final static String SHOW_RESULT = "SHOW_RESULT";
}
