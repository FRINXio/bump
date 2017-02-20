package io.frinx.utils.bump.transformer.simple;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import io.frinx.utils.bump.Bump;
import io.frinx.utils.bump.transformer.FileTransformer;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionTransformationStrategy;
import io.frinx.utils.bump.transformer.simple.SimpleTransformer.PatchAcceptor;
import io.frinx.utils.bump.transformer.util.VersionFactory.Version.VersionParsingException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Script for bumping versions to +1-SNAPSHOT after release.
 * Customization options: this script can increase either micro version or qualifier version.
 * It is required that version contains 'frinx' qualifier.
 * For distribution project, version currently looks like 1.2.3.frinx, so only bumping micro version makes sense.
 * Mode of operation: input patch file contains lines starting with - and + signs.
 * <pre>
 * {@code
 * -      <version>1.2.0.frinx</version>
 * +      <version>1.1.8.frinx-SNAPSHOT</version>
 * }
 * </pre>
 * Output should be:
 * <pre>
 * {@code
 * -      <version>1.2.0.frinx</version>
 * +      <version>1.2.1.frinx-SNAPSHOT</version>
 * }
 * </pre>
 *
 * Lines starting with + must be changed, but version from - should be parsed and increased.
 */
public class BumpSnapshotPatchTransformerFactory {
    public static BumpSnapshotPatchTransformer create(VersionTransformationStrategy strategy) {
        return new BumpSnapshotPatchTransformer(strategy);
    }

    static class BumpSnapshotPatchTransformer implements FileTransformer {

        private final Iterable<Pattern> patterns;

        VersionTransformationStrategy strategy;

        public BumpSnapshotPatchTransformer(VersionTransformationStrategy strategy) {
            this.strategy = strategy;

            List<Pattern> patterns = new ArrayList<>();
            for (String suffix : asList("", "-SNAPSHOT")) {
                patterns.add(Pattern.compile("^-(.*<.+>)(.+" + strategy.getQualifierSuffix() + suffix + ")(</.+>.*)$", Pattern.DOTALL));
                patterns.add(Pattern.compile("^-(.*[\"'])([^\"']+" + strategy.getQualifierSuffix() + suffix + ")([\"'].*)$", Pattern.DOTALL));
            }
            this.patterns = patterns;
        }

        @Override
        public TransformFileResult transformFile(File file) throws IOException {
            checkArgument(acceptFile(file), "File not accepted - " + file.getAbsolutePath());
            System.out.println("Transforming " + file.getAbsolutePath());
            String content = Files.toString(file, Charsets.UTF_8);
            List<String> inputLines = Lists.newArrayList(Splitter.on('\n').split(content));
            List<String> outputLines = fixLines(inputLines, file);
            if (inputLines.equals(outputLines) == false) {
                // replace the file
                Files.write(Joiner.on("\n").join(outputLines), file, Charsets.UTF_8);
                return TransformFileResult.CHANGED;
            }
            return TransformFileResult.NOT_CHANGED;
        }

        List<String> fixLines(List<String> inputLines, File file) {
            List<String> result = new ArrayList<>();
            int lineNumber = 0;

            for (Iterator<String> it = inputLines.iterator(); it.hasNext(); ) {
                lineNumber = transformLine(file, result, lineNumber, it);
            }
            return result;
        }

        private int transformLine(File file, List<String> result, int lineNumber, Iterator<String> it) {
            String inputLine = it.next();
            lineNumber++;
            return transformLine(file, result, lineNumber, it, inputLine, new ArrayDeque<>());
        }

        private int transformLine(File file, List<String> result, int lineNumber, Iterator<String> it, String inputLine, Deque<String> plusLines) {
            String outputLine;
            try {
                outputLine = fixLine(inputLine, file, lineNumber);
            } catch (Exception e) {
                outputLine = inputLine;
                maybeThrowException(file, lineNumber, e);
            }
            if (outputLine.equals(inputLine)) {
                result.add(outputLine);
            } else {
                checkState(inputLine.startsWith("-"));
                result.add(inputLine); // do not modify lines starting with -
                checkState(outputLine.startsWith("+"));
                // the nextLine might not be what we expect
                if (it.hasNext()) {
                    lineNumber++;
                    String maybeExcludedNextLine = it.next();
                    plusLines.add(outputLine);
                    if (maybeExcludedNextLine.startsWith("-")) {
                        // call recursively
                        lineNumber = transformLine(file, result, lineNumber, it, maybeExcludedNextLine, plusLines);
                        if (it.hasNext()) {
                            lineNumber++;
                            maybeExcludedNextLine = it.next();
                            assertSkippedLineStartsWithPlus(file, lineNumber, maybeExcludedNextLine);
                        } else {
                            maybeThrowException(file, lineNumber, new IllegalStateException("Unexpected end of file"));
                        }
                    } else {
                        assertSkippedLineStartsWithPlus(file, lineNumber, maybeExcludedNextLine);
                    }
                    result.add(plusLines.pop());
                } else {
                    maybeThrowException(file, lineNumber, new IllegalStateException("Unexpected end of file"));
                }
            }
            return lineNumber;
        }

        private void assertSkippedLineStartsWithPlus(File file, int lineNumber, String maybeExcludedNextLine) {
            if (maybeExcludedNextLine.startsWith("+") == false) {
                maybeThrowException(file, lineNumber, new IllegalStateException(
                        "Expected line starting with +, got" + maybeExcludedNextLine));
            }
            // otherwise just skip it - outputLine will be used instead
        }

        private void maybeThrowException(File file, int lineNumber, Exception e) {
            if (Bump.ignoreErrors == false) {
                throw new IllegalStateException("Cannot fix " + file.getAbsolutePath() + ":" + lineNumber, e);
            } else {
                System.err.println("Ignoring error " + e.getMessage() + " in " + file.getAbsolutePath() + ":" + lineNumber);
            }
        }

        private boolean acceptFile(File file) {
            return PatchAcceptor.acceptFile(file);
        }

        private String fixLine(String line, File file, int lineNumber) {
            for (Pattern pattern : patterns) {
                Matcher m = pattern.matcher(line);
                line = transformIfMatches(line, file, lineNumber, m);
            }
            return line;
        }

        private String transformIfMatches(String line, File file, int lineNumber, Matcher m) {
            if (m.matches()) {
                String ver = m.group(2);
                String transformed = transformVersion(file, lineNumber, ver);
                line = "+" + m.group(1) + transformed + m.group(3);
            }
            return line;
        }

        private String transformVersion(File file, int lineNumber, String ver) {
            try {
                return strategy.transform(ver);
            } catch (VersionParsingException e) {
                throw new IllegalArgumentException("Cannot parse " + file.getAbsolutePath() + ":" + lineNumber, e);
            }
        }
    }

}
