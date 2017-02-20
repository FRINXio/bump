package io.frinx.utils.bump.transformer.util;

import io.frinx.utils.bump.Bump;
import io.frinx.utils.bump.transformer.util.ProcessUtil.ProcessOutput;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SedUtil {

    public static String sed(String text, String pattern) {
        List<String> command = Arrays.asList("sed", pattern);
        try {
            ProcessOutput result = ProcessUtil.runProcess(null, command, Optional.of(text));
            return result.getStdOut();
        } catch (Exception e) {
            if (Bump.ignoreErrors) {
                System.err.println("Ignoring sed error of input '" + text + "'");
                e.printStackTrace();
                return text;
            }
            throw new RuntimeException("Error running sed with pattern '" + pattern + "' and text '" + text + "'", e);
        }
    }
}
