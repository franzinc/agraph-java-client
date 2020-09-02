#!/bin/bash

# Strips the -SNAPSHOT suffix from the version number.
# Commits the result back to git, skipping gerrit.

set -e

if ! git diff-index --quiet HEAD --; then
    echo "There are uncommited changes, exiting."
    exit 1
fi

VERSION=$(./version.sh)

# Fail if the current version is not a snapshot.
if [[ ! "${VERSION}" == *-SNAPSHOT ]]; then
    echo "${VERSION} is not a snapshot version." 1>&2
    exit 1
fi

# Remove -SNAPSHOT from version, update README.
pushd version-rewriter
mvn -q package
java -jar target/version-rewriter.jar .
popd

# Grab the updated version.
VERSION=$(./version.sh)

# Commit the released version, add a tag.
git add -u
git commit -m "Release ${VERSION}"
# Use -f in case there was a problem with this release
# and the tag already exists.
git tag -f -m "Release ${VERSION}" -a "release_v${VERSION}"
