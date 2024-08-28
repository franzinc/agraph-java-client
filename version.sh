#!/bin/sh

# Print the client's version number (extract from pom.xml) on stdout.
# Used by the makefile and other scripts that need to know the version.

# Maven is more reliable, but painfully slow
# mvn -q exec:exec -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive

grep -m 1 "<version>" pom.xml | sed -e 's/<[^>]*>//g' | xargs

