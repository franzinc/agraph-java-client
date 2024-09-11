#! /usr/bin/env bash

# Strips the -SNAPSHOT suffix from the version number.
# Commits the result back to git, skipping gerrit.

set -eu

VERSION=$(./version.sh)

# Fail if the current version is not a snapshot.
if [[ ! "${VERSION}" == *-SNAPSHOT ]]; then
    echo "${VERSION} is not a snapshot version." 1>&2
    exit 1
fi

# Remove -SNAPSHOT from version, update README.
cd version-rewriter
mvn -q package
java -jar target/version-rewriter.jar .
