#!/bin/bash -eu

echo 'mvn --version in agraph-java-client/events.sh'

mvn --version

# Compile code (including tests) and run the main class.
mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.args="$*" -Dexec.mainClass=test.stress.Events
