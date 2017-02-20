package io.frinx.utils.bump.transformer.simple;

import static io.frinx.utils.bump.transformer.util.LoggingUtil.fatal;
import static java.util.Arrays.asList;

import io.frinx.utils.bump.Bump;
import io.frinx.utils.bump.transformer.MainRunner;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionTransformationStrategy;
import io.frinx.utils.bump.transformer.util.FileUtil;
import io.frinx.utils.bump.transformer.util.VersionFactory;
import io.frinx.utils.bump.transformer.util.VersionFactory.Version;
import io.frinx.utils.bump.transformer.util.VersionFactory.Version.VersionParsingException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleMainRunner implements MainRunner {

    @Override
    public void run(List<String> args) throws Exception {

        if (args.size() == 0) {
            throw fatal("Not enough arguments, try -h");
        }
        if (args.size() == 1 && "-h".equals(args.get(0))) {
            VersionTransformationStrategyFactory.printHelp();
            return;
        }

        Entry<VersionTransformationStrategy, List<String>> entry = VersionTransformationStrategyFactory.parseArgs(args);
        if (entry.getValue().size() > 0) {
            throw fatal("Too many arguments:" + entry.getValue());
        }

        File currentFolder = new File(".");
        SimpleTransformer simple = new BumpMatchingSuffixSimpleTransformer(entry.getKey());
        FileUtil.transformRecursively(currentFolder, simple.toFileTransformer());
    }

    private static class BumpMatchingSuffixSimpleTransformer implements SimpleTransformer {

        private final Iterable<Pattern> patterns;
        private final VersionTransformationStrategy strategy;

        public BumpMatchingSuffixSimpleTransformer(VersionTransformationStrategy strategy) {
            this.strategy = strategy;
            List<Pattern> patterns = new ArrayList<>();
            for(String suffix: asList("", "-SNAPSHOT")) {
                patterns.add(Pattern.compile("^(.*<.+>)(.+" + strategy.getQualifierSuffix() + suffix + ")(</.+>.*)$", Pattern.DOTALL));
                patterns.add(Pattern.compile("^(.*[\"'])([^\"']+" + strategy.getQualifierSuffix() + suffix + ")([\"'].*)$", Pattern.DOTALL));
            }
            this.patterns = patterns;
        }

        @Override
        public String fixLine(String line, File file, int lineNumber) {
            for (Pattern p : patterns) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String ver = m.group(2);
                    try {
                        ver = strategy.transform(ver);
                    } catch (VersionParsingException e) {
                        if (Bump.ignoreErrors == false) {
                            throw new RuntimeException("Cannot parse", e);
                        } else {
                            System.err.println("Ignoring error while transforming version '" + ver + "'");
                            e.printStackTrace();
                        }
                    }
                    return m.group(1) + ver +  m.group(3);
                }
            }
            return line;
        }

        @Override
        public boolean acceptFile(File file) {
            return PomFeaturesAcceptor.acceptFile(file);
        }
    }
}
