#!/bin/bash

# This script is invoked by make deploy
# It packages the Java client and uploads it to OSSRH.
# The final release (to Maven Central) still has to be approved through
# the web interface at https://oss.sonatype.org/
# To make the upload possible you need:
#   - OSSRH credentials, configured in ~/.m2/settings.xml,
#     see http://books.sonatype.com/nexus-book/reference/usertoken.html#_accessing_and_using_your_user_tokens
#   - GPG key for support@franz.com. The script will prompt
#     for the passphrase for this key. Alternatively it can
#     be provided in the KEY_PASSWORD env var. Or the key
#     may be stored unencrypted.
# Note that release versions (i.e. those without the -SNAPSHOT modifier)
# cannot be changed after they're approved and uploaded to the central
# repository. This script will not allow an already released version
# to be uploaded again. It is possible to upload a version more than once
# if it is a snapshot or if it has not been approved yet.
# Tests will be run after the upload. These tests use the freshly uploaded JAR from OSSRH.
# This step can be skipped by setting the value of AG_SKIP_TESTS to anything.

set -e

# Name of the GPG key used to sign releases.
KEY_NAME=support@franz.com

# URL where the deployed versions are visible.
DEPLOYMENT_URL=https://oss.sonatype.org/content/groups/public/com/franz/agraph-java-client

# URL used to check OOSRH credentials.
NEXUS_CHECK_URL=https://oss.sonatype.org/service/local/staging/profiles

# Used to accumulate options for the final mvn call
MVN_OPTS=-DskipTests=true

function info () {
    echo $@ >&2
}

function err () {
    echo $@ >&2
}

function check_passphrase () {
    if [ -n "${KEY_PASSWORD+x}" ]; then
	printf %s "${KEY_PASSWORD:-}" | gpg --batch --passphrase-fd 0 -o /dev/null --local-user "${KEY_NAME}" -as - 2> /dev/null || { err "Passphrase is invalid." && return 1; }
    else
	 echo | gpg --batch -o /dev/null --passphrase-fd 0 --local-user "${KEY_NAME}" -as - 2> /dev/null || { err "GPG passphrase is required." && return 1; }
    fi
    info "GPG key access verified."
    return 0
}

function ag_is_running() {
    AGRAPH_HOST=${AGRAPH_HOST:-127.0.0.1}
    if [ -f "../agraph/lisp/agraph.port" ]; then
	DEFAULT_PORT=$(cat ../agraph/lisp/agraph.port)
    else
	DEFAULT_PORT=10035
    fi
    AGRAPH_PORT=${AGRAPH_PORT:-${DEFAULT_PORT}}
    curl -s -f "http://${AGRAPH_HOST}:${AGRAPH_PORT}/version" > /dev/null 2>&1
    return $?
}

# We'll need AG to run tests, so let's fail early if it's not there.
if [ -z "${AG_SKIP_TESTS+x}" ] && ! ag_is_running; then
    err "Start the server before deployment (or use AG_SKIP_TESTS)."
    exit 1
fi

# Check if the GPG key is available
if ! gpg -K "${KEY_NAME}" > /dev/null; then
    err "You must have a key named ${KEY_NAME} in your GPG keyring."
    exit 1
fi

# Establish the passphrase
while ! check_passphrase ; do
    read -s -p "Deployment key passphrase: " KEY_PASSWORD
    echo
done

# Pass the passphrase to Maven if required.
if [ -n "${KEY_PASSWORD+x}" ]; then
    MVN_OPTS+=" -Dgpg.passphrase=${KEY_PASSWORD}"
fi

# Check if OSSRH credentials are configured.
# This does not check if the user has the right permissions to deploy
# the artifact.
if ! mvn wagon:exist -Dwagon.serverId=ossrh -Dwagon.url=${NEXUS_CHECK_URL}; then
    cat <<EOF >&2
Cannot access oss.sonatype.org Please add proper credentials to the
<servers> section in ~/.m2/settings.xml.

If you do not have a ~/.m2/settings.xml file, copy the default one
from MAVEN_HOME/conf/settings.xml. On AG development machines the full
path is /usr/share/apache-maven/conf/settings.xml .

The credentials should look like this: 

<server>
  <id>ossrh</id>
  <username>your-ossrh-username</username>
  <password>your-ossrh-password</password>
</server>

Consider replacing the username and password with a token, see
http://books.sonatype.com/nexus-book/reference/usertoken.html#_accessing_and_using_your_user_tokens .
EOF
    exit 1
fi

VERSION=$(./version.sh)

info "Preparing version ${VERSION} for upload."

if [[ ${VERSION} =~ -SNAPSHOT$ ]]; then
    info "Uploading a SNAPSHOT version."
else
    info "Uploading a RELEASE version." 
    info "${DEPLOYMENT_URL}/${VERSION}"
    if curl --location --fail --silent --head "${DEPLOYMENT_URL}/${VERSION}" > /dev/null; then
        err "Version ${VERSION} already exists and cannot be overwritten."
        exit 1
    fi
fi

mvn clean deploy -P release ${MVN_OPTS}

if [ -z "${AG_SKIP_TESTS+x}" ]; then
    echo "Running tests using the released version:"
    echo "Waiting 10 seconds ..."
    sleep 10
    make test-release
fi
