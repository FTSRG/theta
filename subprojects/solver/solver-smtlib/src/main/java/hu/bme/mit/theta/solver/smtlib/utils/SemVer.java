package hu.bme.mit.theta.solver.smtlib.utils;

import hu.bme.mit.theta.common.OsHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SemVer implements Comparable<SemVer> {
    private final int[] version;

    private SemVer(final String version) {
        checkNotNull(version);
        checkArgument(version.matches("[0-9]+(\\.[0-9]+)*"));

        this.version = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    public static SemVer of(final String version) {
        return new SemVer(version);
    }

    public boolean hasMajor() {
        return version.length > 0;
    }

    public int getMajor() {
        return version[0];
    }

    public boolean hasMinor() {
        return version.length > 1;
    }

    public int getMinor() {
        return version[1];
    }

    public boolean hasPatch() {
        return version.length > 2;
    }

    public int getPatch() {
        return version[2];
    }

    public int[] getAll() {
        return version;
    }

    @Override
    public int compareTo(final SemVer that) {
        if(that == null) {
            return 1;
        }

        for(int i = 0; i < Math.max(this.version.length, that.version.length); i++) {
            final var thisVer = i < this.version.length ? this.version[i] : 0;
            final var thatVer = i < that.version.length ? that.version[i] : 0;

            if(thisVer < thatVer) {
                return -1;
            }
            else if(thisVer > thatVer) {
                return 1;
            }
            else {
                continue;
            }
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        else if(obj instanceof SemVer) {
            return this.compareTo((SemVer) obj) == 0;
        }
        else {
            return false;
        }
    }

    public static class VersionDecoder {
        private final SemVer version;
        private final Map<OsHelper.OperatingSystem, Map<OsHelper.Architecture, String>> string;

        private VersionDecoder(final SemVer version, final Map<OsHelper.OperatingSystem, Map<OsHelper.Architecture, String>> string) {
            this.version = version;
            this.string = string;
        }

        public static VersionDecoder.Builder create(final SemVer version) {
            return new VersionDecoder.Builder(version);
        }

        public SemVer getVersion() {
            return version;
        }

        public String getOsArchString(final OsHelper.OperatingSystem os, final OsHelper.Architecture arch) {
            if(!string.containsKey(os)) {
                throw new UnsupportedOperationException(String.format("Operating system %s is not supported by z3", os));
            }
            else if(!string.get(os).containsKey(arch)) {
                throw new UnsupportedOperationException(String.format("Architecture %s on operating system %s is not supported by z3", arch, os));
            }
            else {
                return string.get(os).get(arch);
            }
        }

        public static class Builder {
            private final SemVer version;
            private final Map<OsHelper.OperatingSystem, Map<OsHelper.Architecture, String>> string;

            private Builder(final SemVer version) {
                this.version = version;
                this.string = new HashMap<>();
            }

            public VersionDecoder.Builder addString(final OsHelper.OperatingSystem os, final OsHelper.Architecture arch, final String string) {
                if(!this.string.containsKey(os)) {
                    this.string.put(os, new HashMap<>());
                }
                this.string.get(os).put(arch, string);
                return this;
            }

            public VersionDecoder build() {
                return new VersionDecoder(version, string);
            }
        }
    }
}
