package io.frinx.utils.bump.transformer.util;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

public class ProcessUtil {

    public static ProcessOutput runProcess(File workingDirectory, List<String> commands)
            throws IOException, InterruptedException {

        return runProcess(workingDirectory, commands, Optional.empty());
    }

    public static ProcessOutput runProcess(File workingDirectory, List<String> commands,
                                           Optional<String> input)
            throws IOException, InterruptedException {

        System.out.println("Running " + commands);
        Process process = new ProcessBuilder(commands).directory(workingDirectory).start();

        if (input.isPresent()) {
            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(input.get());
            IOUtils.closeQuietly(writer);
        }
        int status = process.waitFor();

        int readInt;
        StringBuilder outBuilder = new StringBuilder();
        InputStream inIS = process.getInputStream();
        while ((readInt = inIS.read()) != -1)
            outBuilder.append((char) readInt);
        String out = outBuilder.toString();

        StringBuilder errBuilder = new StringBuilder();
        InputStream errIS = process.getInputStream();
        while ((readInt = errIS.read()) != -1)
            errBuilder.append((char) readInt);
        String err = errBuilder.toString();
        checkState(status == 0, "Status is " + status + ", err:'" + err + "', out:'" + out + "'");
        return new ProcessOutput(out, err);
    }

    public static class ProcessOutput {
        private final String stdOut, stdErr;

        public ProcessOutput(String stdOut, String stdErr) {
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }

        public String getStdOut() {
            return stdOut;
        }

        public String getStdErr() {
            return stdErr;
        }
    }
}
