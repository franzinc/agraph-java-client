#! /usr/bin/env bash

# Increments the version number by 0.0.1 and ensures that
# it contains the -SNAPSHOT qualifier.
# The change is commited to git, but *not* pushed anywhere.

set -eu

cd version-rewriter
mvn -q package
java -jar target/version-rewriter.jar . next-snapshot
