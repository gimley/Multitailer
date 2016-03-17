package org.sky;

import com.google.gson.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by thor on 15/3/16.
 */
public class ProcessRecordHelper {

    /*
     *   Helper function to process and validate each line of log
     *   LogRecord is valid only if JSON has both
     *   "at" and "note" fields in top level.
     *   Valid records will be prettyPrinted in outputThread.
     *   FIXME: Move into MultiTailer class and make it private
     *   keep here for readabilty.
     */
    public LogRecord processLine(String line, String filepath) {
        try {
            JsonElement element = new JsonParser().parse(line);
            if (element != null && element.isJsonObject()
                    && element.getAsJsonObject().has("at")) {
                JsonElement at = element.getAsJsonObject().get("at");
                JsonElement note = element.getAsJsonObject().get("note");
                if (note != null) {
                    element.getAsJsonObject().addProperty("input", filepath);
                    Date date = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
                            .parse(at.getAsString());
                    return new LogRecord(date, element.toString(), false);
                } else {
                    // Missing note field
                    return new LogRecord(new Date(), element.toString(), true);
                }
            }
        } catch (JsonSyntaxException | ParseException e) {
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        // INVALID Log Record
        return new LogRecord(new Date(), line, true);
    }
}
