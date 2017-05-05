package io.frinx.utils.bump.transformer;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.Maps;
import io.frinx.utils.bump.Bump;
import io.frinx.utils.bump.transformer.util.ArgsParsingUtil;
import io.frinx.utils.bump.transformer.util.SedUtil;
import io.frinx.utils.bump.transformer.util.VersionFactory;
import io.frinx.utils.bump.transformer.util.VersionFactory.Version;
import io.frinx.utils.bump.transformer.util.VersionFactory.Version.VersionParsingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Test;

public class VersionTransformationStrategyFactory {

    public enum SnapshotTransformation {
        NONE, FLIP, ADD, DROP
    }

    public static Entry<VersionTransformationStrategy, List<String> /*remaining args*/> parseArgs(List<String> inputArgs) {
        final String[] qualifierSuffix = {""};
        final SnapshotTransformation[] snapshotTransformation = {SnapshotTransformation.NONE};
        final String[] maybePostprocessSed = new String[1];
        final String[] maybePreprocessSed = new String[1];

        // each switch maps to a closure writing the value to fields
        Map<String, Consumer<String>> parsers = new HashMap<>();
        parsers.put("--suffix", value -> qualifierSuffix[0] = value);
        parsers.put("--snapshot", value -> snapshotTransformation[0] = SnapshotTransformation.valueOf(value.toUpperCase()));
        parsers.put("--preprocess-sed", value -> maybePreprocessSed[0] = value);
        parsers.put("--postprocess-sed", value -> maybePostprocessSed[0] = value);
        // parse args using the map
        List<String> remainingArgs = ArgsParsingUtil.parse(inputArgs, parsers);
        // call constructor using fields
        return Maps.immutableEntry(new VersionTransformationStrategy(
                requireNonNull(qualifierSuffix[0], "--suffix not supplied"),
                snapshotTransformation[0],
                Optional.ofNullable(maybePostprocessSed[0]),
                Optional.ofNullable(maybePreprocessSed[0])
                ), remainingArgs);

    }

    public static void printHelp() {
        System.out.println(
                "Optional: --snapshot <none, flip, add, drop> - modify -SNAPSHOT\n" +
                "Optional: --preprocess-sed <command> - call sed on the version before all other transformations, e.g. s/rc1-frinx/frinx/\n" +
                "Optional: --postprocess-sed <command> - call sed on the version after all other transformations, e.g. s/Beryllium-SR2/Beryllium-SR3/\n"
        );
    }

    public static class VersionTransformationStrategy {
        private final String qualifierSuffix;
        // optional, --snapshot
        private final SnapshotTransformation snapshotTransformation;
        // optional, --postprocess-sed
        private final Optional<String> maybePostprocessSed;
        // optional, --preprocess-sed
        private final Optional<String> maybePreprocessSed;


        public VersionTransformationStrategy(String qualifierSuffix,
                                             SnapshotTransformation snapshotTransformation,
                                             Optional<String> maybePostprocessSed, Optional<String> maybePreprocessSed) {
            this.qualifierSuffix = qualifierSuffix;
            this.snapshotTransformation = requireNonNull(snapshotTransformation);
            this.maybePostprocessSed = requireNonNull(maybePostprocessSed);
            this.maybePreprocessSed = requireNonNull(maybePreprocessSed);
        }

        public String getQualifierSuffix() {
            return qualifierSuffix;
        }

        public SnapshotTransformation getSnapshotTransformation() {
            return snapshotTransformation;
        }

        public String transform(String ver) throws VersionParsingException {

            ver = maybeRunSed(ver, maybePreprocessSed);

            // do we need to parse the version?
            if (snapshotTransformation != SnapshotTransformation.NONE) {
                Version version = VersionFactory.parse(ver);
                SnapshotTransformation snapshot = getSnapshotTransformation();
                if (snapshot == SnapshotTransformation.FLIP) {
                    version = version.withSnapshot(!version.isSnapshot());
                } else if (snapshot == SnapshotTransformation.ADD) {
                    if (version.isSnapshot()) {
                        if (Bump.ignoreErrors == false) {
                            throw new IllegalStateException("Cannot ADD snapshot:" + version + ", can be suppressed with -DignoreErrors=true");
                        }
                    }
                    version = version.withSnapshot(true);
                } else if (snapshot == SnapshotTransformation.DROP) {
                    if (version.isSnapshot() == false) {
                        if (Bump.ignoreErrors == false) {
                            throw new IllegalStateException("Cannot DROP snapshot:" + version + ", can be suppressed with -DignoreErrors=true");
                        }
                    }
                    version = version.withSnapshot(false);
                } else {
                    throw new IllegalStateException();
                }

                ver = version.toString();
            }
            ver = maybeRunSed(ver, maybePostprocessSed);
            return ver;
        }

        private String maybeRunSed(String ver, Optional<String> maybeSomeSed) {
            if (maybeSomeSed.isPresent()) {
                String sed = maybeSomeSed.get();
                ver = SedUtil.sed(ver, sed);
            }
            return ver;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionTransformationStrategy that = (VersionTransformationStrategy) o;
            return Objects.equals(qualifierSuffix, that.qualifierSuffix) &&
                    snapshotTransformation == that.snapshotTransformation &&
                    Objects.equals(maybePreprocessSed, that.maybePreprocessSed) &&
                    Objects.equals(maybePostprocessSed, that.maybePostprocessSed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(qualifierSuffix, snapshotTransformation,
                    maybePreprocessSed, maybePostprocessSed);
        }

        @Override
        public String toString() {
            return "VersionTransformationStrategy{" +
                    "qualifierSuffix='" + qualifierSuffix + '\'' +
                    ", snapshotTransformation=" + snapshotTransformation +
                    ", maybePreprocessSed=" + maybePreprocessSed +
                    ", maybePostprocessSed=" + maybePostprocessSed +
                    '}';
        }
    }

}
