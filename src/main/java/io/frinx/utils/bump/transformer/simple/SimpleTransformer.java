package io.frinx.utils.bump.transformer.simple;

import static java.lang.String.format;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import io.frinx.utils.bump.Bump;
import io.frinx.utils.bump.transformer.FileTransformer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface SimpleTransformer {

    String fixLine(String line, File file, int lineNumber);

    boolean acceptFile(File file);

    class PomFeaturesAcceptor {
        static boolean acceptFile(File file) {
            return "features.xml".equals(file.getName()) || "pom.xml".equals(file.getName());
        }
    }

    class PatchAcceptor {
        static boolean acceptFile(File file) {
            return file.getName().endsWith(".patch");
        }
    }



    default FileTransformer toFileTransformer() {
        SimpleTransformer simpleTransformer = this;
        return new FileTransformer() {
            @Override
            public TransformFileResult transformFile(File file) throws IOException {
                if (simpleTransformer.acceptFile(file)) {
                    String content = Files.toString(file, Charsets.UTF_8);
                    List<String> inputLines = Lists.newArrayList(Splitter.on('\n').split(content));
                    List<String> outputLines = fixLines(inputLines, file);
                    if (inputLines.equals(outputLines) == false) {
                        System.out.println(file.getAbsolutePath());
                        // replace the file
                        Files.write(Joiner.on("\n").join(outputLines), file, Charsets.UTF_8);
                        return TransformFileResult.CHANGED;
                    }
                    return TransformFileResult.NOT_CHANGED;
                }
                return TransformFileResult.NOT_MATCHED;
            }

            private List<String> fixLines(List<String> inputLines, File file) {
                List<String> newLines = new ArrayList<>();
                int lineNumber = 0;
                for (String inputLine : inputLines) {
                    lineNumber++;
                    String outputLine;
                    try {
                        outputLine = simpleTransformer.fixLine(inputLine, file, lineNumber);
                    } catch (Exception e) {
                        if (Bump.ignoreErrors == false) {
                            throw new IllegalStateException("Cannot fix " + file.getAbsolutePath() + ":" + lineNumber, e);
                        } else {
                            outputLine = inputLine;
                            System.err.println("Ignoring error " + e.getMessage() + " in " + file.getAbsolutePath() + ":" + lineNumber);
                        }
                    }
                    if (inputLine.equals(outputLine) == false) {
                        System.err.println(format("different!'%s'--'%s'", inputLine, outputLine));
                    }
                    newLines.add(outputLine);
                }
                return newLines;
            }
        };
    }
}
