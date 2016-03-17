package org.sky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by thor on 15/3/16.
 */
public enum LogRecordOutput implements Runnable {
    LOG_RECORD_OUTPUT; // Singleton class, only one Output thread (scalability?)

    private LinkedBlockingQueue<LogRecord> logRecords = null;
    private List<LogRecord> outputRecords = new ArrayList<>(MultiTailerConstants.MAX_BUFFER_SIZE);
    private boolean running = true;
    private long maxWaitTime = MultiTailerConstants.MAX_TIMEWAIT;
    private static final String outFileName = "output.log";
    private static final String errorFileName = "error.log";
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // debug
    private AtomicInteger count = new AtomicInteger(0);

    public void init(LinkedBlockingQueue<LogRecord> queue, long maxWaitTime) {
        logRecords = queue;
        this.maxWaitTime=maxWaitTime;
    }

    public static LogRecordOutput instance() { return LOG_RECORD_OUTPUT; }

    @Override
    public void run() {

        while (running) {
            try {
                long outputStart = System.currentTimeMillis();

                FileWriter output = new FileWriter(outFileName, true);
                FileWriter err = new FileWriter(errorFileName, true);

                // Drain all log records from buffer into output buffer
                // so that input threads can put more into blocking queue.
                logRecords.drainTo(outputRecords, MultiTailerConstants.MAX_BUFFER_SIZE);

                Collections.sort(outputRecords);

                // FIXME: Count not adding up to input records
                // when input files are always writing
                if (outputRecords.size() > 0)
                    System.out.println("Parsed Count: " + count.addAndGet(outputRecords.size()));

                for (LogRecord record: outputRecords) {
                    if (record.isError())
                        err.write("# INVALID_LINE: " + record.getLog() + "\n");
                    else {
                        JsonParser jp = new JsonParser();
                        JsonElement je = jp.parse(record.getLog());
                        String prettyJsonString = gson.toJson(je);
                        output.write(prettyJsonString + "\n");
                    }
                }
                IOUtils.closeQuietly(output);
                IOUtils.closeQuietly(err);
                outputRecords.clear();

                long outputDuration = System.currentTimeMillis() - outputStart;

                /*
                *   Wait for records to be parsed and put into queue by
                 *  Tailer threads. Periodically also check if BlockingQueue is full.
                 *  We have initialized queue capacity to be MAX_BUFFER_SIZE.
                */
                while (outputDuration < maxWaitTime/2
                        && logRecords.size() < MultiTailerConstants.MAX_BUFFER_SIZE) {
                     Thread.sleep(100);
                    outputDuration = System.currentTimeMillis() - outputStart;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
