package io.frinx.utils.bump.transformer.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Objects.requireNonNull;

import io.frinx.utils.bump.transformer.util.VersionFactory.Version.VersionParsingException;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionFactory {

    public static Version parse(String ver, String expectedQualifierSuffix) throws VersionParsingException {
        boolean snapshot = ver.endsWith("-SNAPSHOT");
        if (snapshot) {
            ver = ver.substring(0, ver.length() - "-SNAPSHOT".length());
        }
        Pattern versionPattern = Pattern.compile("^([\\d\\.]+)\\.(\\d+)\\.([^\\.]+)\\.(\\d+-" + expectedQualifierSuffix + ")$");
        Matcher m = versionPattern.matcher(ver);
        if (m.matches()) {
            return create(m.group(1), m.group(2), Optional.of(VersionPart.parse(m.group(3))), Optional.of(m.group(4)), snapshot, ver, expectedQualifierSuffix);
        }
        Pattern withoutQualifierNrVersionPattern = Pattern.compile("^(\\d+)\\.([\\d]+)\\.([^\\.]+)\\.(" + expectedQualifierSuffix + ")$");
        m = withoutQualifierNrVersionPattern.matcher(ver);
        if (m.matches()) {
            return create(m.group(1), m.group(2), Optional.of(VersionPart.parse(m.group(3))), Optional.empty(), snapshot, ver, expectedQualifierSuffix);
        }
        Pattern withoutMicroVersionPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+-" + expectedQualifierSuffix + ")$");
        m = withoutMicroVersionPattern.matcher(ver);
        if (m.matches()) {
            return create(m.group(1), m.group(2), Optional.empty(), Optional.of(m.group(3)), snapshot, ver, expectedQualifierSuffix);
        }
        throw new IllegalArgumentException("Cannot parse '" + ver + "'");
    }

    private static Version create(String major, String minor, Optional<VersionPart> maybeMicro, Optional<String> maybeQualifier, boolean snapshot,
                                  String wholeVersion, String expectedQualifierSuffix) throws VersionParsingException {
        Optional<Integer> qualifierIdx;
        if (maybeQualifier.isPresent()) {
            String qualifier = maybeQualifier.get();
            checkArgument(qualifier.endsWith("-" + expectedQualifierSuffix));
            qualifier = qualifier.substring(0, qualifier.length() - "-".length() - expectedQualifierSuffix.length());
            int value;
            try {
                value = Integer.parseInt(qualifier);
            } catch (NumberFormatException e) {
                throw new VersionParsingException("Cannot parse " + wholeVersion, e);
            }
            qualifierIdx = Optional.of(value);
        } else {
            qualifierIdx = Optional.empty();
        }
        int minorI;
        try {
            minorI = Integer.parseInt(minor);
        } catch (NumberFormatException e) {
            throw new VersionParsingException("Cannot parse " + wholeVersion, e);
        }
        return new Version(major, minorI, maybeMicro, qualifierIdx, snapshot, expectedQualifierSuffix);
    }

    static class VersionPart {
        private final Optional<Integer> intPart;
        private final Optional<String> stringPart;

        private static final Pattern VERSION_PART_PATTERN = Pattern.compile("^(\\d*)[-]?(.*)$");

        public static VersionPart parse(String part) {
            Matcher matcher = VERSION_PART_PATTERN.matcher(part);
            if (matcher.matches()) {
                Optional<Integer> intPart;
                if (matcher.group(1).isEmpty() == false) {
                    intPart = Optional.of(Integer.parseInt(matcher.group(1)));
                } else {
                    intPart = Optional.empty();
                }
                Optional<String> stringPart;
                if (matcher.group(2).isEmpty() == false) {
                    stringPart = Optional.of(matcher.group(2));
                } else {
                    stringPart = Optional.empty();
                }
                return new VersionPart(intPart, stringPart);
            }
            throw new IllegalStateException("VersionPart does not match regex - " + part);
        }

        public VersionPart(Optional<Integer> intPart, Optional<String> stringPart) {
            if (intPart.isPresent() == false && stringPart.isPresent() == false) {
                throw new IllegalArgumentException("Illegal VersionPart - both parts are empty");
            }
            if (stringPart.isPresent() && stringPart.get().isEmpty()) {
                throw new IllegalArgumentException("Illegal VersionPart - empty string");
            }
            this.intPart = intPart;
            this.stringPart = stringPart;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionPart that = (VersionPart) o;
            return Objects.equals(intPart, that.intPart) &&
                    Objects.equals(stringPart, that.stringPart);
        }

        @Override
        public int hashCode() {
            return Objects.hash(intPart, stringPart);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (intPart.isPresent()) {
                sb.append(intPart.get());
                if (stringPart.isPresent()) {
                    sb.append("-");
                }
            }
            if (stringPart.isPresent()) {
                sb.append(stringPart.get());
            }
            return sb.toString();
        }
    }

    public static class Version {
        private final String major;
        private final int minor;
        private final Optional<VersionPart> maybeMicro;
        private final Optional<Integer> qualifierIndex;
        private final String qualifierSuffix;

        private final boolean snapshot;
        private final String toString;

        Version(String major, int minor, Optional<VersionPart> maybeMicro, Optional<Integer> qualifierIndex, boolean snapshot, String qualifierSuffix) {
            this.major = requireNonNull(major);
            this.minor = minor;
            this.maybeMicro = maybeMicro;
            this.qualifierIndex = requireNonNull(qualifierIndex);
            this.snapshot = snapshot;
            this.qualifierSuffix = qualifierSuffix;
            {
                String toString = major + "." + minor;
                if (maybeMicro.isPresent()) {
                    toString += "." + maybeMicro.get();
                }
                toString += ".";
                if (qualifierIndex.isPresent()) {
                    toString += qualifierIndex.get() + "-";
                }
                toString += qualifierSuffix + (snapshot ? "-SNAPSHOT" : "");
                this.toString = toString;
            }
        }

        public String toString() {
            return toString;
        }

        public Optional<Integer> getQualifierIndex() {
            return qualifierIndex;
        }


        public boolean isSnapshot() {
            return snapshot;
        }

        public Version withSnapshot(boolean snapshot) {
            return new Version(major, minor, maybeMicro, qualifierIndex, snapshot, qualifierSuffix);
        }

        static Entry<Boolean, String> stripSnapshot(String ver) {
            boolean snapshot = ver.endsWith("-SNAPSHOT");
            if (snapshot) {
                ver = ver.substring(0, ver.length() - "-SNAPSHOT".length());
            }
            return immutableEntry(snapshot, ver);
        }

        public Version withQualifierIdx(int qualifierIndex) {
            return new Version(major, minor, maybeMicro, Optional.of(qualifierIndex), snapshot, qualifierSuffix);
        }

        private Version withMinorReset() {
            return new Version(major, 0, maybeMicro, qualifierIndex, snapshot, qualifierSuffix);
        }

        private Version withMicroReset() {
            Optional<VersionPart> newMicro = this.maybeMicro;
            if (newMicro.isPresent()) {
                // reset to 0
                VersionPart versionPart = newMicro.get();
                if (versionPart.intPart.isPresent()) {
                    newMicro = Optional.of(new VersionPart(Optional.of(0), maybeMicro.get().stringPart));
                }
            }
            return new Version(major, minor, newMicro, qualifierIndex, snapshot, qualifierSuffix);
        }

        private Version withQualifierReset() {
            Optional<Integer> newQualifierIndex = this.qualifierIndex;
            if (newQualifierIndex.isPresent()) {
                // reset to 1
                newQualifierIndex = Optional.of(1);
            }
            return new Version(major, minor, maybeMicro, newQualifierIndex, snapshot, qualifierSuffix);
        }

        public Version withMajorIncremented() {
            Version version = this;
            int majorInt = Integer.parseInt(this.major);
            String newMajor = String.valueOf(majorInt+1);
            version = version.withMinorReset();
            version = version.withMicroReset();
            version = version.withQualifierReset();
            return new Version(newMajor, version.minor, version.maybeMicro, version.qualifierIndex,
                    version.snapshot, version.qualifierSuffix);
        }

        public Version withMinorIncremented() {
            Version version = this;
            version = version.withMicroReset();
            version = version.withQualifierReset();
            return new Version(version.major, version.minor + 1, version.maybeMicro, version.qualifierIndex,
                    version.snapshot, version.qualifierSuffix);
        }

        public Version withMicroIncremented() {
            checkState(maybeMicro.isPresent(), "Micro not present in " + this);
            VersionPart presentMicro = maybeMicro.get();
            checkState(presentMicro.intPart.isPresent(), "Micro.intPart not present in " + this);
            VersionPart incremented = new VersionPart(Optional.of(presentMicro.intPart.get() + 1), presentMicro.stringPart);
            Optional<Integer> newQualifierIndex = this.qualifierIndex;
            if (newQualifierIndex.isPresent()) {
                // reset to 1
                newQualifierIndex = Optional.of(1);
            }
            return new Version(major, minor, Optional.of(incremented), newQualifierIndex, snapshot, qualifierSuffix);
        }

        public Version withQualifierIncremented() {
            checkState(qualifierIndex.isPresent());
            int incremented = qualifierIndex.get() + 1;
            return new Version(major, minor, maybeMicro, Optional.of(incremented), snapshot, qualifierSuffix);
        }

        public static class VersionParsingException extends Exception {
            VersionParsingException(String message, Exception e) {
                super(message, e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Version version = (Version) o;
            return minor == version.minor &&
                    snapshot == version.snapshot &&
                    Objects.equals(major, version.major) &&
                    Objects.equals(maybeMicro, version.maybeMicro) &&
                    Objects.equals(qualifierIndex, version.qualifierIndex) &&
                    Objects.equals(qualifierSuffix, version.qualifierSuffix) &&
                    Objects.equals(toString, version.toString);
        }

        @Override
        public int hashCode() {
            return toString.hashCode();
        }
    }

}
