package com.franz.agraph.repository;

import com.franz.util.Util;

import java.util.Arrays;

/**
 * AllegroGraph server version number.
 *
 * A version is constructed from string containing a sequence of numbers
 * separated by non-digit characters.
 *
 * The main purpose of this class is to allow version numbers to be compared.
 * Note that only numeric components are compared, all other characters
 * are ignored. All trailing zeros are stripped, so version "6.0.0" is
 * considered to be equal to "6" and "6.0".
 */
public final class AGServerVersion implements Comparable<AGServerVersion> {
    // Preserve the original string
    private final String versionString;
    // Parsed numerical components
    private final int[] components;

    /**
     * Parses a version string and constructs a version object.
     *
     * @param versionString String to be parsed, must consist of a sequence
     *                      of numbers separated by non-digit characters,
     */
    public AGServerVersion(String versionString) {
        this.versionString = versionString;

        // Zero means "discard trailing empty components"
        String[] rawComponents = versionString.split("[^\\d]+", 0);

        // The first component might be empty
        int start = rawComponents.length > 0 && rawComponents[0].isEmpty() ? 1 : 0;

        // Strip trailing zeros
        int end = rawComponents.length;
        while (end > 0 && rawComponents[end - 1].equals("0")) {
            end--;
        }

        components = Arrays.stream(rawComponents, start, end)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    /**
     * Performs a lexicographic comparison with another version number.
     *
     * Note that only numerical components are considered, all other
     * characters from the original version strings are ignored.
     *
     *  Any trailing zeros are also ignored, so "6.0" will be considered
     * equal to "6.0.0" and "6".
     *
     * @param other The version to compare with.
     * @return See {@link Comparable#compareTo(Object)}
     * @throws NullPointerException if the other version is null.
     */
    @Override
    public int compareTo(AGServerVersion other) {
        return Util.compare(components, other.components);
    }

    /**
     * Checks if another object represents the same version number.
     *
     * Note that only numerical components are considered, all other
     * characters from the original version strings are ignored.
     *
     * Any trailing zeros are also ignored, so "6.0" will be considered
     * equal to "6.0.0" and "6".
     *
     * If the other object is null or not an AGServerVersion false
     * will be returned.
     *
     * @param other The object to compare to.
     * @return True if the other object represents the same version,
     *         false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof AGServerVersion) {
            return compareTo((AGServerVersion) other) == 0;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return versionString;
    }
}
