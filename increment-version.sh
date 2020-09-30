#!/bin/bash

# Increments the version number by 0.0.1 and ensures that
# it contains the -SNAPSHOT qualifier.
# The change is commited to git, but *not* pushed anywhere.

set -e

pushd version-rewriter
mvn -q package
java -jar target/version-rewriter.jar . next-snapshot
popd

NEW_VERSION=$(./version.sh)

# Commit the new version
git add -u
git commit -m "Initial commit for v${NEW_VERSION}"
