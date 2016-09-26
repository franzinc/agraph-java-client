#!/bin/sh

# Print the client's version number (extract from pom.xml) on stdout.
# Used by the makefile and other scripts that need to know the version.

mvn -q exec:exec -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive
