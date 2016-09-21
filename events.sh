#!/bin/bash -eu

# Report Java version first
java -version

# Compute the actual classpath and save it in a file (./classpath).
ant -q classpath

# Run the test, place compiled files (./classes) on the classpath.
java -cp "classes:$(cat classpath)" test.stress.Events $*
