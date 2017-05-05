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

    private static final String DASH_SNAPSHOT = "-SNAPSHOT";

    public static Version parse(String ver) throws VersionParsingException {
        boolean snapshot = ver.endsWith(DASH_SNAPSHOT);
        if (snapshot) {
            ver = ver.substring(0, ver.length() - DASH_SNAPSHOT.length());
        }
        return new Version(ver, snapshot);
    }

    public static class Version {
        private final String versionWithoutSnapshot;
        private final boolean snapshot;

        Version(String versionWithoutSnapshot, boolean snapshot) {
            checkArgument(versionWithoutSnapshot.endsWith(DASH_SNAPSHOT) == false,
                    "Parameter 'versionWithoutSnapshot' cannot end with -SNAPSHOT");
            this.versionWithoutSnapshot = requireNonNull(versionWithoutSnapshot);
            this.snapshot = snapshot;
        }

        public String toString() {
            return versionWithoutSnapshot + (snapshot?DASH_SNAPSHOT:"");
        }


        public boolean isSnapshot() {
            return snapshot;
        }

        public Version withSnapshot(boolean snapshot) {
            return new Version(versionWithoutSnapshot, snapshot);
        }

        static Entry<Boolean, String> stripSnapshot(String ver) {
            boolean snapshot = ver.endsWith(DASH_SNAPSHOT);
            if (snapshot) {
                ver = ver.substring(0, ver.length() - DASH_SNAPSHOT.length());
            }
            return immutableEntry(snapshot, ver);
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
            return snapshot == version.snapshot &&
                    Objects.equals(versionWithoutSnapshot, version.versionWithoutSnapshot);
        }

        @Override
        public int hashCode() {
            return Objects.hash(versionWithoutSnapshot, snapshot);
        }
    }

}
