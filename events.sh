#!/bin/bash -eu

# Report Java version first
java -version

# Compile code (including tests) and run the main class.
mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.args="$*" -Dexec.mainClass=test.stress.Events
