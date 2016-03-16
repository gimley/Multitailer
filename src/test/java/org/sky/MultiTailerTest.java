package org.sky;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by thor on 16/3/16.
 */
public class MultiTailerTest {
    private final String inputDir="input";
    private final String outputFile="output.log";
    private final String errorFile="error.log";

    private MultiTailer classUnderTest = MultiTailer.MULTI_TAILER;

    @Before
    public void setUp() throws IOException {
        File dir = FileUtils.getFile(inputDir);

        if (!dir.isDirectory())
            throw new IOException("No input directory");

        // Clear output log file and error file
        FileWriter outputWriter = new FileWriter(outputFile,false);
        outputWriter.write("");
        FileWriter errWriter = new FileWriter(errorFile,false);
        errWriter.write("");
        IOUtils.closeQuietly(outputWriter);
        IOUtils.closeQuietly(errWriter);

        final String[] extensions = new String[] {"txt"};
        List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, false);

        for (File file : files) {
            classUnderTest.createTailer(file, false);
        }

        classUnderTest.launchOutputThread();
    }

    @Test
    public void testMultiTailer() {
        try {
            Thread.sleep(2*MultiTailerConstants.MAX_TIMEWAIT);

            // Check for earliest error record from input files matches
            // first record in error file
            LineIterator lineIterator = FileUtils.lineIterator(new File(errorFile));
            Assert.assertEquals(lineIterator.nextLine(), "# INVALID_LINE: {\"at\":\"Wed Mar 16 19:08:54 PDT 2016\"}");
            LineIterator.closeQuietly(lineIterator);

            // Check for earliest valid record from input files matches
            // first record in output file
            lineIterator = FileUtils.lineIterator(new File(outputFile));
            String result = "";
            for (int i = 0; i < 5; i++) {
                result += lineIterator.nextLine();
            }
            Assert.assertEquals(result, "{  \"note\": \"content note\",  \"at\": \"Wed Mar 16 19:08:54 PDT 2016\"," +
                    "  \"input\": \"/home/thor/IdeaProjects/multitailer/input/file1.txt\"}");
            LineIterator.closeQuietly(lineIterator);

            classUnderTest.handleExit();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
}
