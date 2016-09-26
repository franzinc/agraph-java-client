#!/bin/bash

# Creates a release build and uploads it to OSSRH.
# See release-process.md for details and usage instructions.

set -e

if ! git diff-index --quiet HEAD --; then
    echo "There are uncommited changes, exiting."
    exit 1
fi

# Remove -SNAPSHOT from versions, update README.
pushd version-rewriter
mvn -q package
java -jar target/version-rewriter.jar .
popd

VERSION=$(./version.sh)

# Commit the released version, add a tag.
git add .
git commit -m "Release ${VERSION}"
# Use -f in case there was a problem with this release and the tag already exists.
git tag -f -m "Release ${VERSION}" -a "v${VERSION}"

# Release
./deploy.sh

# Increment the version number, add -SNAPSHOT
pushd version-rewriter
java -jar target/version-rewriter.jar .
popd

NEW_VERSION=$(./version.sh)

# Commit the new version
git add .
git commit -m "Initial commit for v${NEW_VERSION}"

