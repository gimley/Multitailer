package org.sky;

import java.util.Date;

/**
 * Created by thor on 15/3/16.
 */
public final class LogRecord implements Comparable<LogRecord>{
    private final Date timestamp;
    private final String log;
    private boolean isError;

    LogRecord(Date timestamp, String log, boolean isError) {
        this.timestamp = timestamp;
        this.log = log;
        this.isError = isError;
    }

    public String getLog() {
        return log;
    }

    public boolean isError() {
        return isError;
    }

    /*
    *   Required for sorting records in buffer using
    *   Collections.sort() API
    */
    @Override
    public int compareTo(LogRecord o) {
        return this.timestamp.compareTo(o.timestamp);
    }
}
