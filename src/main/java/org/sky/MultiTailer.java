package org.sky;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by thor on 15/3/16.
 */
public enum MultiTailer {
    MULTI_TAILER; // Singleton class

    /*
        Using LinkedBlockingQueue for managing logs buffer between
        producers and consumer. Since it is multi thread implementation
        we are leaving it to OS for fairness. Draining of this queue
        is done as one shot in output thread.
        This class maintains lifecycle of input threads and output threads
        and handles cleanup when the program is killed.
     */

    private LinkedBlockingQueue<LogRecord> logRecords =
            new LinkedBlockingQueue<>(MultiTailerConstants.MAX_BUFFER_SIZE);
    private LinkedList<Tailer> tailers = new LinkedList<>();
    private long maxWaitTime = MultiTailerConstants.MAX_TIMEWAIT;
    private Thread outputThread = null;
    public AtomicInteger count = new AtomicInteger(0);

    public void launchOutputThread() {
        if (outputThread != null)
            return;
        LogRecordOutput.instance().init(logRecords, maxWaitTime);
        outputThread = new Thread(LogRecordOutput.instance());
        outputThread.start();
    }

    public void processLine(String line, String filepath) {
        try {
            logRecords.put(new ProcessRecordHelper().processLine(line, filepath));
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while processing line: "
                    + line + " FILE: " + filepath );
        }
    }

    public void setMaxWaitTime(long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public void handleExit() {
        tailers.forEach(tailer -> tailer.stop());
        LogRecordOutput.instance().stop();
    }

    /*
     *   @file = file to tail.
     *   @fromEnd = if tailing should be done from start or end of file
     */
    public void createTailer(File file, boolean fromEnd) {
        Tailer tailer =  Tailer.create(file, new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                try {
                    MULTI_TAILER.processLine(line, file.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void handle(Exception ex) {
                ex.printStackTrace(); // debug - never hit
            }

            @Override
            public void fileRotated() {
                // do nothing
            }
        }, 100, fromEnd);

        tailers.add(tailer);
    }

    public static void main(String[] args) throws IOException {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        options.addOption("D", true, "path to logs directory.");
        options.addOption("T", true, "maxwait value is a value " +
                "in milliseconds to delay processing events.");
        options.addOption("B", false,
                "if tailer should start from beginning of the files.");

        try {
            CommandLine commandLine = parser.parse(options, args);
            String logsPath = commandLine.getOptionValue("D");
            if( logsPath == null) {
                throw new ParseException("Mandatory arguments: -D path/to/logs/dir");
            }

            boolean fromEnd = !commandLine.hasOption("B");
            long delay = (commandLine.hasOption("T")
                    ? ((Number)commandLine.getParsedOptionValue("T")).longValue()
                    : MultiTailerConstants.MAX_TIMEWAIT);

            MULTI_TAILER.setMaxWaitTime(delay);

            File dir = FileUtils.getFile(logsPath);

            if (!dir.isDirectory())
                throw new ParseException("Argument '-D' must be directory");

            // Add shutdownhook to stop tailers and print buffer data
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    MULTI_TAILER.handleExit();
                }
            });

            final String[] extensions = new String[] {"log"};
            List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, false);

            for (File file : files) {
                MULTI_TAILER.createTailer(file, fromEnd);
            }

            MULTI_TAILER.launchOutputThread();

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            return;
        }
    }
}
