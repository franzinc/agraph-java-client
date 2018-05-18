package com.franz.agraph.repository;

public class AGServerVersion implements Comparable<AGServerVersion> {
    private int[] versionSpecs;

    public AGServerVersion(String stringVersion) {
        stringVersion = stringVersion.replaceAll("[^\\d.]", "");
        String[] splitted = stringVersion.split("\\.");
        versionSpecs = new int[splitted.length];
        for (int i = 0; i < splitted.length; i++) {
            versionSpecs[i] = Integer.parseInt(splitted[i]);
        }
    }

    @Override
    public int compareTo(AGServerVersion o) {
        // First, compare all digits one by one, if at some point different digits are found, return the compare result.
        for (int i = 0; i < Math.min(versionSpecs.length, o.versionSpecs.length); i++) {
            if (versionSpecs[i] != o.versionSpecs[i]) {
                return Integer.compare(versionSpecs[i], o.versionSpecs[i]);
            }
        }
        // If all digits are equal, compare lengths (e.g. 6.4.1.1 should be bigger than 6.4.1)
        return Integer.compare(versionSpecs.length, o.versionSpecs.length);
    }
}
